package xyz.junerver.android.lockdemo;

import android.util.Log;

import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 门锁控制板工具类
 * 懒加载单例模式，用于控制留样柜门锁的开关、状态查询等操作
 * 注意：指令构造功能已迁移到 LockCtlBoardCmdHelper 工具类中
 */
public class LockCtlBoardUtil {
    private static final String TAG = "LockCtlBoardUtil";
    private static volatile LockCtlBoardUtil instance;
    private SerialPortManager mSerialPortManager;

    private OnDataReceived mOnDataReceived;

    // 数据缓冲区，用于处理分包数据
    private final List<Byte> dataBuffer = new ArrayList<>();

    public interface OnDataReceived {
        void onDataReceived(String json);
    }

    // 默认门锁总数
    private static final int DEFAULT_LOCK_COUNT = 12;

    // 单例私有构造函数
    private LockCtlBoardUtil() {
        // 初始化操作
    }

    /**
     * 获取单例实例（双重检查锁定）
     *
     * @return LockCtlBoardUtil实例
     */
    public static LockCtlBoardUtil getInstance() {
        if (instance == null) {
            synchronized (LockCtlBoardUtil.class) {
                if (instance == null) {
                    instance = new LockCtlBoardUtil();
                }
            }
        }
        return instance;
    }

