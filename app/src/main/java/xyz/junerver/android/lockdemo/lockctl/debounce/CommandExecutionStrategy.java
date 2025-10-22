package xyz.junerver.android.lockdemo.lockctl.debounce;

import android.util.Log;

/**
 * 指令执行策略类
 * 定义不同指令的执行时间和超时计算规则
 */
public class CommandExecutionStrategy {
    private static final String TAG = "CommandExecutionStrategy";

    // 基础执行时间（单位：毫秒）
    private static final int SINGLE_LOCK_EXECUTION_TIME = 350;    // 开单个锁
    private static final int QUERY_SINGLE_EXECUTION_TIME = 100;   // 查询单个门状态
    private static final int QUERY_ALL_EXECUTION_TIME = 200;      // 查询所有门状态
    private static final int CHANNEL_CONTROL_EXECUTION_TIME = 100; // 通道控制（闪烁、常开、关闭）
    private static final int SIMULTANEOUS_LOCKS_EXECUTION_TIME = 350; // 同时开多个锁

    // 安全系数，用于计算超时时间
    private static final int TIMEOUT_SAFETY_FACTOR = 2;

    /**
     * 计算指令的超时时间
     *
     * @param commandData 指令数据
     * @return 超时时间（毫秒）
     */
    public static long calculateTimeout(byte[] commandData) {
        if (commandData == null || commandData.length < 7) {
            Log.w(TAG, "指令数据格式错误，使用默认超时时间");
            System.out.println("[DEBUG] 指令数据格式错误，使用默认超时时间");
            return 1000; // 默认1秒超时
        }

        byte commandByte = commandData[6];
        int paramCount = getParamCount(commandData);
        int baseTime = getBaseExecutionTime(commandByte, paramCount);

        long timeout = baseTime * TIMEOUT_SAFETY_FACTOR;

        String debugMsg = String.format("[DEBUG] calculateTimeout: 指令字=0x%02X, 参数数量=%d, 基础时间=%dms, 超时时间=%dms",
                commandByte & 0xFF, paramCount, baseTime, timeout);
        System.out.println(debugMsg);
        Log.d(TAG, debugMsg);

        return timeout;
    }

    /**
     * 获取指令的基础执行时间
     *
     * @param commandByte 指令字
     * @param paramCount  参数数量
     * @return 基础执行时间（毫秒）
     */
    private static int getBaseExecutionTime(byte commandByte, int paramCount) {
        switch (commandByte) {
            case (byte) 0x80: // 同时开多锁
                return SIMULTANEOUS_LOCKS_EXECUTION_TIME;

            case (byte) 0x82: // 开单个锁
                return SINGLE_LOCK_EXECUTION_TIME;

            case (byte) 0x83: // 查询单个门状态
                return QUERY_SINGLE_EXECUTION_TIME;

            case (byte) 0x84: // 查询所有门状态
                return QUERY_ALL_EXECUTION_TIME;

            case (byte) 0x86: // 开全部锁（效果类似逐一开锁）
            case (byte) 0x87: // 逐一开多锁 (实际使用)
                // 动态计算：开锁数量 * 350ms
                int lockCount = Math.max(1, paramCount);
                return SINGLE_LOCK_EXECUTION_TIME * lockCount;

            case (byte) 0x81: // 通道闪烁
            case (byte) 0x88: // 通道常开
            case (byte) 0x89: // 通道关闭
                return CHANNEL_CONTROL_EXECUTION_TIME;

            default:
                Log.w(TAG, "未知指令字: 0x" + String.format("%02X", commandByte & 0xFF) + "，使用默认执行时间");
                return 500; // 默认500ms
        }
    }

    /**
     * 从指令数据中提取参数数量
     *
     * @param commandData 指令数据
     * @return 参数数量
     */
    private static int getParamCount(byte[] commandData) {
        // 根据指令类型提取参数数量
        if (commandData.length < 8) {
            return 1; // 默认参数数量
        }

        byte commandByte = commandData[6];

        switch (commandByte) {
            case (byte) 0x80: // 同时开多锁
            case (byte) 0x86: // 开全部锁（效果类似逐一开锁）
            case (byte) 0x87: // 逐一开多锁 (实际使用)
                // 数据域第一个字节是锁数量
                return commandData[7] & 0xFF;

            case (byte) 0x82: // 开单个锁
            case (byte) 0x83: // 查询单个门状态
            case (byte) 0x81: // 通道闪烁
            case (byte) 0x88: // 通道常开
            case (byte) 0x89: // 通道关闭
                // 这些指令只有一个参数（通道ID）
                return 1;

            case (byte) 0x84: // 查询所有门状态
            case (byte) 0x85: // 开全部锁
                // 这些指令没有参数
                return 0;

            default:
                return 1;
        }
    }

    /**
     * 获取指令的描述信息
     *
     * @param commandByte 指令字
     * @return 指令描述
     */
    public static String getCommandDescription(byte commandByte) {
        switch (commandByte) {
            case (byte) 0x80:
                return "同时开多锁";
            case (byte) 0x81:
                return "通道闪烁";
            case (byte) 0x82:
                return "开单个锁";
            case (byte) 0x83:
                return "查询单个门状态";
            case (byte) 0x84:
                return "查询所有门状态";
            case (byte) 0x85:
                return "锁控板主动上报数据";
            case (byte) 0x86:
                return "开全部锁";
            case (byte) 0x87:
                return "逐一开多锁";
            case (byte) 0x88:
                return "通道常开";
            case (byte) 0x89:
                return "通道关闭";
            default:
                return "未知指令(0x" + String.format("%02X", commandByte & 0xFF) + ")";
        }
    }
}