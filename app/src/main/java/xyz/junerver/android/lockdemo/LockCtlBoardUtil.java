package xyz.junerver.android.lockdemo;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.junerver.android.lockdemo.debounce.CommandSender;
import xyz.junerver.android.lockdemo.debounce.LockCtlBoardSerialSender;
import xyz.junerver.android.lockdemo.debounce.OnResponseListener;

/**
 * 门锁控制板工具类
 * 懒加载单例模式，用于控制留样柜门锁的开关、状态查询等操作
 * 注意：指令构造功能已迁移到 LockCtlBoardCmdHelper 工具类中
 *
 * 改造说明：现在通过 CommandSender 接口发送指令，支持多种通信方式
 */
public class LockCtlBoardUtil {
    private static final String TAG = "LockCtlBoardUtil";
    private static volatile LockCtlBoardUtil instance;

    // 移除对直接串口管理器的依赖，改为使用CommandSender接口
    // private SerialPortManager mSerialPortManager; // 已废弃

    // 新的指令发送器接口
    private CommandSender commandSender;

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
        // 默认使用串口发送器
        this.commandSender = new LockCtlBoardSerialSender();

        // 设置响应监听器适配器
        if (this.commandSender != null) {
            this.commandSender.setOnResponseListener(new OnResponseListener() {
                @Override
                public void onResponseReceived(byte[] response) {
                    // 将新数据添加到缓冲区
                    synchronized (dataBuffer) {
                        for (byte b : response) {
                            dataBuffer.add(b);
                        }

                        // 尝试从缓冲区中提取完整的响应帧
                        extractCompleteFrames();
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "指令发送器错误: " + error);
                }
            });
        }
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

    /**
     * 设置指令发送器
     * 支持动态切换通信方式（串口、网络、蓝牙等）
     *
     * @param commandSender 指令发送器实现
     */
    public void setCommandSender(CommandSender commandSender) {
        if (commandSender == null) {
            Log.e(TAG, "指令发送器不能为null");
            return;
        }

        // 清理旧的发送器
        if (this.commandSender != null) {
            this.commandSender.disconnect();
        }

        this.commandSender = commandSender;

        // 设置响应监听器适配器
        this.commandSender.setOnResponseListener(new OnResponseListener() {
            @Override
            public void onResponseReceived(byte[] response) {
                // 将新数据添加到缓冲区
                synchronized (dataBuffer) {
                    for (byte b : response) {
                        dataBuffer.add(b);
                    }

                    // 尝试从缓冲区中提取完整的响应帧
                    extractCompleteFrames();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "指令发送器错误: " + error);
            }
        });

        Log.i(TAG, "指令发送器已切换为: " + commandSender.getClass().getSimpleName());
    }

    /**
     * 获取当前使用的指令发送器
     *
     * @return 当前的指令发送器
     */
    public CommandSender getCommandSender() {
        return commandSender;
    }

    /**
     * 切换到默认的串口发送器
     */
    public void useSerialSender() {
        setCommandSender(new LockCtlBoardSerialSender());
    }

    /**
     * 切换到指定配置的串口发送器
     *
     * @param devicePath 设备路径
     * @param baudRate   波特率
     */
    public void useSerialSender(String devicePath, int baudRate) {
        setCommandSender(new LockCtlBoardSerialSender(devicePath, baudRate));
    }

    /**
     * 获取连接状态信息
     *
     * @return 状态信息
     */
    public String getConnectionInfo() {
        if (commandSender == null) {
            return "未初始化指令发送器";
        }

        return String.format("LockCtlBoardUtil{sender=%s, connected=%s, bufferSize=%d}",
                commandSender.getClass().getSimpleName(),
                commandSender.isConnected(),
                getBufferSize());
    }

    // 打开串口（兼容性方法，现在通过CommandSender工作）
    public void openSerialPort() {
        Log.i(TAG, "通过CommandSender打开串口连接");

        // 如果当前使用的是串口发送器，确保其连接状态
        if (commandSender instanceof LockCtlBoardSerialSender) {
            LockCtlBoardSerialSender serialSender = (LockCtlBoardSerialSender) commandSender;
            if (!serialSender.isConnected()) {
                Log.i(TAG, "重新连接串口发送器");
                serialSender.reconnect();
            }
        } else {
            Log.w(TAG, "当前使用的不是串口发送器，忽略openSerialPort调用");
        }
    }

    // 关闭串口（兼容性方法，现在通过CommandSender工作）
    public void closeSerialPort() {
        Log.i(TAG, "通过CommandSender关闭连接");

        if (commandSender != null) {
            commandSender.disconnect();
        }

        // 清空数据缓冲区
        synchronized (dataBuffer) {
            dataBuffer.clear();
        }

        Log.i(TAG, "连接已关闭");
    }

    // 检查串口是否已打开（兼容性方法，现在通过CommandSender工作）
    public boolean isSerialPortOpen() {
        return commandSender != null && commandSender.isConnected();
    }

