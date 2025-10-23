package xyz.junerver.android.lockdemo.check;

/**
 * 通道映射关系数据模型
 * 表示锁控板通道号与实际门锁序号的对应关系
 */
public class ChannelMapping {

    /**
     * 门锁状态枚举
     */
    public enum LockStatus {
        OPEN,       // 开门
        CLOSED,     // 关门
        UNKNOWN     // 未知状态
    }

    private int channelNo;      // 锁控板通道号
    private int lockNo;         // 实际门锁序号
    private LockStatus status;  // 当前锁状态
    private boolean isConnected; // 是否连接了门锁
    private long closeTimestamp; // 关门时间戳
    private String note;        // 备注

    // 构造函数
    public ChannelMapping(int channelNo) {
        this.channelNo = channelNo;
        this.lockNo = -1; // 默认未分配门锁序号
        this.status = LockStatus.UNKNOWN;
        this.isConnected = false;
        this.closeTimestamp = 0;
        this.note = "";
    }

    public ChannelMapping(int channelNo, int lockNo) {
        this.channelNo = channelNo;
        this.lockNo = lockNo;
        this.status = LockStatus.UNKNOWN;
        this.isConnected = false;
        this.closeTimestamp = 0;
        this.note = "";
    }

    // Getter和Setter方法
    public int getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(int channelNo) {
        this.channelNo = channelNo;
    }

    public int getLockNo() {
        return lockNo;
    }

    public void setLockNo(int lockNo) {
        this.lockNo = lockNo;
    }

    public LockStatus getStatus() {
        return status;
    }

    public void setStatus(LockStatus status) {
        this.status = status;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public long getCloseTimestamp() {
        return closeTimestamp;
    }

    public void setCloseTimestamp(long closeTimestamp) {
        this.closeTimestamp = closeTimestamp;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // 业务方法

    /**
     * 设置为已连接状态
     */
    public void markAsConnected() {
        this.isConnected = true;
    }

    /**
     * 设置锁状态并记录关门时间
     *
     * @param status 新的锁状态
     */
    public void updateStatus(LockStatus status) {
        this.status = status;
        if (status == LockStatus.CLOSED) {
            this.closeTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * 分配门锁序号
     *
     * @param lockNo 门锁序号
     */
    public void assignLockNo(int lockNo) {
        this.lockNo = lockNo;
    }

    /**
     * 检查是否有有效的门锁序号
     *
     * @return 是否有门锁序号
     */
    public boolean hasLockNo() {
        return lockNo > 0;
    }

    /**
     * 检查是否有关门记录
     *
     * @return 是否有关门时间戳
     */
    public boolean hasClosedRecord() {
        return closeTimestamp > 0;
    }

    /**
     * 获取状态的中文描述
     *
     * @return 状态描述
     */
    public String getStatusDescription() {
        switch (status) {
            case OPEN:
                return "开门";
            case CLOSED:
                return "关门";
            case UNKNOWN:
            default:
                return "未知";
        }
    }

    /**
     * 获取连接状态描述
     *
     * @return 连接状态描述
     */
    public String getConnectionDescription() {
        return isConnected ? "已连接" : "未连接";
    }

    @Override
    public String toString() {
        return "ChannelMapping{" +
                "channelNo=" + channelNo +
                ", lockNo=" + lockNo +
                ", status=" + status +
                ", isConnected=" + isConnected +
                ", closeTimestamp=" + closeTimestamp +
                ", note='" + note + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChannelMapping that = (ChannelMapping) o;

        return channelNo == that.channelNo;
    }

    @Override
    public int hashCode() {
        return channelNo;
    }
}