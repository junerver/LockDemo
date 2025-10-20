package xyz.junerver.android.lockdemo;

import android.util.Log;

import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.File;

/**
 * 门锁控制板工具类
 * 懒加载单例模式，用于控制留样柜门锁的开关、状态查询等操作
 * 注意：指令构造功能已迁移到 LockCtlBoardCmdHelper 工具类中
 */
public class LockCtlBoardUtil {
    private static final String TAG = "LockCtlBoardUtil";
    private static volatile LockCtlBoardUtil instance;
    private SerialPortManager mSerialPortManager;

    private OnDataReceived mOnDataReceived;

    public interface OnDataReceived {
        void onDataReceived(String json);
    }

    // 默认门锁总数
    private static final int DEFAULT_LOCK_COUNT = 12;

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

    // 打开串口
    public void openSerialPort() {
        if (null == mSerialPortManager) {
            mSerialPortManager = new SerialPortManager();
        }
        mSerialPortManager
                .setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                    @Override
                    public void onSuccess(File device) {
                        Log.d(TAG, "串口打开成功");
                    }

                    @Override
                    public void onFail(File device, Status status) {
                        Log.e(TAG, "onFail: " + device.getAbsolutePath());
                    }
                })
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {

                    @Override
                    public void onDataReceived(byte[] bytes) {
                        String json = LockCtlBoardCmdHelper.parseResponseToJson(bytes);
                        if (mOnDataReceived != null) {
                            mOnDataReceived.onDataReceived(json);
                        }
                        Log.d(TAG, "onDataReceived: " + json);
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {

                    }
                })
                .openSerialPort(new File("/dev/ttyS4"), 9600);
    }

    // 设置串口数据监听器
    public void setOnDataReceived(OnDataReceived onDataReceived) {
        this.mOnDataReceived = onDataReceived;
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

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand((byte) 0x00, lockIds);
        if (command == null) {
            Log.e(TAG, "构造同时开多锁指令失败");
            return false;
        }

        Log.d(TAG, "同时开启门锁: " + java.util.Arrays.toString(lockIds));
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁打开完成");

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

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildFlashChannelCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造通道闪烁指令失败");
            return false;
        }

        Log.d(TAG, "门锁 " + lockId + " LED闪烁，持续时间: " + duration + "ms");
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁LED闪烁完成");

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

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造开单锁指令失败");
            return false;
        }

        Log.d(TAG, "开启门锁: " + lockId);
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁打开完成");

        return true;
    }

    /**
     * 4. 查询单个门锁状态
     *
     * @param lockId 门锁ID
     * @return 门锁状态（0-关闭，1-打开，-1-错误）
     */
    public boolean getSingleLockStatus(int lockId) {
        if (lockId < 0 || lockId >= DEFAULT_LOCK_COUNT) {
            Log.e(TAG, "门锁ID无效: " + lockId);
            return false;
        }

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand((byte) 0x00, lockId);
        if (command == null) {
            Log.e(TAG, "构造查询单锁状态指令失败");
            return false;
        }

        Log.d(TAG, "查询门锁状态: " + lockId);
        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "门锁状态查询完成");
        return true;
    }

    /**
     * 5. 查询所有门锁状态
     *
     * @return 所有门锁状态数组
     */
    public boolean getAllLocksStatus() {
        Log.d(TAG, "查询所有门锁状态");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造查询所有锁状态指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
        Log.d(TAG, "所有门锁状态查询完成");
        return true;
    }

    /**
     * 6. 全部开锁（逐一打开）
     *
     * @return 操作是否成功
     */
    public boolean openAllLocksSequentially() {
        Log.d(TAG, "开始逐一开启所有门锁");

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenAllLocksCommand((byte) 0x00);
        if (command == null) {
            Log.e(TAG, "构造开全部锁指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);
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

        // 构造指令
        byte[] command = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand((byte) 0x00, lockIds);
        if (command == null) {
            Log.e(TAG, "构造逐一开多锁指令失败");
            return false;
        }

        Log.d(TAG, "发送指令: " + LockCtlBoardCmdHelper.bytesToHex(command));

        mSerialPortManager.sendBytes(command);

        Log.d(TAG, "指定门锁开启完成");
        return true;
    }

}