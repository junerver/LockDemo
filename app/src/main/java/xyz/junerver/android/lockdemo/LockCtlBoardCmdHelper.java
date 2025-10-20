package xyz.junerver.android.lockdemo;

import android.util.Log;

/**
 * 门锁控制板指令构造辅助工具类
 * 提供静态方法用于构造各种门锁控制指令，符合串口通讯协议规范
 */
public class LockCtlBoardCmdHelper {
    private static final String TAG = "LockCtlBoardCmdHelper";

    // 通讯协议常量
    private static final byte[] START_BYTES = {0x57, 0x4B, 0x4C, 0x59}; // 起始符: "W K L Y"
    
    // 指令字常量
    private static final byte CMD_OPEN_MULTIPLE_LOCKS = (byte) 0x80; // 同时开多个锁
    private static final byte CMD_FLASH_CHANNEL = (byte) 0x81;       // 通道闪烁
    private static final byte CMD_OPEN_SINGLE_LOCK = (byte) 0x82;    // 开单个锁
    private static final byte CMD_GET_SINGLE_STATUS = (byte) 0x83;   // 查询单个门状态
    private static final byte CMD_GET_ALL_STATUS = (byte) 0x84;      // 查询所有门状态
    private static final byte CMD_STATUS_UPLOAD = (byte) 0x85;       // 主动上传门状态变化
    private static final byte CMD_OPEN_ALL_LOCKS = (byte) 0x86;      // 开全部锁
    private static final byte CMD_OPEN_MULTIPLE_SEQUENTIAL = (byte) 0x87; // 开多个锁
    private static final byte CMD_CHANNEL_KEEP_OPEN = (byte) 0x88;   // 通道持续打开
    private static final byte CMD_CLOSE_CHANNEL = (byte) 0x89;       // 通道关闭
    
    // 状态字常量
    private static final byte STATUS_SUCCESS = 0x00;          // 执行成功
    private static final byte STATUS_FAILED = (byte) 0xFF;    // 执行失败
    
    // 默认门锁总数
    private static final int DEFAULT_LOCK_COUNT = 24;

    /**
     * 构造串口通讯指令（通用方法）
     * @param boardAddress 板地址 (0-31)
     * @param command 指令字
     * @param data 数据域内容 (可为空)
     * @return 完整的指令字节数组
     */
    public static byte[] buildCommand(byte boardAddress, byte command, byte[] data) {
        // 计算数据长度
        int dataLength = (data != null) ? data.length : 0;
        
        // 帧长度 = 起始符(4) + 帧长度(1) + 板地址(1) + 指令字(1) + 数据域(n) + 校验字节(1)
        int frameLength = 4 + 1 + 1 + 1 + dataLength + 1;
        
        // 构造指令数组
        byte[] commandBytes = new byte[frameLength];
        int index = 0;
        
        // 1. 起始符
        System.arraycopy(START_BYTES, 0, commandBytes, index, START_BYTES.length);
        index += START_BYTES.length;
        
        // 2. 帧长度
        commandBytes[index++] = (byte) frameLength;
        
        // 3. 板地址
        commandBytes[index++] = boardAddress;
        
        // 4. 指令字
        commandBytes[index++] = command;
        
        // 5. 数据域
        if (data != null && data.length > 0) {
            System.arraycopy(data, 0, commandBytes, index, data.length);
            index += data.length;
        }
        
        // 6. 校验字节 (从起始符到数据域最后一个字节的异或值)
        byte checksum = calculateChecksum(commandBytes, 0, index);
        commandBytes[index] = checksum;
        
        Log.d(TAG, "构造指令: " + bytesToHex(commandBytes));
        return commandBytes;
    }

