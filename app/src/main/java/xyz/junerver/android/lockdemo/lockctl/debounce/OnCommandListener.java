package xyz.junerver.android.lockdemo.lockctl.debounce;

/**
 * 指令执行监听器
 * 用于监听单个指令的执行结果
 */
public interface OnCommandListener {

    /**
     * 指令执行成功
     */
    void onSuccess();

    /**
     * 指令执行失败
     *
     * @param error 错误信息
     */
    void onError(String error);
}