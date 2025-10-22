package xyz.junerver.android.lockdemo.debounce;

import android.util.Log;

/**
 * 响应匹配器
 * 用于验证响应是否对应特定指令，并解析响应结果
 */
public class ResponseMatcher {
    private static final String TAG = "ResponseMatcher";

    // 协议常量
    private static final byte[] START_BYTES = {0x57, 0x4B, 0x4C, 0x59}; // 起始符: "W K L Y"
    private static final byte STATUS_SUCCESS = 0x00;          // 执行成功
    private static final byte STATUS_FAILED = (byte) 0xFF;    // 执行失败

    /**
     * 检查响应是否对应特定指令
     *
     * @param response 响应数据
     * @param command  原始指令数据
     * @return 是否匹配
     */
    public static boolean isResponseForCommand(byte[] response, byte[] command) {
        if (response == null || command == null) {
            Log.w(TAG, "响应或指令为null");
            return false;
        }

        if (response.length < 7 || command.length < 7) {
            Log.w(TAG, "响应或指令长度不足");
            return false;
        }

        // 验证响应的基本格式
        if (!isValidResponseFormat(response)) {
            Log.w(TAG, "响应格式无效");
            return false;
        }

        byte responseCmd = response[6];
        byte commandCmd = command[6];
        byte responseAddr = response[5];
        byte commandAddr = command[5];

        boolean isMatch = responseCmd == commandCmd && responseAddr == commandAddr;

        Log.i(TAG, String.format("响应匹配检查: 指令字=0x%02X->0x%02X, 板地址=0x%02X->0x%02X, 匹配=%s",
                commandCmd & 0xFF, responseCmd & 0xFF,
                commandAddr & 0xFF, responseAddr & 0xFF, isMatch));

        return isMatch;
    }

    /**
     * 检查响应是否表示执行成功
     *
     * @param response 响应数据
     * @return 是否成功
     */
    public static boolean isSuccessResponse(byte[] response) {
        if (response == null || response.length < 8) {
            Log.w(TAG, "响应数据长度不足，无法判断执行状态");
            return false;
        }

        byte status = response[7];
        boolean isSuccess = status == STATUS_SUCCESS;

        Log.d(TAG, String.format("响应状态检查: 状态字节=0x%02X, 执行%s",
                status & 0xFF, isSuccess ? "成功" : "失败"));

        return isSuccess;
    }

    /**
     * 验证响应数据的基本格式
     *
     * @param response 响应数据
     * @return 是否有效
     */
    public static boolean isValidResponseFormat(byte[] response) {
        if (response == null || response.length < 7) {
            return false;
        }

        // 检查起始符
        if (response.length < START_BYTES.length ||
                !java.util.Arrays.equals(
                        java.util.Arrays.copyOfRange(response, 0, START_BYTES.length),
                        START_BYTES)) {
            Log.w(TAG, "响应起始符不匹配");
            return false;
        }

        // 检查帧长度
        int frameLength = response[4] & 0xFF;
        if (frameLength != response.length) {
            Log.w(TAG, String.format("响应帧长度不匹配: 期望=%d, 实际=%d", frameLength, response.length));
            return false;
        }

        return true;
    }

    /**
     * 从响应中提取状态信息
     *
     * @param response 响应数据
     * @return 状态描述
     */
    public static String getResponseStatusDescription(byte[] response) {
        if (response == null || response.length < 8) {
            return "响应数据无效";
        }

        byte status = response[7];
        switch (status) {
            case STATUS_SUCCESS:
                return "执行成功";
            case STATUS_FAILED:
                return "执行失败";
            default:
                return String.format("未知状态(0x%02X)", status & 0xFF);
        }
    }

    /**
     * 从响应中提取通道信息（如果有的话）
     *
     * @param response 响应数据
     * @return 通道ID，如果响应中没有通道信息则返回-1
     */
    public static int getChannelId(byte[] response) {
        if (response == null || response.length < 9) {
            return -1;
        }

        byte commandByte = response[6];

        // 根据指令类型判断是否包含通道信息
        switch (commandByte) {
            case (byte) 0x81: // 通道闪烁
            case (byte) 0x82: // 开单个锁
            case (byte) 0x83: // 查询单个门状态
            case (byte) 0x88: // 通道常开
            case (byte) 0x89: // 通道关闭
                return response[8] & 0xFF;

            default:
                return -1;
        }
    }

    /**
     * 从响应中提取锁状态信息（如果有的话）
     *
     * @param response 响应数据
     * @return 锁状态，如果响应中没有锁状态则返回-1
     */
    public static int getLockStatus(byte[] response) {
        if (response == null || response.length < 10) {
            return -1;
        }

        byte commandByte = response[6];

        // 根据指令类型判断是否包含锁状态
        switch (commandByte) {
            case (byte) 0x82: // 开单个锁
            case (byte) 0x83: // 查询单个门状态
                return response[9] & 0xFF;

            default:
                return -1;
        }
    }
}