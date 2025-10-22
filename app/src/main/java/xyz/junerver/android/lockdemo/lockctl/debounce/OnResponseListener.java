package xyz.junerver.android.lockdemo.lockctl.debounce;

/**
 * 响应监听接口
 * 用于接收指令执行的结果
 */
public interface OnResponseListener {

    /**
     * 收到响应数据
     *
     * @param response 响应数据
     */
    void onResponseReceived(byte[] response);

    /**
     * 发生错误
     *
     * @param error 错误信息
     */
    void onError(String error);
}