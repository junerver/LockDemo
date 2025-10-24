package xyz.junerver.android.lockdemo.lockctl.debounce;

import android.util.Log;

import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 锁控板串口发送器
 * 实现CommandSender接口，封装真实的串口通信功能
 * 适用于生产环境
 */
public class LockCtlBoardSerialSender implements CommandSender {
    private static final String TAG = "LockCtlBoardSerialSender";

    // 串口配置
    private static final String DEFAULT_DEVICE_PATH = "/dev/ttyS4";
    private static final int DEFAULT_BAUD_RATE = 9600;

    // 组件
    private SerialPortManager serialPortManager;
    private OnResponseListener responseListener;

    // 状态管理
    private volatile boolean isConnected = false;
    private final List<byte[]> sentCommands = new ArrayList<>();

    // 数据缓冲区，用于处理分包数据
    private final List<Byte> dataBuffer = new ArrayList<>();

    /**
     * 构造函数（使用默认配置）
     */
    public LockCtlBoardSerialSender() {
        this(DEFAULT_DEVICE_PATH, DEFAULT_BAUD_RATE);
    }

    /**
     * 构造函数（指定设备路径，使用默认波特率）
     *
     * @param devicePath 设备路径
     */
    public LockCtlBoardSerialSender(String devicePath) {
        this(devicePath, DEFAULT_BAUD_RATE);
    }

    /**
     * 构造函数
     *
     * @param devicePath 设备路径
     * @param baudRate   波特率
     */
    public LockCtlBoardSerialSender(String devicePath, int baudRate) {
        Log.i(TAG, String.format("初始化锁控板串口发送器: 设备=%s, 波特率=%d", devicePath, baudRate));
        initializeSerialPort(devicePath, baudRate);
    }

    /**
     * 初始化串口连接
     *
     * @param devicePath 设备路径
     * @param baudRate   波特率
     */
    private void initializeSerialPort(String devicePath, int baudRate) {
        try {
            serialPortManager = new SerialPortManager();

            serialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                @Override
                public void onSuccess(File device) {
                    isConnected = true;
                    Log.i(TAG, "串口连接成功: " + device.getAbsolutePath());

                    // 清空数据缓冲区
                    synchronized (dataBuffer) {
                        dataBuffer.clear();
                    }
                }

                @Override
                public void onFail(File device, Status status) {
                    isConnected = false;
                    Log.e(TAG, "串口连接失败: " + device.getAbsolutePath() + ", 状态: " + status);

                    // 通知监听器连接失败
                    if (responseListener != null) {
                        responseListener.onError("串口连接失败: " + status);
                    }
                }
            });

            serialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    handleSerialData(bytes);
                }

                @Override
                public void onDataSent(byte[] bytes) {
                    // 记录发送的数据
                    synchronized (sentCommands) {
                        sentCommands.add(bytes.clone());
                    }

                    Log.d(TAG, String.format("串口数据已发送: 长度=%d, 数据=%s",
                            bytes.length, bytesToHex(bytes)));
                }
            });

            // 打开串口连接
            serialPortManager.openSerialPort(new File(devicePath), baudRate);

        } catch (Exception e) {
            Log.e(TAG, "初始化串口失败", e);
            isConnected = false;

            if (responseListener != null) {
                responseListener.onError("初始化串口失败: " + e.getMessage());
            }
        }
    }

    /**
     * 处理接收到的串口数据
     *
     * @param bytes 接收到的字节数据
     */
    private void handleSerialData(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        Log.d(TAG, String.format("收到串口数据: 长度=%d, 数据=%s",
                bytes.length, bytesToHex(bytes)));

        // 将新数据添加到缓冲区
        synchronized (dataBuffer) {
            for (byte b : bytes) {
                dataBuffer.add(b);
            }

            // 尝试从缓冲区中提取完整的响应帧
            extractCompleteFrames();
        }
    }

    /**
     * 从缓冲区中提取完整的响应帧
     */
    private void extractCompleteFrames() {
        // 查找起始符位置 (57 4B 4C 59)
        int startIndex = -1;
        for (int i = 0; i <= dataBuffer.size() - 4; i++) {
            if (dataBuffer.get(i) == 0x57 &&
                    dataBuffer.get(i + 1) == 0x4B &&
                    dataBuffer.get(i + 2) == 0x4C &&
                    dataBuffer.get(i + 3) == 0x59) {
                startIndex = i;
                break;
            }
        }

        // 如果没找到起始符，清空缓冲区（防止错误数据堆积）
        if (startIndex == -1) {
            if (dataBuffer.size() > 100) { // 防止缓冲区无限增长
                dataBuffer.clear();
                Log.w(TAG, "清空无效数据缓冲区");
            }
            return;
        }

        // 检查是否有足够的数据来读取帧长度字段
        if (dataBuffer.size() < startIndex + 5) {
            return; // 数据不足，等待更多数据
        }

        // 读取帧长度
        int frameLength = dataBuffer.get(startIndex + 4) & 0xFF;

        // 检查是否有完整的数据帧
        if (dataBuffer.size() < startIndex + frameLength) {
            return; // 数据不完整，等待更多数据
        }

        // 提取完整的数据帧
        byte[] frameData = new byte[frameLength];
        for (int i = 0; i < frameLength; i++) {
            frameData[i] = dataBuffer.get(startIndex + i);
        }

        // 从缓冲区中移除已处理的数据
        for (int i = 0; i < startIndex + frameLength; i++) {
            dataBuffer.remove(0);
        }

        // 验证帧的完整性
        if (ResponseMatcher.isValidResponseFormat(frameData)) {
            Log.d(TAG, String.format("提取完整响应帧: 指令字=0x%02X, 长度=%d",
                    frameData.length > 6 ? frameData[6] & 0xFF : 0, frameData.length));

            // 发送响应数据
            if (responseListener != null) {
                responseListener.onResponseReceived(frameData);
            }
        } else {
            Log.w(TAG, "收到无效响应帧: " + bytesToHex(frameData));
        }

        // 递归处理缓冲区中可能的其他完整帧
        if (dataBuffer.size() > 0) {
            extractCompleteFrames();
        }
    }

    @Override
    public void sendCommand(byte[] command) {
        if (!isConnected) {
            Log.e(TAG, "串口未连接，拒绝发送指令");
            if (responseListener != null) {
                responseListener.onError("串口未连接");
            }
            return;
        }

        if (serialPortManager == null) {
            Log.e(TAG, "串口管理器为null");
            if (responseListener != null) {
                responseListener.onError("串口管理器未初始化");
            }
            return;
        }

        if (command == null || command.length == 0) {
            Log.e(TAG, "指令数据为空");
            if (responseListener != null) {
                responseListener.onError("指令数据为空");
            }
            return;
        }

        try {
            Log.d(TAG, String.format("发送指令: 指令字=0x%02X, 长度=%d",
                    command.length > 6 ? command[6] & 0xFF : 0, command.length));

            serialPortManager.sendBytes(command);

        } catch (Exception e) {
            Log.e(TAG, "发送指令失败", e);
            if (responseListener != null) {
                responseListener.onError("发送指令失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void setOnResponseListener(OnResponseListener listener) {
        this.responseListener = listener;
        Log.d(TAG, "响应监听器已" + (listener != null ? "设置" : "清除"));
    }

    @Override
    public OnResponseListener getOnResponseListener() {
        return this.responseListener;
    }

    @Override
    public boolean isConnected() {
        return isConnected && serialPortManager != null;
    }

    @Override
    public void disconnect() {
        if (serialPortManager != null) {
            try {
                serialPortManager.closeSerialPort();
                Log.i(TAG, "串口连接已关闭");
            } catch (Exception e) {
                Log.e(TAG, "关闭串口连接失败", e);
            } finally {
                serialPortManager = null;
                isConnected = false;
            }
        }

        // 清空数据缓冲区
        synchronized (dataBuffer) {
            dataBuffer.clear();
        }

        Log.i(TAG, "锁控板串口发送器已断开连接");
    }

    /**
     * 重新连接串口
     */
    public void reconnect() {
        disconnect();
        initializeSerialPort(DEFAULT_DEVICE_PATH, DEFAULT_BAUD_RATE);
    }

    /**
     * 重新连接串口
     *
     * @param devicePath 设备路径
     * @param baudRate   波特率
     */
    public void reconnect(String devicePath, int baudRate) {
        disconnect();
        initializeSerialPort(devicePath, baudRate);
    }

    /**
     * 清空数据缓冲区
     */
    public void clearDataBuffer() {
        synchronized (dataBuffer) {
            dataBuffer.clear();
            Log.d(TAG, "数据缓冲区已清空");
        }
    }

    /**
     * 获取缓冲区中的数据大小（用于调试）
     *
     * @return 缓冲区大小
     */
    public int getBufferSize() {
        synchronized (dataBuffer) {
            return dataBuffer.size();
        }
    }

    /**
     * 获取已发送的指令列表
     *
     * @return 指令列表的副本
     */
    public List<byte[]> getSentCommands() {
        synchronized (sentCommands) {
            List<byte[]> result = new ArrayList<>();
            for (byte[] cmd : sentCommands) {
                result.add(cmd.clone());
            }
            return result;
        }
    }

    /**
     * 清空发送记录
     */
    public void clearSentCommands() {
        synchronized (sentCommands) {
            sentCommands.clear();
            Log.d(TAG, "发送记录已清空");
        }
    }

    /**
     * 字节数组转十六进制字符串（用于日志输出）
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 获取连接状态信息
     *
     * @return 状态信息
     */
    public String getConnectionInfo() {
        return String.format("LockCtlBoardSerialSender{connected=%s, bufferSize=%d, sentCommands=%d}",
                isConnected, getBufferSize(), getSentCommands().size());
    }
}