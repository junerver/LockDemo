package xyz.junerver.android.lockdemo.debounce;

/**
 * 指令发送接口
 * 抽象层，支持各种通信方式（串口、网络、蓝牙等）
 */
public interface CommandSender {

    /**
     * 发送指令
     *
     * @param command 指令数据
     */
    void sendCommand(byte[] command);

    /**
     * 设置响应监听器
     *
     * @param listener 响应监听器
     */
    void setOnResponseListener(OnResponseListener listener);

    /**
     * 检查连接状态
     *
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 关闭连接
     */
    void disconnect();
}