package xyz.junerver.android.lockdemo.lockctl;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 门锁控制板响应数据模型
 * 使用统一的JSON格式解析不同类型的响应数据
 */
public class LockCtlBoardResponseModels {

    /**
     * 基础响应模型
     */
    public static class BaseResponse {
        @SerializedName("commandType")
        private String commandType;

        @SerializedName("status")
        private int status;

        @SerializedName("message")
        private String message;

        public BaseResponse() {}

        public BaseResponse(String commandType, int status, String message) {
            this.commandType = commandType;
            this.status = status;
            this.message = message;
        }

        // Getters and setters
        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 状态+通道响应模型 (81, 88, 89)
     */
    public static class ChannelResponse extends BaseResponse {
        @SerializedName("channelNo")
        private int channelNo;

        public ChannelResponse() {}

        public ChannelResponse(String commandType, int status, int channelNo, String message) {
            super(commandType, status, message);
            this.channelNo = channelNo;
        }

        public int getChannelNo() { return channelNo; }
        public void setChannelNo(int channelNo) { this.channelNo = channelNo; }
    }

    /**
     * 通道状态信息模型
     */
    public static class ChannelStatus {
        @SerializedName("channelNo")
        private int channelNo;

        @SerializedName("lockStatus")
        private int lockStatus;  // 0=打开, 1=关闭, 255=失败

        @SerializedName("isLocked")
        private boolean isLocked; // true=关闭, false=打开

        public ChannelStatus() {}

        public ChannelStatus(int channelNo, int lockStatus) {
            this.channelNo = channelNo;
            this.lockStatus = lockStatus;
            this.isLocked = (lockStatus == 0x01); // 0x01表示关闭
        }

        public int getChannelNo() { return channelNo; }
        public void setChannelNo(int channelNo) { this.channelNo = channelNo; }

        public int getLockStatus() { return lockStatus; }
        public void setLockStatus(int lockStatus) {
            this.lockStatus = lockStatus;
            this.isLocked = (lockStatus == 0x01);
        }

        public boolean isLocked() { return isLocked; }

        public String getLockStatusText() {
            switch (lockStatus) {
                case 0x00: return "打开";
                case 0x01: return "关闭";
                case 0xFF: return "失败";
                default: return "未知";
            }
        }
    }

    /**
     * 状态+通道+锁状态响应模型 (82, 83)
     */
    public static class LockStatusResponse extends ChannelResponse {
        @SerializedName("channelStatus")
        private ChannelStatus channelStatusInfo;

        public LockStatusResponse() {}

        public LockStatusResponse(String commandType, int status, int channelNo, int lockStatus, String message) {
            super(commandType, status, channelNo, message);
            this.channelStatusInfo = new ChannelStatus(channelNo, lockStatus);
        }

        public ChannelStatus getChannelStatusInfo() { return channelStatusInfo; }
        public void setChannelStatusInfo(ChannelStatus channelStatusInfo) { this.channelStatusInfo = channelStatusInfo; }
    }

    /**
     * 全部门锁状态响应模型 (84)
     */
    public static class AllLocksStatusResponse extends BaseResponse {
        @SerializedName("channelCount")
        private int channelCount;

        @SerializedName("channelStatus")
        private List<ChannelStatus> channelStatusList;

        public AllLocksStatusResponse() {}

        public AllLocksStatusResponse(int status, int channelCount, List<ChannelStatus> channelStatusList) {
            super("get_all_locks_status", status, "查询全部门锁状态");
            this.channelCount = channelCount;
            this.channelStatusList = channelStatusList;
        }

        public int getChannelCount() { return channelCount; }
        public void setChannelCount(int channelCount) { this.channelCount = channelCount; }

        public List<ChannelStatus> getChannelStatusList() { return channelStatusList; }
        public void setChannelStatusList(List<ChannelStatus> channelStatusList) { this.channelStatusList = channelStatusList; }
    }

    /**
     * 主动上报响应模型 (85)
     */
    public static class StatusUploadResponse {
        @SerializedName("commandType")
        private String commandType;

        @SerializedName("channelNo")
        private int channelNo;

        @SerializedName("channelStatus")
        private ChannelStatus channelStatusInfo;

        @SerializedName("message")
        private String message;

        public StatusUploadResponse() {}

        public StatusUploadResponse(int channelNo, int lockStatus) {
            this.commandType = "status_upload";
            this.channelNo = channelNo;
            this.channelStatusInfo = new ChannelStatus(channelNo, lockStatus);
            this.message = String.format("通道%d状态变化：%s", channelNo, channelStatusInfo.getLockStatusText());
        }

        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }

        public int getChannelNo() { return channelNo; }
        public void setChannelNo(int channelNo) { this.channelNo = channelNo; }

        public ChannelStatus getChannelStatusInfo() { return channelStatusInfo; }
        public void setChannelStatusInfo(ChannelStatus channelStatusInfo) { this.channelStatusInfo = channelStatusInfo; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}