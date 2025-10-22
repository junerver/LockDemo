package xyz.junerver.android.lockdemo.lockctl.debounce;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 指令防抖管理器
 * 核心组件，负责管理指令队列、执行时序控制和超时处理
 * 基于响应确认机制，确保指令按正确顺序执行
 */
public class CommandDebounceManager {
    private static final String TAG = "CommandDebounceManager";

    // 线程池：用于指令执行
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CommandDebounce-Executor");
        thread.setDaemon(false);  // 改为非守护线程，确保测试环境正常工作
        return thread;
    });

    // 定时器：用于超时控制
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "CommandDebounce-Timeout");
        thread.setDaemon(false);  // 改为非守护线程，确保测试环境正常工作
        return thread;
    });

    // 核心组件
    private final CommandSender underlyingSender;
    private final Queue<QueuedCommand> commandQueue = new LinkedList<>();
    private final Object queueLock = new Object();

    // 当前执行状态
    private volatile QueuedCommand currentExecutingCommand = null;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // 统计信息
    private volatile long totalCommandsSent = 0;
    private volatile long totalCommandsCompleted = 0;
    private volatile long totalTimeouts = 0;
    private volatile long totalErrors = 0;

    /**
     * 构造函数
     *
     * @param underlyingSender 底层指令发送器
     */
    public CommandDebounceManager(CommandSender underlyingSender) {
        if (underlyingSender == null) {
            throw new IllegalArgumentException("底层发送器不能为null");
        }

        this.underlyingSender = underlyingSender;
        setupResponseListener();

        Log.i(TAG, "指令防抖管理器已初始化");
    }

    /**
     * 发送指令（异步执行）
     *
     * @param command  指令数据
     * @param listener 执行监听器
     */
    public void sendCommand(byte[] command, OnCommandListener listener) {
        if (isShutdown.get()) {
            Log.w(TAG, "管理器已关闭，拒绝接收新指令");
            if (listener != null) {
                listener.onError("管理器已关闭");
            }
            return;
        }

        if (command == null || command.length < 7) {
            Log.e(TAG, "指令数据格式错误");
            if (listener != null) {
                listener.onError("指令数据格式错误");
            }
            return;
        }

        try {
            QueuedCommand queuedCommand = new QueuedCommand(command, listener);

            synchronized (queueLock) {
                commandQueue.offer(queuedCommand);
                totalCommandsSent++;

                Log.i(TAG, String.format("指令入队: %s, 队列大小: %d, 当前执行指令: %s",
                        queuedCommand.getDescription(), commandQueue.size(),
                        currentExecutingCommand != null ? currentExecutingCommand.getDescription() : "无"));
            }

            // 如果当前没有执行中的指令，启动处理流程
            if (currentExecutingCommand == null) {
                Log.i(TAG, "启动处理流程，提交 processNextCommand 任务");
                executor.submit(this::processNextCommand);
            } else {
                Log.i(TAG, "当前有指令在执行，等待队列处理");
            }

        } catch (Exception e) {
            Log.e(TAG, "创建队列指令失败", e);
            totalErrors++;
            if (listener != null) {
                listener.onError("创建队列指令失败: " + e.getMessage());
            }
        }
    }

    /**
     * 设置响应监听器
     *
     * @param listener 响应监听器
     */
    private void setupResponseListener() {
        underlyingSender.setOnResponseListener(new OnResponseListener() {
            @Override
            public void onResponseReceived(byte[] response) {
                handleResponse(response);
            }

            @Override
            public void onError(String error) {
                handleError(error);
            }
        });
    }

    /**
     * 处理下一个指令
     */
    private void processNextCommand() {
        if (isShutdown.get()) {
            Log.d(TAG, "管理器已关闭，停止处理指令");
            return;
        }

        synchronized (queueLock) {
            currentExecutingCommand = commandQueue.poll();
            if (currentExecutingCommand == null) {
                Log.d(TAG, "指令队列为空，等待新指令");
                return;
            }
        }

        Log.i(TAG, "开始执行指令: " + currentExecutingCommand.getDescription() +
                ", 当前总发送数: " + totalCommandsSent +
                ", 当前总完成数: " + totalCommandsCompleted);

        try {
            // 1. 发送指令
            underlyingSender.sendCommand(currentExecutingCommand.getCommandData());

            // 2. 设置超时控制
            scheduleTimeout(currentExecutingCommand);

        } catch (Exception e) {
            Log.e(TAG, "发送指令失败", e);
            totalErrors++;
            completeCurrentCommand(false, "发送指令失败: " + e.getMessage());
        }
    }

    /**
     * 设置超时控制
     *
     * @param command 当前执行的指令
     */
    private void scheduleTimeout(QueuedCommand command) {
        long timeout = command.getTimeout();
        Log.i(TAG, String.format("设置超时: %s, 超时时间: %dms", command.getDescription(), timeout));

        timeoutExecutor.schedule(() -> {
            Log.i(TAG, String.format("检查超时: %s, 当前指令=%s, 已完成=%s",
                    command.getDescription(),
                    currentExecutingCommand != null ? currentExecutingCommand.getDescription() : "无",
                    command.isCompleted()));

            if (currentExecutingCommand == command && !command.isCompleted()) {
                Log.w(TAG, String.format("指令执行超时: %s, 超时时间: %dms",
                        command.getDescription(), timeout));
                totalTimeouts++;
                command.setTimedOut(true);
                completeCurrentCommand(false, "指令执行超时");
            } else {
                Log.d(TAG, "超时检查通过，指令已完成或已更换");
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理收到的响应
     *
     * @param response 响应数据
     */
    private void handleResponse(byte[] response) {
        Log.i(TAG, "收到响应，当前执行指令: " +
                (currentExecutingCommand != null ? currentExecutingCommand.getDescription() : "无"));

        if (currentExecutingCommand == null) {
            Log.w(TAG, "收到响应但当前无执行中的指令");
            return;
        }

        // 检查响应是否匹配当前执行的指令
        boolean isMatch = ResponseMatcher.isResponseForCommand(response, currentExecutingCommand.getCommandData());
        Log.i(TAG, "响应匹配检查: " + (isMatch ? "匹配" : "不匹配"));

        if (isMatch) {
            boolean success = ResponseMatcher.isSuccessResponse(response);
            String message = ResponseMatcher.getResponseStatusDescription(response);

            // 记录详细信息
            int channelId = ResponseMatcher.getChannelId(response);
            int lockStatus = ResponseMatcher.getLockStatus(response);

            Log.i(TAG, String.format("指令执行完成: %s, 结果: %s, 通道: %d, 锁状态: %d",
                    currentExecutingCommand.getDescription(), message, channelId, lockStatus));

            completeCurrentCommand(success, message);

        } else {
            Log.w(TAG, "响应不匹配当前指令，可能是指令延迟或乱序");
        }
    }

    /**
     * 处理通信错误
     *
     * @param error 错误信息
     */
    private void handleError(String error) {
        Log.e(TAG, "通信错误: " + error);
        totalErrors++;
        if (currentExecutingCommand != null) {
            completeCurrentCommand(false, "通信错误: " + error);
        }
    }

    /**
     * 完成当前指令
     *
     * @param success 是否成功
     * @param message 结果消息
     */
    private void completeCurrentCommand(boolean success, String message) {
        QueuedCommand completedCommand = currentExecutingCommand;
        if (completedCommand != null) {
            completedCommand.setCompleted(true);
            totalCommandsCompleted++;

            // 通知监听器
            OnCommandListener listener = completedCommand.getListener();

            Log.i(TAG, "指令完成: " + completedCommand.getDescription() +
                    ", 完成总数: " + totalCommandsCompleted +
                    ", 监听器: " + (listener != null ? "有" : "无"));
            if (listener != null) {
                try {
                    if (success) {
                        listener.onSuccess();
                    } else {
                        listener.onError(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "通知指令监听器失败", e);
                }
            }

            currentExecutingCommand = null;

            // 继续处理下一个指令
            executor.submit(this::processNextCommand);
        }
    }

    /**
     * 获取队列状态
     *
     * @return 队列状态信息
     */
    public QueueStatus getStatus() {
        synchronized (queueLock) {
            return new QueueStatus(
                    commandQueue.size(),
                    currentExecutingCommand != null ? currentExecutingCommand.getCommandByte() : -1,
                    currentExecutingCommand != null && !currentExecutingCommand.isCompleted(),
                    totalCommandsSent,
                    totalCommandsCompleted,
                    totalTimeouts,
                    totalErrors
            );
        }
    }

    /**
     * 清空指令队列
     */
    public void clearQueue() {
        synchronized (queueLock) {
            int clearedCount = commandQueue.size();
            commandQueue.clear();

            Log.i(TAG, "清空指令队列，清除了 " + clearedCount + " 个待执行指令");

            // 通知所有被清除的指令监听器
            // 注意：这里简化处理，实际应用中可能需要更复杂的错误通知机制
        }
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            Log.i(TAG, "正在关闭指令防抖管理器...");

            // 清空队列
            clearQueue();

            // 关闭线程池
            executor.shutdown();
            timeoutExecutor.shutdown();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                timeoutExecutor.shutdownNow();
            }

            // 断开底层连接
            if (underlyingSender != null) {
                underlyingSender.disconnect();
            }

            Log.i(TAG, "指令防抖管理器已关闭");
        }
    }

    /**
     * 队列状态信息类
     */
    public static class QueueStatus {
        public final int queueSize;
        public final int currentCommandByte;
        public final boolean isExecuting;
        public final long totalCommandsSent;
        public final long totalCommandsCompleted;
        public final long totalTimeouts;
        public final long totalErrors;

        public QueueStatus(int queueSize, int currentCommandByte, boolean isExecuting,
                           long totalCommandsSent, long totalCommandsCompleted,
                           long totalTimeouts, long totalErrors) {
            this.queueSize = queueSize;
            this.currentCommandByte = currentCommandByte;
            this.isExecuting = isExecuting;
            this.totalCommandsSent = totalCommandsSent;
            this.totalCommandsCompleted = totalCommandsCompleted;
            this.totalTimeouts = totalTimeouts;
            this.totalErrors = totalErrors;
        }

        @Override
        public String toString() {
            return String.format("QueueStatus{queue=%d, executing=0x%02X, isExecuting=%s, sent=%d, completed=%d, timeouts=%d, errors=%d}",
                    queueSize, currentCommandByte, isExecuting, totalCommandsSent, totalCommandsCompleted, totalTimeouts, totalErrors);
        }
    }
}