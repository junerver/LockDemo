package xyz.junerver.android.lockdemo.lockctl;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import xyz.junerver.android.lockdemo.lockctl.debounce.LockCtlBoardSerialSender;
import xyz.junerver.android.lockdemo.lockctl.debounce.OnResponseListener;

/**
 * 串口设备检测工具类
 * 用于自动检测正确的门锁控制板串口设备
 */
public class SerialPortDetector {
    private static final String TAG = "SerialPortDetector";

    // SP 首选项相关常量
    private static final String PREFS_NAME = "serial_port_prefs";
    private static final String KEY_DETECTED_PORT = "detected_port_path";

    // 检测超时时间（毫秒）
    private static final int DETECTION_TIMEOUT = 3000;

    private Context context;
    private SerialPortFinder serialPortFinder;

    public SerialPortDetector(Context context) {
        this.context = context.getApplicationContext();
        this.serialPortFinder = new SerialPortFinder();
    }

    /**
     * 检测结果回调接口
     */
    public interface OnDetectionListener {
        void onPortDetected(String portPath);

        void onDetectionFailed(String error);

        void onDetectionProgress(String currentPort);
    }

    /**
     * 获取已保存的串口路径
     *
     * @return 已保存的串口路径，如果没有保存则返回null
     */
    public String getSavedPortPath() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DETECTED_PORT, null);
    }

    /**
     * 保存检测到的串口路径
     *
     * @param portPath 串口路径
     */
    public void savePortPath(String portPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DETECTED_PORT, portPath).apply();
        Log.i(TAG, "串口路径已保存: " + portPath);
    }

    /**
     * 清除已保存的串口路径
     */
    public void clearSavedPortPath() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DETECTED_PORT).apply();
        Log.i(TAG, "已清除保存的串口路径");
    }

    /**
     * 自动检测门锁控制板串口设备
     *
     * @param listener 检测结果监听器
     */
    public void autoDetectPort(OnDetectionListener listener) {
        // 首先检查是否有已保存的串口路径
        String savedPort = getSavedPortPath();
        if (savedPort != null) {
            Log.i(TAG, "使用已保存的串口路径: " + savedPort);
            listener.onPortDetected(savedPort);
            return;
        }

        // 获取所有串口设备
        ArrayList<Device> devices = serialPortFinder.getDevices();
        if (devices == null || devices.isEmpty()) {
            String error = "未找到任何串口设备";
            Log.e(TAG, error);
            listener.onDetectionFailed(error);
            return;
        }

        // 在新线程中执行检测
        new Thread(() -> {
            for (Device device : devices) {
                if (device != null && device.getFile() != null) {
                    String portPath = device.getFile().getAbsolutePath();
                    Log.d(TAG, "正在检测串口: " + portPath);

                    // 在主线程中更新进度
                    if (listener != null) {
                        listener.onDetectionProgress(portPath);
                    }

                    // 检测当前串口
                    if (testPortConnection(portPath)) {
                        Log.i(TAG, "检测到正确的串口设备: " + portPath);

                        // 保存检测到的串口路径
                        savePortPath(portPath);

                        // 在主线程中返回结果
                        if (listener != null) {
                            listener.onPortDetected(portPath);
                        }
                        return;
                    }
                }
            }

            // 所有串口都检测完成，没有找到合适的设备
            String error = "未检测到门锁控制板设备";
            Log.e(TAG, error);
            if (listener != null) {
                listener.onDetectionFailed(error);
            }
        }).start();
    }

    /**
     * 测试指定串口是否连接了门锁控制板
     *
     * @param portPath 串口路径
     * @return true表示检测到正确的设备，false表示不是目标设备
     */
    private boolean testPortConnection(String portPath) {
        final boolean[] isConnected = {false};
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            // 创建一个临时的串口发送器用于测试
            LockCtlBoardSerialSender testSender = new LockCtlBoardSerialSender(portPath);

            // 设置响应监听器
            testSender.setOnResponseListener(new OnResponseListener() {
                @Override
                public void onResponseReceived(byte[] response) {
                    // 验证响应是否有效
                    if (LockCtlBoardCmdHelper.validateResponse(response)) {
                        Log.d(TAG, "收到有效响应: " + bytesToHex(response));
                        isConnected[0] = true;
                    }
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "串口测试错误: " + error);
                    latch.countDown();
                }
            });

            // 尝试连接串口
            if (!testSender.isConnected()) {
                Log.d(TAG, "无法连接到串口: " + portPath);
                latch.countDown();
                return false;
            }

            // 发送查询所有锁状态指令 (0x84)
            byte[] command = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand((byte) 0x00);
            if (command == null) {
                Log.e(TAG, "构造查询指令失败");
                latch.countDown();
                return false;
            }

            Log.d(TAG, "向串口 " + portPath + " 发送查询指令");
            testSender.sendCommand(command);

            // 等待响应，最多等待3秒
            boolean result = latch.await(DETECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result) {
                Log.w(TAG, "串口 " + portPath + " 响应超时");
            }

            // 断开测试连接
            testSender.disconnect();

            return isConnected[0];

        } catch (Exception e) {
            Log.e(TAG, "测试串口 " + portPath + " 时发生异常", e);
            latch.countDown();
            return false;
        }
    }

    /**
     * 强制重新检测串口设备（忽略已保存的路径）
     *
     * @param listener 检测结果监听器
     */
    public void forceRedetectPort(OnDetectionListener listener) {
        // 清除已保存的路径
        clearSavedPortPath();

        // 重新开始检测
        autoDetectPort(listener);
    }

    /**
     * 字节数组转十六进制字符串（用于日志输出）
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}