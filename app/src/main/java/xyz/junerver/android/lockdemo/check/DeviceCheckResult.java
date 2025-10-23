package xyz.junerver.android.lockdemo.check;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备自检结果数据模型
 * 存储自检过程中获取的所有关键信息
 */
public class DeviceCheckResult {

    /**
     * 检测状态枚举
     */
    public enum CheckStatus {
        NOT_STARTED,        // 未开始
        IN_PROGRESS,        // 检测中
        WAITING_USER_ACTION, // 等待用户操作
        COMPLETED,          // 检测完成
        FAILED,             // 检测失败
        INTERRUPTED         // 用户中断
    }

    // 检测基本信息
    private CheckStatus status;
    private Date checkStartTime;
    private Date checkEndTime;

    // 锁控板信息
    private int totalChannels;          // 锁控板全部通道数量
    private int connectedLocksCount;    // 实际连接的门锁数量
    private List<Integer> connectedChannels; // 实际连接的通道号列表

    // 映射关系：通道号 -> 门锁序号
    private Map<Integer, Integer> channelToLockMapping;

    // 检测过程中的详细信息
    private String errorMessage;        // 错误信息
    private String currentStep;         // 当前执行步骤描述
    private int closedLocksCount;       // 已关门数量

    // 默认构造函数
    public DeviceCheckResult() {
        this.status = CheckStatus.NOT_STARTED;
        this.channelToLockMapping = new HashMap<>();
        this.connectedLocksCount = 0;
        this.closedLocksCount = 0;
        this.currentStep = "准备开始检测";
    }

    // Getter和Setter方法
    public CheckStatus getStatus() {
        return status;
    }

    public void setStatus(CheckStatus status) {
        this.status = status;
    }

    public Date getCheckStartTime() {
        return checkStartTime;
    }

    public void setCheckStartTime(Date checkStartTime) {
        this.checkStartTime = checkStartTime;
    }

    public Date getCheckEndTime() {
        return checkEndTime;
    }

    public void setCheckEndTime(Date checkEndTime) {
        this.checkEndTime = checkEndTime;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public void setTotalChannels(int totalChannels) {
        this.totalChannels = totalChannels;
    }

    public int getConnectedLocksCount() {
        return connectedLocksCount;
    }

    public void setConnectedLocksCount(int connectedLocksCount) {
        this.connectedLocksCount = connectedLocksCount;
    }

    public List<Integer> getConnectedChannels() {
        return connectedChannels;
    }

    public void setConnectedChannels(List<Integer> connectedChannels) {
        this.connectedChannels = connectedChannels;
    }

    public Map<Integer, Integer> getChannelToLockMapping() {
        return channelToLockMapping;
    }

    public void setChannelToLockMapping(Map<Integer, Integer> channelToLockMapping) {
        this.channelToLockMapping = channelToLockMapping;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public int getClosedLocksCount() {
        return closedLocksCount;
    }

    public void setClosedLocksCount(int closedLocksCount) {
        this.closedLocksCount = closedLocksCount;
    }

    // 业务方法

    /**
     * 添加通道映射关系
     *
     * @param channelNo 通道号
     * @param lockNo    门锁序号
     */
    public void addChannelMapping(int channelNo, int lockNo) {
        this.channelToLockMapping.put(channelNo, lockNo);
    }

    /**
     * 根据通道号获取门锁序号
     *
     * @param channelNo 通道号
     * @return 门锁序号，如果未找到返回-1
     */
    public int getLockNoByChannel(int channelNo) {
        Integer lockNo = channelToLockMapping.get(channelNo);
        return lockNo != null ? lockNo : -1;
    }

    /**
     * 根据门锁序号获取通道号
     *
     * @param lockNo 门锁序号
     * @return 通道号，如果未找到返回-1
     */
    public int getChannelByLockNo(int lockNo) {
        for (Map.Entry<Integer, Integer> entry : channelToLockMapping.entrySet()) {
            if (entry.getValue() == lockNo) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * 获取检测持续时间（毫秒）
     *
     * @return 持续时间，如果未完成返回0
     */
    public long getCheckDuration() {
        if (checkStartTime == null) {
            return 0;
        }
        long endTime = checkEndTime != null ? checkEndTime.getTime() : System.currentTimeMillis();
        return endTime - checkStartTime.getTime();
    }

    /**
     * 检测是否已完成（成功或失败）
     *
     * @return 是否已完成
     */
    public boolean isCheckFinished() {
        return status == CheckStatus.COMPLETED ||
                status == CheckStatus.FAILED ||
                status == CheckStatus.INTERRUPTED;
    }

    /**
     * 检测映射关系是否完整
     *
     * @return 是否所有连接的门锁都有映射关系
     */
    public boolean isMappingComplete() {
        return channelToLockMapping.size() == connectedLocksCount;
    }

    /**
     * 获取检测完成度百分比
     *
     * @return 完成度 (0-100)
     */
    public int getCompletionPercentage() {
        if (connectedLocksCount == 0) {
            return 0;
        }
        return (closedLocksCount * 100) / connectedLocksCount;
    }

    @Override
    public String toString() {
        return "DeviceCheckResult{" +
                "status=" + status +
                ", totalChannels=" + totalChannels +
                ", connectedLocksCount=" + connectedLocksCount +
                ", closedLocksCount=" + closedLocksCount +
                ", mappingSize=" + channelToLockMapping.size() +
                ", errorMessage='" + errorMessage + '\'' +
                ", currentStep='" + currentStep + '\'' +
                '}';
    }
}