    // 清空数据缓冲区
    public void clearDataBuffer() {
        synchronized (dataBuffer) {
            dataBuffer.clear();
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
     * 通过CommandSender发送指令的统一方法
     *
     * @param command   指令数据
     * @param operation 操作描述（用于日志）
     * @return 操作是否成功
     */
    private boolean sendCommandViaSender(byte[] command, String operation) {
        if (command == null) {
            Log.e(TAG, "指令数据为空: " + operation);
            return false;
        }

        if (commandSender == null) {
            Log.e(TAG, "指令发送器未初始化: " + operation);
            return false;
        }

        if (!commandSender.isConnected()) {
            Log.e(TAG, "指令发送器未连接: " + operation);
            return false;
        }

        try {
            commandSender.sendCommand(command);
            Log.d(TAG, "指令已发送: " + operation);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送指令失败: " + operation, e);
            return false;
        }
    }

    /**
     * 1. 同时开多锁
     * <p>
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

        Log.i(TAG, "同时开启锁: " + Arrays.toString(lockIds));

        // 使用统一的发送方法
        return sendCommandViaSender(command, "同时开启锁: " + Arrays.toString(lockIds));
    }

    /**
     * 2. 锁通道LED闪烁
     * <p>
     * 注意：LED闪烁功能只支持接入设备为LED，如果是锁将会不断处于解锁状态，请勿使用此功能（必须重启才能重置）
     *
     * @param channelId 通道ID
     * @return 操作是否成功
     */
    public boolean flashLockLed(int channelId) {
        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildFlashChannelCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造通道闪烁指令失败");
            return false;
        }

        Log.i(TAG, "LED闪烁: 通道 " + channelId);

        // 使用统一的发送方法
        return sendCommandViaSender(command, "LED闪烁: 通道 " + channelId);
    }

    /**
     * 3. 单独开一把锁
     *
     * @param channelId 通道ID
     * @return 操作是否成功
     */
    public boolean openSingleLock(int channelId) {
        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造开单锁指令失败");
            return false;
        }

        Log.i(TAG, "开启锁: " + channelId);

        // 使用统一的发送方法
        return sendCommandViaSender(command, "开启锁: " + channelId);
    }

    /**
     * 4. 查询单个门锁状态
     *
     * @param channelId 通道ID
     * @return 门锁状态（0-关闭，1-打开，-1-错误）
     */
    public boolean getSingleLockStatus(int channelId) {
        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造查询单锁状态指令失败");
            return false;
        }

        Log.i(TAG, "查询状态: 锁 " + channelId);

        // 使用统一的发送方法
        return sendCommandViaSender(command, "查询状态: 锁 " + channelId);
    }

    /**
     * 5. 查询所有门锁状态
     *
     * @return 所有门锁状态数组
     */
    public boolean getAllLocksStatus() {
        Log.i(TAG, "查询所有锁状态");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造查询所有锁状态指令失败");
            return false;
        }

        // 使用统一的发送方法
        return sendCommandViaSender(command, "查询所有锁状态");
    }

    /**
     * 6. 全部开锁（逐一打开）
     *
     * @return 操作是否成功
     */
    public boolean openAllLocksSequentially() {
        Log.i(TAG, "开启所有锁");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenAllLocksCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造开全部锁指令失败");
            return false;
        }

        // 使用统一的发送方法
        return sendCommandViaSender(command, "开启所有锁");
    }

    /**
     * 7. 开多锁，逐一打开（可变长参数）
     * <p>
     * 这个功能可以支持多锁打开，不同于同时打开的方法，需要开多锁时优先使用该方法
     *
     * @param lockIds 门锁ID数组（可变长参数）
     * @return 操作是否成功
     */
    public boolean openMultipleLocksSequentially(int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return false;
        }

        Log.i(TAG, "依次开启锁: " + Arrays.toString(lockIds));

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand((byte) 0x00, lockIds);
        if (command == null) {
            Log.e(TAG, "构造逐一开多锁指令失败");
            return false;
        }

        // 使用统一的发送方法
        return sendCommandViaSender(command, "依次开启锁: " + Arrays.toString(lockIds));
    }

    /**
     * 8. 通道持续打开，适用于继电器、灯具等场景，不可以用于控锁
     *
     * @param channelId 通道ID
     * @return 操作是否成功
     */
    public boolean keepChannelOpen(int channelId) {
        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildChannelKeepOpenCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造通道持续打开指令失败");
            return false;
        }

        Log.i(TAG, "持续打开通道: " + channelId);

        // 使用统一的发送方法
        return sendCommandViaSender(command, "持续打开通道: " + channelId);
    }

    /**
     * 9. 关闭通道
     *
     * @param channelId 通道ID
     * @return 操作是否成功
     */
    public boolean closeChannel(int channelId) {
        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildCloseChannelCommand((byte) 0x00, channelId);
        if (command == null) {
            Log.e(TAG, "构造关闭通道指令失败");
            return false;
        }

        Log.i(TAG, "关闭通道: " + channelId);

        // 使用统一的发送方法
        return sendCommandViaSender(command, "关闭通道: " + channelId);
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
        } else {
            Log.w(TAG, "收到无效响应帧");
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
        if (bytes == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

}