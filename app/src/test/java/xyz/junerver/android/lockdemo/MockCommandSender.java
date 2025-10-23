package xyz.junerver.android.lockdemo;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import xyz.junerver.android.lockdemo.lockctl.debounce.CommandExecutionStrategy;
import xyz.junerver.android.lockdemo.lockctl.debounce.CommandSender;
import xyz.junerver.android.lockdemo.lockctl.debounce.OnResponseListener;

/**
 * Mock指令发送器
 * 用于测试环境，模拟真实的指令发送和响应过程
 * 支持延迟响应、错误模拟、响应记录等功能
 */
public class MockCommandSender implements CommandSender {
    private static final String TAG = "MockCommandSender";

    // 配置参数
    private long defaultResponseDelay = 100; // 默认响应延迟（毫秒）
    private boolean useCustomDelay = false;  // 是否使用自定义延迟（用于测试）
    private boolean simulateErrors = false;  // 是否模拟错误
    private double errorRate = 0.0;          // 错误率（0.0-1.0）

    // 状态管理
    private OnResponseListener responseListener;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final List<byte[]> sentCommands = new ArrayList<>();
    private final List<CommandRecord> commandHistory = new ArrayList<>();

    // 线程池：用于模拟延迟响应
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "MockCommandSender-Scheduler");
        thread.setDaemon(false);  // 改为非守护线程，确保测试环境正常工作
        return thread;
    });

    /**
     * 指令记录类
     */
    public static class CommandRecord {
        public final byte[] command;
        public final long timestamp;
        public final byte[] simulatedResponse;
        public final long responseDelay;

        public CommandRecord(byte[] command, long timestamp, byte[] simulatedResponse, long responseDelay) {
            this.command = command.clone();
            this.timestamp = timestamp;
            this.simulatedResponse = simulatedResponse != null ? simulatedResponse.clone() : null;
            this.responseDelay = responseDelay;
        }
    }

    /**
     * 构造函数
     */
    public MockCommandSender() {
        this(true);
    }

    /**
     * 构造函数
     *
     * @param autoConnect 是否自动连接
     */
    public MockCommandSender(boolean autoConnect) {
        if (autoConnect) {
            connect();
        }
        Log.d(TAG, "MockCommandSender已创建，自动连接: " + autoConnect);
    }

    /**
     * 连接
     */
    public void connect() {
        isConnected.set(true);
        Log.d(TAG, "MockCommandSender已连接");
    }

    @Override
    public void sendCommand(byte[] command) {
        if (!isConnected.get()) {
            Log.w(TAG, "未连接，拒绝发送指令");
            if (responseListener != null) {
                responseListener.onError("未连接");
            }
            return;
        }

        if (command == null) {
            Log.w(TAG, "指令为null");
            return;
        }

        // 记录发送的指令
        synchronized (sentCommands) {
            sentCommands.add(command.clone());
        }

        Log.d(TAG, String.format("发送指令: %s, 长度: %d",
                CommandExecutionStrategy.getCommandDescription(command.length > 6 ? command[6] : 0),
                command.length));

        // 模拟发送延迟和响应
        simulateResponse(command);
    }

    /**
     * 计算响应延迟
     *
     * @param command 指令数据
     * @return 延迟时间（毫秒）
     */
    private long calculateResponseDelay(byte[] command) {
        // 如果设置了自定义延迟，直接返回（用于测试超时等场景）
        if (useCustomDelay) {
            return defaultResponseDelay;
        }

        if (command == null || command.length < 7) {
            return defaultResponseDelay;
        }

        byte commandByte = command[6];

        // 根据指令类型设置不同的延迟时间
        switch (commandByte) {
            case (byte) 0x82: // 开单个锁
                return 350 + (long) (Math.random() * 100); // 350-450ms

            case (byte) 0x83: // 查询单个门状态
                return 80 + (long) (Math.random() * 40); // 80-120ms

            case (byte) 0x84: // 查询所有门状态
                return 180 + (long) (Math.random() * 40); // 180-220ms

            case (byte) 0x80: // 同时开多锁
                return 350 + (long) (Math.random() * 100); // 350-450ms

            case (byte) 0x86: // 逐一开多锁
            case (byte) 0x87: // 逐一开多锁 (实际使用)
                int lockCount = command.length > 7 ? command[7] & 0xFF : 1;
                return (350 * lockCount) + (long) (Math.random() * 100); // 基础时间 + 随机延迟

            case (byte) 0x81: // 通道闪烁
            case (byte) 0x88: // 通道常开
            case (byte) 0x89: // 通道关闭
                return 80 + (long) (Math.random() * 40); // 80-120ms

            default:
                return defaultResponseDelay;
        }
    }

    /**
     * 生成模拟响应
     *
     * @param command 原始指令
     * @return 模拟响应数据
     */
    private byte[] generateMockResponse(byte[] command) {
        if (command == null || command.length < 7) {
            return createErrorResponse();
        }

        byte commandByte = command[6];
        byte boardAddress = command[5];

        // 根据指令类型生成对应的响应
        switch (commandByte) {
            case (byte) 0x80: // 同时开多锁
                return createResponse(boardAddress, commandByte, new byte[]{0x00}); // 成功状态

            case (byte) 0x81: // 通道闪烁
                byte channelId = command.length > 7 ? command[7] : 0;
                return createResponse(boardAddress, commandByte, new byte[]{0x00, channelId});

            case (byte) 0x82: // 开单个锁
                channelId = command.length > 7 ? command[7] : 0;
                byte lockStatus = (byte) (Math.random() > 0.5 ? 0x00 : 0x01); // 随机锁状态
                return createResponse(boardAddress, commandByte, new byte[]{0x00, channelId, lockStatus});

            case (byte) 0x83: // 查询单个门状态
                channelId = command.length > 7 ? command[7] : 0;
                lockStatus = (byte) (Math.random() > 0.3 ? 0x01 : 0x00); // 70%概率关闭
                return createResponse(boardAddress, commandByte, new byte[]{0x00, channelId, lockStatus});

            case (byte) 0x84: // 查询所有门状态
                byte channelCount = 12; // 假设12个通道
                byte[] channels = new byte[channelCount];
                for (int i = 0; i < channelCount; i++) {
                    channels[i] = (byte) (Math.random() > 0.3 ? 0x01 : 0x00); // 70%概率关闭
                }
                byte[] allStatusData = new byte[1 + channelCount];
                allStatusData[0] = channelCount;
                System.arraycopy(channels, 0, allStatusData, 1, channelCount);
                return createResponse(boardAddress, commandByte, allStatusData);

            case (byte) 0x86: // 逐一开多锁
                return createResponse(boardAddress, commandByte, new byte[]{0x00});

            case (byte) 0x88: // 通道常开
            case (byte) 0x89: // 通道关闭
                channelId = command.length > 7 ? command[7] : 0;
                return createResponse(boardAddress, commandByte, new byte[]{0x00, channelId});

            default:
                return createErrorResponse();
        }
    }

    /**
     * 创建响应数据
     *
     * @param boardAddress 板地址
     * @param commandByte  指令字
     * @param data         数据域
     * @return 完整的响应数据
     */
    private byte[] createResponse(byte boardAddress, byte commandByte, byte[] data) {
        byte[] startBytes = {0x57, 0x4B, 0x4C, 0x59};
        int dataLength = data != null ? data.length : 0;
        int frameLength = startBytes.length + 1 + 1 + 1 + dataLength + 1;

        byte[] response = new byte[frameLength];

        // 起始符
        System.arraycopy(startBytes, 0, response, 0, startBytes.length);

        // 帧长度
        response[4] = (byte) (frameLength & 0xFF);

        // 板地址
        response[5] = boardAddress;

        // 指令字
        response[6] = commandByte;

        // 数据域
        if (data != null && data.length > 0) {
            System.arraycopy(data, 0, response, 7, data.length);
        }

        // 校验字节
        byte checksum = 0;
        for (int i = 0; i < frameLength - 1; i++) {
            checksum ^= response[i];
        }
        response[frameLength - 1] = checksum;

        return response;
    }

    /**
     * 创建错误响应
     *
     * @return 错误响应数据
     */
    private byte[] createErrorResponse() {
        byte[] startBytes = {0x57, 0x4B, 0x4C, 0x59};
        byte[] response = new byte[8]; // 最小响应帧

        System.arraycopy(startBytes, 0, response, 0, startBytes.length);
        response[4] = 8; // 帧长度
        response[5] = 0x00; // 板地址
        response[6] = 0x00; // 指令字
        response[7] = (byte) 0xFF; // 错误状态

        // 计算校验
        byte checksum = 0;
        for (int i = 0; i < 7; i++) {
            checksum ^= response[i];
        }
        response[7] = checksum;

        return response;
    }

    /**
     * 判断是否应该模拟错误
     *
     * @return 是否模拟错误
     */
    private boolean shouldSimulateError() {
        return simulateErrors && Math.random() < errorRate;
    }

    @Override
    public void setOnResponseListener(OnResponseListener listener) {
        this.responseListener = listener;
    }

    @Override
    public OnResponseListener getOnResponseListener() {
        return this.responseListener;
    }

    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    @Override
    public void disconnect() {
        isConnected.set(false);
        Log.d(TAG, "MockCommandSender已断开连接");
    }

    /**
     * 手动模拟响应
     *
     * @param command 要响应的指令（可以为null，响应最后一条指令）
     */
    public void simulateResponse(byte[] command) {
        if (command == null) {
            synchronized (sentCommands) {
                if (!sentCommands.isEmpty()) {
                    command = sentCommands.get(sentCommands.size() - 1);
                } else {
                    Log.w(TAG, "没有可模拟响应的指令");
                    return;
                }
            }
        }

        final byte[] finalCommand = command;

        // 计算响应延迟
        long responseDelay = calculateResponseDelay(finalCommand);

        // 生成响应数据
        byte[] response = generateMockResponse(finalCommand);

        // 记录到历史
        synchronized (commandHistory) {
            commandHistory.add(new CommandRecord(finalCommand, System.currentTimeMillis(), response, responseDelay));
        }

        Log.d(TAG, String.format("计划在 %dms 后发送响应", responseDelay));

        // 使用调度器异步发送响应
        scheduler.schedule(() -> {
            Log.i(TAG, String.format("发送响应: 延迟=%dms, 指令字=0x%02X, 监听器=%s",
                    responseDelay, finalCommand.length > 6 ? finalCommand[6] & 0xFF : 0,
                    responseListener != null ? "有" : "无"));

            if (shouldSimulateError()) {
                Log.w(TAG, "模拟错误响应");
                if (responseListener != null) {
                    responseListener.onError("模拟的通信错误");
                }
            } else {
                Log.i(TAG, "发送成功响应");
                if (responseListener != null) {
                    responseListener.onResponseReceived(response);
                } else {
                    Log.w(TAG, "响应监听器为null，无法发送响应");
                }
            }
        }, responseDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * 手动模拟错误
     */
    public void simulateError() {
        if (responseListener != null) {
            responseListener.onError("手动模拟的错误");
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
     * 获取指令历史记录
     *
     * @return 指令历史记录的副本
     */
    public List<CommandRecord> getCommandHistory() {
        synchronized (commandHistory) {
            return new ArrayList<>(commandHistory);
        }
    }

    /**
     * 清空记录
     */
    public void clearRecords() {
        synchronized (sentCommands) {
            sentCommands.clear();
        }
        synchronized (commandHistory) {
            commandHistory.clear();
        }
        Log.d(TAG, "已清空所有记录");
    }

    /**
     * 设置配置参数
     */
    public void setDefaultResponseDelay(long delay) {
        this.defaultResponseDelay = Math.max(0, delay);
        this.useCustomDelay = true;  // 标记使用自定义延迟
    }

    /**
     * 重置为自动延迟模式（根据指令类型自动计算延迟）
     */
    public void resetToAutoDelay() {
        this.useCustomDelay = false;
        this.defaultResponseDelay = 100;
    }

    public void setSimulateErrors(boolean simulateErrors) {
        this.simulateErrors = simulateErrors;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = Math.max(0.0, Math.min(1.0, errorRate));
    }

    /**
     * 关闭Mock发送器
     */
    public void shutdown() {
        disconnect();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        Log.d(TAG, "MockCommandSender已关闭");
    }
}