package xyz.junerver.android.lockdemo;

import android.util.Log;

/**
 * 门锁控制板工具类
 * 懒加载单例模式，用于控制留样柜门锁的开关、状态查询等操作
 */
public class LockCtlBoardUtil {
    private static final String TAG = "LockCtlBoardUtil";
    private static volatile LockCtlBoardUtil instance;

    // 默认门锁总数
    private static final int DEFAULT_LOCK_COUNT = 12;

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

    // 默认板地址
    private static final byte DEFAULT_BOARD_ADDRESS = 0x00;

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

    /**
     * 1. 同时开多锁
     *
     * @param lockIds 门锁ID数组（可变长参数）
     * @return 操作是否成功
     */
    public boolean openMultipleLocksSimultaneously(int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return false;
        }

        Log.d(TAG, "同时开启门锁: " + java.util.Arrays.toString(lockIds));
        // TODO: 实现同时开多锁的具体逻辑
        return true;
    }

    /**
     * 2. 锁通道LED闪烁
     *
     * @param lockId   门锁ID
     * @param duration 闪烁持续时间（毫秒）
     * @return 操作是否成功
     */
    public boolean flashLockLed(int lockId, long duration) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return false;
        }

        Log.d(TAG, "门锁 " + lockId + " LED闪烁，持续时间: " + duration + "ms");
        // TODO: 实现LED闪烁的具体逻辑
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

        Log.d(TAG, "开启门锁: " + lockId);
        // TODO: 实现单独开锁的具体逻辑
        return true;
    }

    /**
     * 4. 查询单个门锁状态
     *
     * @param lockId 门锁ID
     * @return 门锁状态（0-关闭，1-打开，-1-错误）
     */
    public int getSingleLockStatus(int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return -1;
        }

        Log.d(TAG, "查询门锁状态: " + lockId);
        // TODO: 实现查询单个门锁状态的具体逻辑
        return 0; // 默认返回关闭状态
    }

    /**
     * 5. 查询所有门锁状态
     *
     * @return 所有门锁状态数组
     */
    public int[] getAllLocksStatus() {
        Log.d(TAG, "查询所有门锁状态");
        int[] statusArray = new int[DEFAULT_LOCK_COUNT];

        // TODO: 实现查询所有门锁状态的具体逻辑
        // 暂时返回全部关闭状态
        for (int i = 0; i < DEFAULT_LOCK_COUNT; i++) {
            statusArray[i] = 0;
        }

        return statusArray;
    }

    /**
     * 6. 全部开锁（逐一打开）
     *
     * @return 操作是否成功
     */
    public boolean openAllLocksSequentially() {
        Log.d(TAG, "开始逐一开启所有门锁");

        for (int i = 0; i < DEFAULT_LOCK_COUNT; i++) {
            if (!openSingleLock(i)) {
                Log.e(TAG, "开启门锁 " + i + " 失败");
                return false;
            }

            // 门锁间添加延时，避免硬件冲突
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "门锁开启延时被中断");
                return false;
            }
        }

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

        Log.d(TAG, "开始逐一开启门锁: " + java.util.Arrays.toString(lockIds));

        for (int lockId : lockIds) {
            if (!openSingleLock(lockId)) {
                Log.e(TAG, "开启门锁 " + lockId + " 失败");
                return false;
            }

            // 门锁间添加延时，避免硬件冲突
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "门锁开启延时被中断");
                return false;
            }
        }

        Log.d(TAG, "指定门锁开启完成");
        return true;
    }

    /**
     * 构造串口通讯指令
     *
     * @param boardAddress 板地址 (0-31)
     * @param command      指令字
     * @param data         数据域内容 (可为空)
     * @return 完整的指令字节数组
     */
    public byte[] buildCommand(byte boardAddress, byte command, byte[] data) {
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
     * 计算校验字节 (XOR)
     *
     * @param data  字节数组
     * @param start 起始位置
     * @param end   结束位置 (不包含)
     * @return XOR校验值
     */
    private byte calculateChecksum(byte[] data, int start, int end) {
        byte checksum = 0;
        for (int i = start; i < end; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }

    /**
     * 字节数组转十六进制字符串 (用于日志输出)
     *
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

    /**
     * 构造同时开多锁指令
     *
     * @param boardAddress 板地址
     * @param lockIds      门锁ID数组
     * @return 指令字节数组
     */
    public byte[] buildOpenMultipleLocksCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 每个门锁ID占一个字节
        byte[] data = new byte[lockIds.length];
        for (int i = 0; i < lockIds.length; i++) {
            data[i] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_LOCKS, data);
    }

    /**
     * 构造通道闪烁指令
     *
     * @param boardAddress 板地址
     * @param lockId       门锁ID
     * @return 指令字节数组
     */
    public byte[] buildFlashChannelCommand(byte boardAddress, int lockId) {
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
     *
     * @param boardAddress 板地址
     * @param lockId       门锁ID
     * @return 指令字节数组
     */
    public byte[] buildOpenSingleLockCommand(byte boardAddress, int lockId) {
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
     *
     * @param boardAddress 板地址
     * @param lockId       门锁ID
     * @return 指令字节数组
     */
    public byte[] buildGetSingleLockStatusCommand(byte boardAddress, int lockId) {
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
     *
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public byte[] buildGetAllLocksStatusCommand(byte boardAddress) {
        // 数据域为空
        return buildCommand(boardAddress, CMD_GET_ALL_STATUS, null);
    }

    /**
     * 构造开全部锁指令
     *
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public byte[] buildOpenAllLocksCommand(byte boardAddress) {
        // 数据域为空
        return buildCommand(boardAddress, CMD_OPEN_ALL_LOCKS, null);
    }

    /**
     * 构造开多个锁指令 (逐一打开)
     *
     * @param boardAddress 板地址
     * @param lockIds      门锁ID数组
     * @return 指令字节数组
     */
    public byte[] buildOpenMultipleSequentialCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 每个门锁ID占一个字节
        byte[] data = new byte[lockIds.length];
        for (int i = 0; i < lockIds.length; i++) {
            data[i] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_SEQUENTIAL, data);
    }

    /**
     * 验证响应数据的完整性
     *
     * @param response 响应数据
     * @return 验证是否通过
     */
    public boolean validateResponse(byte[] response) {
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
     *
     * @param response 响应数据
     * @return 状态字节 (0x00=成功, 0xFF=失败, 其他=未知)
     */
    public byte parseResponseStatus(byte[] response) {
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
     * 测试指令构造功能的示例方法
     * 该方法演示了如何构造各种门锁控制指令
     */
    public void testCommandBuilding() {
        Log.d(TAG, "=== 开始测试指令构造功能 ===");

        byte boardAddress = 0x00; // 0号板

        // 测试1: 1号通道闪烁，持续1000毫秒
        // 对应示例: 57 4B 4C 59 09 00 81 01 80
        byte[] flashCommand = buildFlashChannelCommand(boardAddress, 1);
        Log.d(TAG, "通道闪烁指令: " + bytesToHex(flashCommand));

        // 测试2: 同时开启多个锁(1,2,3)
        byte[] openMultipleCommand = buildOpenMultipleLocksCommand(boardAddress, 1, 2, 3);
        Log.d(TAG, "同时开多锁指令: " + bytesToHex(openMultipleCommand));

        // 测试3: 开单个锁(5号锁)
        byte[] openSingleCommand = buildOpenSingleLockCommand(boardAddress, 5);
        Log.d(TAG, "开单锁指令: " + bytesToHex(openSingleCommand));

        // 测试4: 查询单个门锁状态(2号锁)
        byte[] getSingleStatusCommand = buildGetSingleLockStatusCommand(boardAddress, 2);
        Log.d(TAG, "查询单锁状态指令: " + bytesToHex(getSingleStatusCommand));

        // 测试5: 查询所有门锁状态
        byte[] getAllStatusCommand = buildGetAllLocksStatusCommand(boardAddress);
        Log.d(TAG, "查询所有锁状态指令: " + bytesToHex(getAllStatusCommand));

        // 测试6: 开全部锁
        byte[] openAllCommand = buildOpenAllLocksCommand(boardAddress);
        Log.d(TAG, "开全部锁指令: " + bytesToHex(openAllCommand));

        // 测试7: 逐一开多个锁(3,5,7)
        byte[] openSequentialCommand = buildOpenMultipleSequentialCommand(boardAddress, 3, 5, 7);
        Log.d(TAG, "逐一开多锁指令: " + bytesToHex(openSequentialCommand));

        // 测试响应验证功能
        testResponseValidation();

        Log.d(TAG, "=== 指令构造功能测试完成 ===");
    }

    /**
     * 测试响应数据解析功能
     */
    private void testResponseValidation() {
        Log.d(TAG, "=== 测试响应数据解析 ===");

        // 模拟一个成功的响应: 57 4B 4C 59 0A 00 81 00 01 83
        // 对应1号通道闪烁的成功响应
        byte[] successResponse = {0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, (byte) 0x81, 0x00, 0x01, (byte) 0x83};

        boolean isValid = validateResponse(successResponse);
        Log.d(TAG, "响应数据验证结果: " + (isValid ? "通过" : "失败"));

        if (isValid) {
            byte status = parseResponseStatus(successResponse);
            Log.d(TAG, "解析到的状态: " + (status == STATUS_SUCCESS ? "成功" : "失败"));
        }

        // 测试一个错误的响应数据
        byte[] invalidResponse = {0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, (byte) 0x81, 0x00, 0x01, 0x00}; // 错误的校验字节
        boolean isInvalid = validateResponse(invalidResponse);
        Log.d(TAG, "错误响应数据验证结果: " + (isInvalid ? "通过(意外)" : "失败(预期)"));
    }
}