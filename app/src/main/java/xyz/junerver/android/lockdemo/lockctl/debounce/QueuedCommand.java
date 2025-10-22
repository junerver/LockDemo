package xyz.junerver.android.lockdemo.lockctl.debounce;

import android.util.Log;

/**
 * 队列中的指令项
 * 封装指令数据、执行状态和监听器
 */
public class QueuedCommand {
    private static final String TAG = "QueuedCommand";

    private final byte[] commandData;
    private final byte commandByte;  // 指令字，用于匹配响应
    private final byte boardAddress; // 板地址，用于匹配响应
    private final long timeout;      // 超时时间（毫秒）
    private final OnCommandListener listener;
    private final long timestamp;    // 创建时间戳
    private volatile boolean completed = false; // 是否已完成
    private volatile boolean timedOut = false;  // 是否已超时

    /**
     * 构造函数
     *
     * @param commandData 指令数据
     * @param listener    指令执行监听器
     */
    public QueuedCommand(byte[] commandData, OnCommandListener listener) {
        if (commandData == null || commandData.length < 7) {
            throw new IllegalArgumentException("指令数据格式错误");
        }

        this.commandData = commandData.clone(); // 防止外部修改
        this.commandByte = commandData[6];
        this.boardAddress = commandData[5];
        this.timeout = CommandExecutionStrategy.calculateTimeout(commandData);
        this.listener = listener;
        this.timestamp = System.currentTimeMillis();

        Log.d(TAG, String.format("创建队列指令: 指令字=0x%02X, 板地址=0x%02X, 超时=%dms",
                commandByte & 0xFF, boardAddress & 0xFF, timeout));
    }

    /**
     * 获取指令数据
     *
     * @return 指令数据的副本
     */
    public byte[] getCommandData() {
        return commandData.clone();
    }

    /**
     * 获取指令字
     *
     * @return 指令字
     */
    public byte getCommandByte() {
        return commandByte;
    }

    /**
     * 获取板地址
     *
     * @return 板地址
     */
    public byte getBoardAddress() {
        return boardAddress;
    }

    /**
     * 获取超时时间
     *
     * @return 超时时间（毫秒）
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * 获取监听器
     *
     * @return 监听器
     */
    public OnCommandListener getListener() {
        return listener;
    }

    /**
     * 获取创建时间戳
     *
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 检查是否已完成
     *
     * @return 是否已完成
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * 标记为已完成
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
        Log.d(TAG, String.format("指令完成状态更新: 指令字=0x%02X, 完成=%s",
                commandByte & 0xFF, completed));
    }

    /**
     * 检查是否已超时
     *
     * @return 是否已超时
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * 标记为已超时
     */
    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
        if (timedOut) {
            Log.w(TAG, String.format("指令超时: 指令字=0x%02X", commandByte & 0xFF));
        }
    }

    /**
     * 检查是否已经超时（基于时间计算）
     *
     * @return 是否已超时
     */
    public boolean isExpired() {
        long elapsedTime = System.currentTimeMillis() - timestamp;
        return elapsedTime > timeout;
    }

    /**
     * 获取剩余超时时间
     *
     * @return 剩余超时时间（毫秒），如果已超时则返回0
     */
    public long getRemainingTimeout() {
        long elapsedTime = System.currentTimeMillis() - timestamp;
        long remaining = timeout - elapsedTime;
        return Math.max(0, remaining);
    }

    /**
     * 获取指令描述
     *
     * @return 指令描述
     */
    public String getDescription() {
        return CommandExecutionStrategy.getCommandDescription(commandByte);
    }

    @Override
    public String toString() {
        return String.format("QueuedCommand{cmd=0x%02X, addr=0x%02X, timeout=%dms, completed=%s, timedOut=%s, desc='%s'}",
                commandByte & 0xFF, boardAddress & 0xFF, timeout, completed, timedOut, getDescription());
    }
}