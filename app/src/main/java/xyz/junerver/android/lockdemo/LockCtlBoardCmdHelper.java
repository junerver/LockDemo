package xyz.junerver.android.lockdemo;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.AllLocksStatusResponse;
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.BaseResponse;
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.ChannelResponse;
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.ChannelStatus;
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.LockStatusResponse;
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.StatusUploadResponse;

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

    // 锁状态常量
    private static final byte LOCK_STATUS_OPEN = 0x00;        // 门打开
    private static final byte LOCK_STATUS_CLOSED = 0x01;      // 门关闭
    private static final byte LOCK_STATUS_ERROR = (byte) 0xFF; // 执行失败

    // 默认门锁总数
    private static final int DEFAULT_CHANNEL_COUNT = 24;

    // Gson实例用于JSON解析
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * 构造串口通讯指令（通用方法）
     *
     * @param boardAddress 板地址 (0-31)
     * @param command      指令字
     * @param data         数据域内容 (可为空)
     * @return 完整的指令字节数组
     */
    public static byte[] buildCommand(byte boardAddress, byte command, byte[] data) {
        // 计算数据长度
        int dataLength = (data != null) ? data.length : 0;

        // 帧长度 = 起始符(4) + 帧长度(1) + 板地址(1) + 指令字(1) + 数据域(n) + 校验字节(1)
        int frameLength = 4 + 1 + 1 + 1 + dataLength + 1;

        byte[] commandBytes = new byte[frameLength];

        // 1. 起始符
        System.arraycopy(START_BYTES, 0, commandBytes, 0, START_BYTES.length);

        // 2. 帧长度
        commandBytes[4] = (byte) (frameLength & 0xFF);

        // 3. 板地址
        commandBytes[5] = boardAddress;

        // 4. 指令字
        commandBytes[6] = command;

        // 5. 数据域
        if (data != null && data.length > 0) {
            System.arraycopy(data, 0, commandBytes, 7, data.length);
        }

        // 6. 校验字节 (对整个帧进行XOR校验)
        byte checksum = calculateChecksum(commandBytes, 0, frameLength - 1);
        commandBytes[frameLength - 1] = checksum;


        return commandBytes;
    }

    /**
     * 1. 同时开多锁指令
     *
     * @param boardAddress 板地址
     * @param lockIds      门锁ID数组
     * @return 指令字节数组
     */
    public static byte[] buildOpenMultipleLocksCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 锁数量 + 锁ID列表
        byte[] data = new byte[1 + lockIds.length];
        data[0] = (byte) lockIds.length;
        for (int i = 0; i < lockIds.length; i++) {
            data[i + 1] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_LOCKS, data);
    }

    /**
     * 2. 通道闪烁指令
     *
     * @param boardAddress 板地址
     * @param channelId    通道ID
     * @return 指令字节数组
     */
    public static byte[] buildFlashChannelCommand(byte boardAddress, int channelId) {
        if (channelId < 0 || channelId > DEFAULT_CHANNEL_COUNT) {
            Log.e(TAG, "通道ID范围错误");
            return null;
        }
        byte[] data = {(byte) channelId};
        return buildCommand(boardAddress, CMD_FLASH_CHANNEL, data);
    }

    /**
     * 3. 开单个锁指令
     *
     * @param boardAddress 板地址
     * @param channelId    通道ID
     * @return 指令字节数组
     */
    public static byte[] buildOpenSingleLockCommand(byte boardAddress, int channelId) {
        if (channelId < 0 || channelId > DEFAULT_CHANNEL_COUNT) {
            Log.e(TAG, "通道ID范围错误");
            return null;
        }
        byte[] data = {(byte) channelId};
        return buildCommand(boardAddress, CMD_OPEN_SINGLE_LOCK, data);
    }

    /**
     * 4. 查询单个门锁状态指令
     *
     * @param boardAddress 板地址
     * @param channelId    通道ID
     * @return 指令字节数组
     */
    public static byte[] buildGetSingleLockStatusCommand(byte boardAddress, int channelId) {
        if (channelId < 0 || channelId > DEFAULT_CHANNEL_COUNT) {
            Log.e(TAG, "通道ID范围错误");
            return null;
        }
        byte[] data = {(byte) channelId};
        return buildCommand(boardAddress, CMD_GET_SINGLE_STATUS, data);
    }

    /**
     * 5. 查询所有门锁状态指令
     *
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public static byte[] buildGetAllLocksStatusCommand(byte boardAddress) {
        return buildCommand(boardAddress, CMD_GET_ALL_STATUS, null);
    }

    /**
     * 6. 开全部锁指令
     *
     * @param boardAddress 板地址
     * @return 指令字节数组
     */
    public static byte[] buildOpenAllLocksCommand(byte boardAddress) {
        return buildCommand(boardAddress, CMD_OPEN_ALL_LOCKS, null);
    }

    /**
     * 7. 逐一开多锁指令
     *
     * @param boardAddress 板地址
     * @param lockIds      门锁ID数组
     * @return 指令字节数组
     */
    public static byte[] buildOpenMultipleSequentialCommand(byte boardAddress, int... lockIds) {
        if (lockIds == null || lockIds.length == 0) {
            Log.e(TAG, "门锁ID不能为空");
            return null;
        }

        // 数据域: 锁数量 + 锁ID列表
        byte[] data = new byte[1 + lockIds.length];
        data[0] = (byte) lockIds.length;
        for (int i = 0; i < lockIds.length; i++) {
            data[i + 1] = (byte) lockIds[i];
        }

        return buildCommand(boardAddress, CMD_OPEN_MULTIPLE_SEQUENTIAL, data);
    }

    /**
     * 8. 通道持续打开指令 (0x88)
     *
     * @param boardAddress 板地址
     * @param channelId    通道ID
     * @return 指令字节数组
     */
    public static byte[] buildChannelKeepOpenCommand(byte boardAddress, int channelId) {
        byte[] data = {(byte) channelId};
        return buildCommand(boardAddress, CMD_CHANNEL_KEEP_OPEN, data);
    }

    /**
     * 9. 停止通道持续打开指令 (0x89)
     *
     * @param boardAddress 板地址
     * @param channelId    通道ID
     * @return 指令字节数组
     */
    public static byte[] buildCloseChannelCommand(byte boardAddress, int channelId) {
        byte[] data = {(byte) channelId};
        return buildCommand(boardAddress, CMD_CLOSE_CHANNEL, data);
    }

    // ==================== JSON响应解析方法 ====================

    /**
     * 解析响应为JSON格式（统一入口）
     *
     * @param response 响应数据
     * @return JSON格式的字符串
     */
    public static String parseResponseToJson(byte[] response) {
        if (!validateResponse(response)) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据格式错误"));
        }

        try {
            byte command = response[6];

            switch (command) {
                case CMD_OPEN_MULTIPLE_LOCKS:
                    return parseOpenMultipleLocksJsonResponse(response);
                case CMD_FLASH_CHANNEL:
                    return parseFlashChannelJsonResponse(response);
                case CMD_OPEN_SINGLE_LOCK:
                    return parseOpenSingleLockJsonResponse(response);
                case CMD_GET_SINGLE_STATUS:
                    return parseGetSingleLockStatusJsonResponse(response);
                case CMD_GET_ALL_STATUS:
                    return parseGetAllLocksStatusJsonResponse(response);
                case CMD_STATUS_UPLOAD:
                    return parseStatusUploadJsonResponse(response);
                case CMD_OPEN_ALL_LOCKS:
                    return parseOpenAllLocksJsonResponse(response);
                case CMD_OPEN_MULTIPLE_SEQUENTIAL:
                    return parseOpenMultipleSequentialJsonResponse(response);
                case CMD_CHANNEL_KEEP_OPEN:
                    return parseChannelKeepOpenJsonResponse(response);
                case CMD_CLOSE_CHANNEL:
                    return parseCloseChannelJsonResponse(response);
                default:
                    return gson.toJson(new BaseResponse("unknown_command", -1, "未知指令字: 0x" +
                            String.format("%02X", command & 0xFF)));
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON解析响应失败", e);
            return gson.toJson(new BaseResponse("parse_error", -1, "解析失败: " + e.getMessage()));
        }
    }

    /**
     * 验证响应数据的完整性
     *
     * @param response 响应数据
     * @return 验证是否通过
     */
    public static boolean validateResponse(byte[] response) {
        if (response == null || response.length < 6) {
            return false;
        }

        // 检查起始符
        if (response.length < START_BYTES.length ||
                !Arrays.equals(Arrays.copyOfRange(response, 0, START_BYTES.length), START_BYTES)) {
            return false;
        }

        // 检查帧长度
        int frameLength = response[4] & 0xFF;
        if (frameLength != response.length) {
            return false;
        }

        // 验证校验字节
        byte expectedChecksum = calculateChecksum(response, 0, response.length - 1);
        byte actualChecksum = response[response.length - 1];

        return expectedChecksum == actualChecksum;
    }

    /**
     * 解析同时开多锁响应为JSON (0x80)
     */
    private static String parseOpenMultipleLocksJsonResponse(byte[] response) {
        if (response.length < 8) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        String message = String.format("同时开多锁操作%s",
                status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new BaseResponse("open_multiple_locks", status & 0xFF, message));
    }

    /**
     * 解析通道闪烁响应为JSON (0x81)
     */
    private static String parseFlashChannelJsonResponse(byte[] response) {
        if (response.length < 9) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channel = response[8];
        String message = String.format("通道%d闪烁操作%s",
                channel & 0xFF, status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new ChannelResponse("flash_channel", status & 0xFF, channel & 0xFF, message));
    }

    /**
     * 解析开单锁响应为JSON (0x82)
     */
    private static String parseOpenSingleLockJsonResponse(byte[] response) {
        if (response.length < 10) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channel = response[8];
        byte lockStatus = response[9];

        String message = String.format("通道%d开锁%s，锁状态：%s",
                channel & 0xFF,
                status == STATUS_SUCCESS ? "成功" : "失败",
                lockStatus == LOCK_STATUS_OPEN ? "打开" :
                        lockStatus == LOCK_STATUS_CLOSED ? "关闭" : "错误");

        return gson.toJson(new LockStatusResponse("open_single_lock", status & 0xFF,
                channel & 0xFF, lockStatus & 0xFF, message));
    }

    /**
     * 解析查询单个门锁状态响应为JSON (0x83)
     */
    private static String parseGetSingleLockStatusJsonResponse(byte[] response) {
        if (response.length < 10) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channel = response[8];
        byte lockStatus = response[9];

        String message = String.format("查询通道%d状态%s，锁状态：%s",
                channel & 0xFF,
                status == STATUS_SUCCESS ? "成功" : "失败",
                lockStatus == LOCK_STATUS_OPEN ? "打开" :
                        lockStatus == LOCK_STATUS_CLOSED ? "关闭" : "错误");

        return gson.toJson(new LockStatusResponse("get_single_lock_status", status & 0xFF,
                channel & 0xFF, lockStatus & 0xFF, message));
    }

    /**
     * 解析查询所有门锁状态响应为JSON (0x84)
     */
    private static String parseGetAllLocksStatusJsonResponse(byte[] response) {
        if (response.length < 9) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channelCount = response[8];

        List<ChannelStatus> channelStatusList = new ArrayList<>();

        // 解析每个通道的状态
        for (int i = 0; i < channelCount && (9 + i) < response.length; i++) {
            byte lockStatus = response[9 + i];
            channelStatusList.add(new ChannelStatus(i + 1, lockStatus & 0xFF));
        }

        return gson.toJson(new AllLocksStatusResponse(status & 0xFF, channelCount & 0xFF, channelStatusList));
    }

    /**
     * 解析状态上传响应为JSON (0x85)
     */
    private static String parseStatusUploadJsonResponse(byte[] response) {
        if (response.length < 9) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte channel = response[7];
        byte lockStatus = response[8];

        return gson.toJson(new StatusUploadResponse(channel & 0xFF, lockStatus & 0xFF));
    }

    /**
     * 解析开全部锁响应为JSON (0x86)
     */
    private static String parseOpenAllLocksJsonResponse(byte[] response) {
        if (response.length < 8) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        String message = String.format("开全部锁操作%s",
                status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new BaseResponse("open_all_locks", status & 0xFF, message));
    }

    /**
     * 解析逐一开多锁响应为JSON (0x87)
     */
    private static String parseOpenMultipleSequentialJsonResponse(byte[] response) {
        if (response.length < 8) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        String message = String.format("逐一开多锁操作%s",
                status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new BaseResponse("open_multiple_sequential", status & 0xFF, message));
    }

    /**
     * 解析通道持续打开响应为JSON (0x88)
     */
    private static String parseChannelKeepOpenJsonResponse(byte[] response) {
        if (response.length < 9) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channel = response[8];
        String message = String.format("通道%d持续打开操作%s",
                channel & 0xFF, status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new ChannelResponse("channel_keep_open", status & 0xFF, channel & 0xFF, message));
    }

    /**
     * 解析通道关闭响应为JSON (0x89)
     */
    private static String parseCloseChannelJsonResponse(byte[] response) {
        if (response.length < 9) {
            return gson.toJson(new BaseResponse("error", -1, "响应数据长度不足"));
        }
        byte status = response[7];
        byte channel = response[8];
        String message = String.format("通道%d关闭操作%s",
                channel & 0xFF, status == STATUS_SUCCESS ? "成功" : "失败");
        return gson.toJson(new ChannelResponse("close_channel", status & 0xFF, channel & 0xFF, message));
    }

    /**
     * 计算校验字节 (XOR校验)
     *
     * @param data   数据
     * @param offset 起始位置
     * @param length 结束位置(不包含)
     * @return 校验字节
     */
    private static byte calculateChecksum(byte[] data, int offset, int length) {
        byte checksum = 0;
        for (int i = offset; i < length; i++) {
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
        if (bytes == null) {
            return "null";
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X ", b);
            hexString.append(hex);
        }
        return hexString.toString().trim();
    }
}