    // 打开串口
    public void openSerialPort() {
        if (null == mSerialPortManager) {
            mSerialPortManager = new SerialPortManager();
        }
        mSerialPortManager
                .setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                    @Override
                    public void onSuccess(File device) {
                        Log.d(TAG, "串口打开成功");
                    }

                    @Override
                    public void onFail(File device, Status status) {
                        Log.e(TAG, "onFail: " + device.getAbsolutePath());
                    }
                })
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {

                    @Override
                    public void onDataReceived(byte[] bytes) {
                        // 将新数据添加到缓冲区
                        synchronized (dataBuffer) {
                            for (byte b : bytes) {
                                dataBuffer.add(b);
                            }

                            // 尝试从缓冲区中提取完整的响应帧
                            extractCompleteFrames();
                        }
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {

                    }
                })
                .openSerialPort(new File("/dev/ttyS4"), 9600);
    }

    // 关闭串口
    public void closeSerialPort() {
        if (null != mSerialPortManager) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;

            // 清空数据缓冲区
            synchronized (dataBuffer) {
                dataBuffer.clear();
            }

            Log.d(TAG, "串口已关闭，数据缓冲区已清空");
        }
    }

    // 检查串口是否已打开
    public boolean isSerialPortOpen() {
        return mSerialPortManager != null;
    }

    // 清空数据缓冲区
    public void clearDataBuffer() {
        synchronized (dataBuffer) {
            dataBuffer.clear();
            Log.d(TAG, "数据缓冲区已手动清空");
        }
    }

    // 获取缓冲区中的数据大小（用于调试）
    public int getBufferSize() {
        synchronized (dataBuffer) {
            return dataBuffer.size();
        }
    }

    // 设置串口数据监听器
    public void setOnDataReceived(OnDataReceived onDataReceived) {
        this.mOnDataReceived = onDataReceived;
    }

    /**
     * 1. 同时开多锁
     *
     * 实际使用时最多支持两个锁，第三个锁将无法打开
     *
     * @param lockIds 门锁ID数组（可变长参数）
     * @return 操作是否成功
     */
    public boolean openMultipleLocksSimultaneously(int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand((byte) 0x00, lockIds);
        if (command == null) {
            Log.e(TAG, "构造同时开多锁指令失败");
            return false;
        }

        Log.d(TAG, "同时开启门锁: " + Arrays.toString(lockIds));
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁打开完成");

        return true;
    }

    /**
     * 2. 锁通道LED闪烁
     *
     * 注意：LED闪烁功能只支持接入设备为LED，如果是锁将无法
     *
     * @param lockId   门锁ID
     * @return 操作是否成功
     */
    public boolean flashLockLed(int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildFlashChannelCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造通道闪烁指令失败");
            return false;
        }

        Log.d(TAG, "门锁 " + lockId + " LED闪烁");
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁LED闪烁完成");

        return true;
    }

    /**
     * 3. 单独开一把锁
     *
     * @param lockId 门锁ID
     * @return 操作是否成功
     */
    public boolean openSingleLock(int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造开单锁指令失败");
            return false;
        }

        Log.d(TAG, "开启门锁: " + lockId);
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁打开完成");

        return true;
    }

    /**
     * 4. 查询单个门锁状态
     *
     * @param lockId 门锁ID
     * @return 门锁状态（0-关闭，1-打开，-1-错误）
     */
    public boolean getSingleLockStatus(int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造查询单锁状态指令失败");
            return false;
        }

        Log.d(TAG, "查询门锁状态: " + lockId);
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁状态查询完成");
        return true;
    }

    /**
     * 5. 查询所有门锁状态
     *
     * @return 所有门锁状态数组
     */
    public boolean getAllLocksStatus() {
        Log.d(TAG, "查询所有门锁状态");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造查询所有锁状态指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "所有门锁状态查询完成");
        return true;
    }

    /**
     * 6. 全部开锁（逐一打开）
     *
     * @return 操作是否成功
     */
    public boolean openAllLocksSequentially() {
        Log.d(TAG, "开始逐一开启所有门锁");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenAllLocksCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造开全部锁指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "所有门锁开启完成");
        return true;
    }

    /**
     * 7. 开多锁，逐一打开（可变长参数）
     *
     * @param lockIds 门锁ID数组（可变长参数）
     * @return 操作是否成功
     */
    public boolean openMultipleLocksSequentially(int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return false;
        }

        Log.d(TAG, "开始逐一开启门锁: " + Arrays.toString(lockIds));

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand((byte) 0x00, lockIds);
        if (command == null) {
            Log.e(TAG, "构造逐一开多锁指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);

        Log.d(TAG, "指定门锁开启完成");
        return true;
    }

    /**
     * 8. 通道持续打开
     *
     * @param channelId 通道ID
     * @param duration  持续时间（毫秒）
     * @return 操作是否成功
     */
    public boolean keepChannelOpen(int channelId, long duration) {
        if (channelId < 0 || channelId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "通道ID无效: " + channelId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildChannelKeepOpenCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造通道持续打开指令失败");
            return false;
        }

        Log.d(TAG, "通道 " + channelId + " 持续打开，持续时间: " + duration + "ms");
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "通道持续打开完成");

        return true;
    }

    /**
     * 9. 关闭通道
     *
     * @param channelId 通道ID
     * @return 操作是否成功
     */
    public boolean closeChannel(int channelId) {
        if (channelId < 0 || channelId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "通道ID无效: " + channelId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildCloseChannelCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造关闭通道指令失败");
            return false;
        }

        Log.d(TAG, "关闭通道: " + channelId);
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "通道关闭完成");

        return true;
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
                Log.w(TAG, "缓冲区中有大量无效数据，清空缓冲区");
                dataBuffer.clear();
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
        if (LockCtlBoardCmdHelper.validateResponse(frameData)) {
            // 解析并发送响应数据
            String json = LockCtlBoardCmdHelper.parseResponseToJson(frameData);
            if (mOnDataReceived != null) {
                mOnDataReceived.onDataReceived(json);
            }
            Log.d(TAG, "收到完整响应帧: " + json);
        } else {
            Log.w(TAG, "收到无效的响应帧，丢弃: " + bytesToHex(frameData));
        }

        // 递归处理缓冲区中可能的其他完整帧
        if (dataBuffer.size() > 0) {
            extractCompleteFrames();
        }
    }

    /**
     * 字节数组转十六进制字符串（用于日志输出）
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

}