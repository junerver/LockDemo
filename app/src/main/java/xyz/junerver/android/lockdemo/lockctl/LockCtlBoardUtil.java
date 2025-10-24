package xyz.junerver.android.lockdemo.lockctl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.junerver.android.lockdemo.lockctl.debounce.CommandDebounceManager;
import xyz.junerver.android.lockdemo.lockctl.debounce.CommandSender;
import xyz.junerver.android.lockdemo.lockctl.debounce.LockCtlBoardSerialSender;
import xyz.junerver.android.lockdemo.lockctl.debounce.OnResponseListener;

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

    // 新的指令发送器接口 - 延迟初始化
    private CommandSender commandSender;

    private OnDataReceived mOnDataReceived;

    private CommandDebounceManager commandDebounceManager;

    // 串口检测器
    private SerialPortDetector serialPortDetector;

    private boolean useDebounce = false;

    // 数据缓冲区，用于处理分包数据
    private final List<Byte> dataBuffer = new ArrayList<>();

    // 初始化状态标志
    private volatile boolean isInitialized = false;

    // 常量定义
    private static final int MAX_BUFFER_SIZE = 1024;
    private static final int BUFFER_CLEANUP_THRESHOLD = 100;
    private static final byte[] FRAME_HEADER = {0x57, 0x4B, 0x4C, 0x59};
    private static final int MIN_LOCK_ID = 1;
    private static final int MAX_LOCK_ID = 12;

    public static final String TYPE_CONNECT_SUCCESSES = "connect_successes";
    public static final String TYPE_CONNECT_FAILED = "connect_failed";
    public static final String TYPE_CONNECT_CLOSED = "connect_closed";
    public static final String TYPE_DETECT_SUCCESSES = "detect_successes";
    public static final String TYPE_DETECT_FAILED = "detect_failed";

    public interface OnDataReceived {
        void onDataReceived(String json);
    }

    public interface OnInitListener {
        void onSuccess(String message);

        void onError(String error);

        void onProgress(String message);
    }

    public interface OnPortDetectedListener {
        void onPortDetected(String portPath);

        void onDetectionFailed(String error);

        void onDetectionProgress(String currentPort);
    }

    // 默认门锁总数
    private static final int DEFAULT_LOCK_COUNT = 12;

    // 单例私有构造函数
    private LockCtlBoardUtil() {
        // 延迟初始化，不在构造函数中创建 CommandSender
        Log.d(TAG, "LockCtlBoardUtil 实例已创建，等待初始化");
    }

    /**
     * 初始化串口检测器
     *
     * @param context 应用上下文
     */
    public void initSerialPortDetector(Context context) {
        this.serialPortDetector = new SerialPortDetector(context);
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
     * 简化的初始化方法 - 自动检测串口并连接
     * 将串口检测、连接等内部逻辑封装，对外只提供统一的响应回调
     *
     * @param context 应用上下文
     */
    public void initialize(Context context) {
        Log.i(TAG, "开始简化初始化流程");

        // 使用自动检测方式初始化
        initWithAutoDetection(context, new OnInitListener() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "串口检测成功: " + message);

                // 发送关键状态通知：串口检测成功
                sendStatusNotification(TYPE_DETECT_SUCCESSES, message);

                // 自动连接串口
                autoConnectSerialPort();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "串口检测失败: " + error);
                // 检测失败也通知上层
                sendStatusNotification(TYPE_DETECT_FAILED, error);
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG, "检测进度: " + message);
                // 进度信息只记录日志，不通知上层
            }
        });
    }

    /**
     * 简化的初始化方法 - 使用指定串口并连接
     *
     * @param context  应用上下文
     * @param portPath 串口路径
     */
    public void initialize(Context context, String portPath) {
        Log.i(TAG, "开始简化初始化流程，指定串口: " + portPath);

        // 使用指定串口方式初始化
        initWithSpecifiedPort(context, portPath, new OnInitListener() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "指定串口初始化成功: " + message);

                // 发送关键状态通知：串口检测成功
                sendStatusNotification(TYPE_DETECT_SUCCESSES, message);

                // 自动连接串口
                autoConnectSerialPort();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "指定串口初始化失败: " + error);
                // 初始化失败也通知上层
                sendStatusNotification(TYPE_DETECT_FAILED, error);
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG, "初始化进度: " + message);
                // 进度信息只记录日志，不通知上层
            }
        });
    }

    /**
     * 强制重新检测并初始化
     *
     * @param context 应用上下文
     */
    public void forceReinitialize(Context context) {
        Log.i(TAG, "开始强制重新初始化");

        forceRedetectAndInit(context, new OnInitListener() {
            @Override
            public void onSuccess(String message) {
                Log.i(TAG, "强制重新初始化成功: " + message);

                // 发送关键状态通知：串口检测成功
                sendStatusNotification(TYPE_DETECT_SUCCESSES, message);

                // 自动连接串口
                autoConnectSerialPort();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "强制重新初始化失败: " + error);
                // 重新初始化失败也通知上层
                sendStatusNotification(TYPE_DETECT_FAILED, error);
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG, "重新初始化进度: " + message);
                // 进度信息只记录日志，不通知上层
            }
        });
    }

    /**
     * 自动连接串口（内部方法）
     */
    private void autoConnectSerialPort() {
        try {
            Log.i(TAG, "开始自动连接串口");

            openSerialPort();

            // 延迟检查连接状态，确保连接建立
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (isSerialPortOpen()) {
                    Log.i(TAG, "串口连接成功");

                    // 发送关键状态通知：连接成功
                    sendStatusNotification(TYPE_CONNECT_SUCCESSES, "门锁控制板已连接，可以开始使用");
//
//                    // 查询所有锁状态作为初始化完成的验证
//                    getAllLocksStatus();
                } else {
                    Log.e(TAG, "串口连接失败");

                    // 发送关键状态通知：连接失败
                    sendStatusNotification(TYPE_CONNECT_FAILED, "串口连接失败，请检查设备连接");
                }
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "自动连接串口异常", e);

            // 发送关键状态通知：连接异常
            sendStatusNotification(TYPE_CONNECT_FAILED, "串口连接异常: " + e.getMessage());
        }
    }

    /**
     * 发送状态通知（内部方法）
     *
     * @param status  状态类型
     * @param message 详细消息
     */
    private void sendStatusNotification(String status, String message) {
        if (mOnDataReceived != null) {
            try {
                // 构造状态通知的JSON格式，使用英文字段
                String statusJson = String.format(
                        "{\"type\":\"connection\",\"event\":\"%s\",\"message\":\"%s\"}",
                        status, message
                );

                // 通过统一的响应回调发送状态通知
                mOnDataReceived.onDataReceived(statusJson);

            } catch (Exception e) {
                Log.e(TAG, "发送状态通知失败", e);
            }
        }
    }

    /**
     * 使用指定的串口路径初始化
     *
     * @param context  应用上下文
     * @param portPath 串口路径
     * @param listener 初始化结果监听器
     */
    public void initWithSpecifiedPort(Context context, String portPath, OnInitListener listener) {
        if (isInitialized) {
            Log.w(TAG, "LockCtlBoardUtil 已经初始化，跳过重复初始化");
            if (listener != null) {
                listener.onSuccess("已初始化");
            }
            return;
        }

        Log.i(TAG, "使用指定串口路径初始化: " + portPath);

        // 初始化串口检测器（用于保存路径）
        this.serialPortDetector = new SerialPortDetector(context);

        // 保存指定的串口路径
        this.serialPortDetector.savePortPath(portPath);

        // 创建 CommandSender
        createCommandSenderWithPort(portPath);

        // 标记为已初始化
        isInitialized = true;

        if (listener != null) {
            listener.onSuccess("使用指定串口初始化成功: " + portPath);
        }
    }

    /**
     * 自动检测串口并初始化
     *
     * @param context  应用上下文
     * @param listener 初始化结果监听器
     */
    public void initWithAutoDetection(Context context, OnInitListener listener) {
        if (isInitialized) {
            Log.w(TAG, "LockCtlBoardUtil 已经初始化，跳过重复初始化");
            if (listener != null) {
                listener.onSuccess("已初始化");
            }
            return;
        }

        Log.i(TAG, "开始自动检测串口并初始化");

        // 初始化串口检测器
        this.serialPortDetector = new SerialPortDetector(context);

        // 开始检测
        serialPortDetector.autoDetectPort(new SerialPortDetector.OnDetectionListener() {
            @Override
            public void onPortDetected(String portPath) {
                Log.i(TAG, "检测到串口设备: " + portPath);

                // 创建 CommandSender
                createCommandSenderWithPort(portPath);

                // 标记为已初始化
                isInitialized = true;

                if (listener != null) {
                    listener.onSuccess("自动检测并初始化成功: " + portPath);
                }
            }

            @Override
            public void onDetectionFailed(String error) {
                Log.e(TAG, "串口检测失败: " + error);
                isInitialized = false;

                if (listener != null) {
                    listener.onError("串口检测失败: " + error);
                }
            }

            @Override
            public void onDetectionProgress(String currentPort) {
                Log.d(TAG, "正在检测串口: " + currentPort);
                if (listener != null) {
                    listener.onProgress("正在检测串口: " + currentPort);
                }
            }
        });
    }

    /**
     * 强制重新检测串口并初始化
     *
     * @param context  应用上下文
     * @param listener 初始化结果监听器
     */
    public void forceRedetectAndInit(Context context, OnInitListener listener) {
        Log.i(TAG, "强制重新检测串口并初始化");

        // 清除已保存的路径
        if (serialPortDetector != null) {
            serialPortDetector.clearSavedPortPath();
        }

        // 重置初始化状态
        isInitialized = false;

        // 断开当前连接
        if (commandSender != null) {
            commandSender.disconnect();
            commandSender = null;
        }

        // 重新开始自动检测
        initWithAutoDetection(context, listener);
    }

    /**
     * 设置 CommandSender 的响应监听器（提取公共代码）
     *
     * @param sender CommandSender 实例
     */
    private void setupResponseListener(CommandSender sender) {
        if (sender != null) {
            sender.setOnResponseListener(new OnResponseListener() {
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
     * 初始化防抖管理器
     *
     * @param sender CommandSender 实例
     */
    private void initDebounceManager(CommandSender sender) {
        if (sender != null) {
            commandDebounceManager = new CommandDebounceManager(sender);
        }
    }

    /**
     * 使用指定的串口路径创建 CommandSender
     *
     * @param portPath 串口路径
     */
    private void createCommandSenderWithPort(String portPath) {
        try {
            // 关闭旧的 CommandSender
            if (commandSender != null) {
                commandSender.disconnect();
            }

            // 创建新的 CommandSender
            commandSender = new LockCtlBoardSerialSender(portPath);

            // 使用公共方法设置响应监听器
            setupResponseListener(commandSender);

            // 使用公共方法初始化防抖管理器
            initDebounceManager(commandSender);

            Log.i(TAG, "CommandSender 创建成功，串口路径: " + portPath);
        } catch (Exception e) {
            Log.e(TAG, "创建 CommandSender 失败", e);
            throw new RuntimeException("创建 CommandSender 失败: " + e.getMessage(), e);
        }
    }

    public void setUseDebounce(boolean useDebounce) {
        this.useDebounce = useDebounce;
    }

    public boolean isUseDebounce() {
        return useDebounce;
    }

    /**
     * 获取初始化状态
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    // 打开串口（兼容性方法，现在通过CommandSender工作）
    public void openSerialPort() {
        if (!isInitialized) {
            Log.e(TAG, "LockCtlBoardUtil 未初始化，无法打开串口");
            return;
        }

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

        // 发送关键状态通知：串口关闭
        sendStatusNotification(TYPE_CONNECT_CLOSED, "门锁控制板连接已断开");
    }

    // 检查串口是否已打开（兼容性方法，现在通过CommandSender工作）
    public boolean isSerialPortOpen() {
        return isInitialized && commandSender != null && commandSender.isConnected();
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
     * 自动检测串口设备
     *
     * @param listener 检测结果监听器
     */
    public void autoDetectSerialPort(OnPortDetectedListener listener) {
        if (serialPortDetector == null) {
            Log.e(TAG, "串口检测器未初始化，请先调用 initSerialPortDetector()");
            if (listener != null) {
                listener.onDetectionFailed("串口检测器未初始化");
            }
            return;
        }

        serialPortDetector.autoDetectPort(new SerialPortDetector.OnDetectionListener() {
            @Override
            public void onPortDetected(String portPath) {
                Log.i(TAG, "检测到串口设备: " + portPath);

                // 关闭当前连接
                if (commandSender != null && commandSender.isConnected()) {
                    commandSender.disconnect();
                }

                // 创建新的串口发送器，使用检测到的路径
                commandSender = new LockCtlBoardSerialSender(portPath);

                // 使用公共方法设置响应监听器和防抖管理器
                setupResponseListener(commandSender);
                initDebounceManager(commandSender);

                if (listener != null) {
                    listener.onPortDetected(portPath);
                }
            }

            @Override
            public void onDetectionFailed(String error) {
                Log.e(TAG, "串口检测失败: " + error);
                if (listener != null) {
                    listener.onDetectionFailed(error);
                }
            }

            @Override
            public void onDetectionProgress(String currentPort) {
                if (listener != null) {
                    listener.onDetectionProgress(currentPort);
                }
            }
        });
    }

    /**
     * 强制重新检测串口设备（忽略已保存的路径）
     *
     * @param listener 检测结果监听器
     */
    public void forceRedetectSerialPort(OnPortDetectedListener listener) {
        if (serialPortDetector == null) {
            Log.e(TAG, "串口检测器未初始化，请先调用 initSerialPortDetector()");
            if (listener != null) {
                listener.onDetectionFailed("串口检测器未初始化");
            }
            return;
        }

        serialPortDetector.forceRedetectPort(new SerialPortDetector.OnDetectionListener() {
            @Override
            public void onPortDetected(String portPath) {
                Log.i(TAG, "重新检测到串口设备: " + portPath);

                // 关闭当前连接
                if (commandSender != null && commandSender.isConnected()) {
                    commandSender.disconnect();
                }

                // 创建新的串口发送器，使用检测到的路径
                commandSender = new LockCtlBoardSerialSender(portPath);

                // 使用公共方法设置响应监听器和防抖管理器
                setupResponseListener(commandSender);
                initDebounceManager(commandSender);

                if (listener != null) {
                    listener.onPortDetected(portPath);
                }
            }

            @Override
            public void onDetectionFailed(String error) {
                Log.e(TAG, "串口重新检测失败: " + error);
                if (listener != null) {
                    listener.onDetectionFailed(error);
                }
            }

            @Override
            public void onDetectionProgress(String currentPort) {
                if (listener != null) {
                    listener.onDetectionProgress(currentPort);
                }
            }
        });
    }

    /**
     * 获取当前已保存的串口路径
     *
     * @return 已保存的串口路径，如果没有保存则返回null
     */
    public String getSavedPortPath() {
        if (serialPortDetector == null) {
            return null;
        }
        return serialPortDetector.getSavedPortPath();
    }

    /**
     * 清除已保存的串口路径
     */
    public void clearSavedPortPath() {
        if (serialPortDetector != null) {
            serialPortDetector.clearSavedPortPath();
        }
    }

    /**
     * 通过CommandSender发送指令的统一方法
     *
     * @param command   指令数据
     * @param operation 操作描述（用于日志）
     * @return 操作是否成功
     */
    private boolean sendCommandViaSender(byte[] command, String operation) {
        // 详细的参数验证
        if (!isInitialized) {
            Log.e(TAG, "LockCtlBoardUtil 未初始化，操作失败: " + operation);
            return false;
        }

        if (command == null) {
            Log.e(TAG, "指令数据为null，操作失败: " + operation);
            return false;
        }

        if (command.length == 0) {
            Log.e(TAG, "指令数据为空数组，操作失败: " + operation);
            return false;
        }

        if (commandSender == null) {
            Log.e(TAG, "指令发送器未初始化，操作失败: " + operation);
            return false;
        }

        if (!commandSender.isConnected()) {
            Log.e(TAG, "指令发送器未连接，操作失败: " + operation +
                    "。发送器类型: " + commandSender.getClass().getSimpleName());
            return false;
        }

        try {
            if (useDebounce) {
                if (commandDebounceManager == null) {
                    Log.e(TAG, "防抖管理器未初始化，操作失败: " + operation);
                    return false;
                }
                commandDebounceManager.sendCommand(command, null);
                Log.d(TAG, "指令已通过防抖管理器发送: " + operation);
            } else {
                commandSender.sendCommand(command);
                Log.d(TAG, "指令已直接发送: " + operation);
            }
            return true;
        } catch (IllegalStateException e) {
            Log.e(TAG, "指令发送器状态异常: " + operation + ", 错误: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "发送指令失败: " + operation + ", 错误类型: " + e.getClass().getSimpleName() +
                    ", 错误信息: " + e.getMessage(), e);
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
        // 使用新的参数验证方法
        if (!areValidLockIds(lockIds)) {
            Log.e(TAG, "门锁ID参数无效");
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
        // 使用新的参数验证方法
        if (!isValidChannelId(channelId)) {
            Log.e(TAG, "通道ID参数无效: " + channelId);
            return false;
        }

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
        // 使用新的参数验证方法
        if (!isValidChannelId(channelId)) {
            Log.e(TAG, "通道ID参数无效: " + channelId);
            return false;
        }

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
        // 使用新的参数验证方法
        if (!isValidChannelId(channelId)) {
            Log.e(TAG, "通道ID参数无效: " + channelId);
            return false;
        }

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
        // 使用新的参数验证方法
        if (!areValidLockIds(lockIds)) {
            Log.e(TAG, "门锁ID参数无效");
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
        // 使用新的参数验证方法
        if (!isValidChannelId(channelId)) {
            Log.e(TAG, "通道ID参数无效: " + channelId);
            return false;
        }

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
        // 使用新的参数验证方法
        if (!isValidChannelId(channelId)) {
            Log.e(TAG, "通道ID参数无效: " + channelId);
            return false;
        }

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
        // 检查缓冲区大小，防止无限增长
        if (dataBuffer.size() > MAX_BUFFER_SIZE) {
            Log.w(TAG, "缓冲区溢出，清空数据。当前大小: " + dataBuffer.size());
            dataBuffer.clear();
            return;
        }

        // 查找起始符位置 (57 4B 4C 59)
        int startIndex = findFrameHeader();

        // 如果没找到起始符，清理部分缓冲区
        if (startIndex == -1) {
            if (dataBuffer.size() > BUFFER_CLEANUP_THRESHOLD) {
                Log.d(TAG, "未找到有效帧头，清理缓冲区");
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

        // 验证帧长度的合理性
        if (frameLength < 5 || frameLength > 256) {
            Log.w(TAG, "无效的帧长度: " + frameLength + "，跳过该帧");
            dataBuffer.subList(0, Math.min(startIndex + 1, dataBuffer.size())).clear();
            return;
        }

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
     * 查找帧头位置
     *
     * @return 帧头起始位置，未找到返回-1
     */
    private int findFrameHeader() {
        for (int i = 0; i <= dataBuffer.size() - FRAME_HEADER.length; i++) {
            boolean match = true;
            for (int j = 0; j < FRAME_HEADER.length; j++) {
                if (dataBuffer.get(i + j) != FRAME_HEADER[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 验证门锁ID是否有效
     *
     * @param lockId 门锁ID
     * @return 是否有效
     */
    private boolean isValidLockId(int lockId) {
        return lockId >= MIN_LOCK_ID && lockId <= MAX_LOCK_ID;
    }

    /**
     * 验证多个门锁ID是否有效
     *
     * @param lockIds 门锁ID数组
     * @return 是否全部有效
     */
    private boolean areValidLockIds(int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            return false;
        }
        for (int lockId : lockIds) {
            if (!isValidLockId(lockId)) {
                Log.w(TAG, "无效的门锁ID: " + lockId + "，有效范围: " + MIN_LOCK_ID + "-" + MAX_LOCK_ID);
                return false;
            }
        }
        return true;
    }

    /**
     * 验证通道ID是否有效（与门锁ID使用相同的范围）
     *
     * @param channelId 通道ID
     * @return 是否有效
     */
    private boolean isValidChannelId(int channelId) {
        return isValidLockId(channelId);
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