    /**
     * 构造同时开多锁指令
     * @param boardAddress 板地址
     * @param lockIds 门锁ID数组
     * @return 指令字节数组
     */
    public static byte[] buildOpenMultipleLocksCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 每个门锁ID占一个字节
        byte[] data = new byte[lockIds.length + 1];
        data[0] = (byte) lockIds.length;
        for (int i = 0; i < lockIds.length; i++) {
            data[i + 1] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_LOCKS, data);
    }

    /**
     * 构造通道闪烁指令
     * @param boardAddress 板地址
     * @param lockId 门锁ID
     * @return 指令字节数组
     */
    public static byte[] buildFlashChannelCommand(byte boardAddress, int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return null;
        }

        // 根据示例，数据域为1字节: 通道号(1字节)
        byte[] data = new byte[1];
        data[0] = (byte) lockId;

        return buildCommand(boardAddress, CMD_FLASH_CHANNEL, data);
    }

    /**
     * 构造开单锁指令
     * @param boardAddress 板地址
     * @param lockId 门锁ID
     * @return 指令字节数组
     */
    public static byte[] buildOpenSingleLockCommand(byte boardAddress, int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return null;
        }

        // 数据域: 通道号(1字节)
        byte[] data = {(byte) lockId};
        return buildCommand(boardAddress, CMD_OPEN_SINGLE_LOCK, data);
    }

    /**
     * 构造查询单个门锁状态指令
     * @param boardAddress 板地址
     * @param lockId 门锁ID
     * @return 指令字节数组
     */
    public static byte[] buildGetSingleLockStatusCommand(byte boardAddress, int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return null;
        }

        // 数据域: 通道号(1字节)
        byte[] data = {(byte) lockId};
        return buildCommand(boardAddress, CMD_GET_SINGLE_STATUS, data);
    }

    /**
     * 构造查询所有门锁状态指令
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public static byte[] buildGetAllLocksStatusCommand(byte boardAddress) {
        // 数据域为空
        return buildCommand(boardAddress, CMD_GET_ALL_STATUS, null);
    }

    /**
     * 构造开全部锁指令
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public static byte[] buildOpenAllLocksCommand(byte boardAddress) {
        // 数据域为空
        return buildCommand(boardAddress, CMD_OPEN_ALL_LOCKS, null);
    }

    /**
     * 构造开多个锁指令 (逐一打开)
     * @param boardAddress 板地址
     * @param lockIds 门锁ID数组
     * @return 指令字节数组
     */
    public static byte[] buildOpenMultipleSequentialCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 每个门锁ID占一个字节
        byte[] data = new byte[lockIds.length + 1];
        data[0] = (byte) lockIds.length;
        for (int i = 0; i < lockIds.length; i++) {
            data[i + 1] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_SEQUENTIAL, data);
    }

    /**
     * 验证响应数据的完整性
     * @param response 响应数据
     * @return 验证是否通过
     */
    public static boolean validateResponse(byte[] response) {
        if (response == null || response.length < 6) {
            Log.e(TAG, "响应数据长度不足");
            return false;
        }

        // 检查起始符
        if (response.length < START_BYTES.length ||
                !java.util.Arrays.equals(java.util.Arrays.copyOfRange(response, 0, START_BYTES.length), START_BYTES)) {
            Log.e(TAG, "起始符不匹配");
            return false;
        }

        // 检查帧长度
        int frameLength = response[4] & 0xFF;
        if (frameLength != response.length) {
            Log.e(TAG, "帧长度不匹配: 期望 " + frameLength + ", 实际 " + response.length);
            return false;
        }

        // 验证校验字节
        byte expectedChecksum = calculateChecksum(response, 0, response.length - 1);
        byte actualChecksum = response[response.length - 1];

        if (expectedChecksum != actualChecksum) {
            Log.e(TAG, "校验字节不匹配: 期望 " + String.format("0x%02X", expectedChecksum) +
                    ", 实际 " + String.format("0x%02X", actualChecksum));
            return false;
        }

        Log.d(TAG, "响应数据验证通过: " + bytesToHex(response));
        return true;
    }

    /**
     * 解析响应数据中的状态字节
     * @param response 响应数据
     * @return 状态字节 (0x00=成功, 0xFF=失败, 其他=未知)
     */
    public static byte parseResponseStatus(byte[] response) {
        if (!validateResponse(response)) {
            return (byte) 0xFF; // 返回失败状态
        }

        // 状态字节在数据域的第一个字节位置(起始符4+帧长度1+板地址1+指令字1=7)
        if (response.length > 7) {
            byte status = response[7];
            Log.d(TAG, "响应状态: " + String.format("0x%02X", status) +
                    (status == STATUS_SUCCESS ? " (成功)" :
                            status == STATUS_FAILED ? " (失败)" : " (未知)"));
            return status;
        }

        Log.e(TAG, "响应数据中未找到状态字节");
        return (byte) 0xFF;
    }

    /**
     * 计算校验字节 (XOR)
     * @param data 字节数组
     * @param start 起始位置
     * @param end 结束位置 (不包含)
     * @return XOR校验值
     */
    private static byte calculateChecksum(byte[] data, int start, int end) {
        byte checksum = 0;
        for (int i = start; i < end; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }

    /**
     * 字节数组转十六进制字符串 (用于日志输出)
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";

        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X ", b);
            hexString.append(hex);
        }
        return hexString.toString().trim();
    }
}
