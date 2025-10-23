package xyz.junerver.android.lockdemo.check;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardResponseModels;
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardUtil;

/**
 * 设备自检管理器
 * 负责管理自检流程、处理指令响应、建立映射关系
 */
public class DeviceCheckManager {

    private static final String TAG = "DeviceCheckManager";
    private static final String PREFS_NAME = "device_check_prefs";
    private static final String KEY_CHECK_RESULT = "last_check_result";

    // 单例实例
    private static volatile DeviceCheckManager instance;
    private final Context context;
    private final LockCtlBoardUtil lockCtl;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    // 检测状态
    private DeviceCheckResult checkResult;
    private CheckEventListener eventListener;
    private int nextLockNo = 1; // 下一个要分配的门锁序号

    /**
     * 检测事件监听器
     */
    public interface CheckEventListener {
        void onCheckStatusChanged(DeviceCheckResult.CheckStatus oldStatus, DeviceCheckResult.CheckStatus newStatus);

        void onStepChanged(String stepDescription);

        void onChannelDetected(int channelNo, boolean isOpen);

        void onLockClosed(int channelNo, int lockNo);

        void onCheckCompleted(DeviceCheckResult result);

        void onCheckFailed(String errorMessage);

        void onProgressUpdated(int closedCount, int totalCount);

        void onDeviceInfoUpdated(int totalChannels, int connectedLocksCount);
    }

    private DeviceCheckManager(Context context) {
        this.context = context.getApplicationContext();
        this.lockCtl = LockCtlBoardUtil.getInstance();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        initCheckResult();
        setupSerialListener();
    }

    /**
     * 获取单例实例
     */
    public static DeviceCheckManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceCheckManager.class) {
                if (instance == null) {
                    instance = new DeviceCheckManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 设置事件监听器
     */
    public void setEventListener(CheckEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * 开始自检流程
     */
    public boolean startCheck() {
        if (checkResult.getStatus() == DeviceCheckResult.CheckStatus.IN_PROGRESS) {
            Log.w(TAG, "检测已在进行中");
            return false;
        }

        if (!lockCtl.isSerialPortOpen()) {
            String error = "串口未连接，无法开始检测";
            Log.e(TAG, error);
            if (eventListener != null) {
                eventListener.onCheckFailed(error);
            }
            return false;
        }

        Log.i(TAG, "开始设备自检流程");
        initCheckResult();

        // 步骤1: 发送0x84指令查询当前状态（用户应该已关闭所有门锁）
        if (!sendQueryAllStatusCommand()) {
            String error = "发送查询状态指令失败";
            Log.e(TAG, error);
            if (eventListener != null) {
                eventListener.onCheckFailed(error);
            }
            return false;
        }

        return true;
    }

    /**
     * 停止自检流程
     */
    public void stopCheck() {
        if (checkResult.getStatus() == DeviceCheckResult.CheckStatus.IN_PROGRESS) {
            checkResult.setStatus(DeviceCheckResult.CheckStatus.INTERRUPTED);
            checkResult.setCheckEndTime(new Date());
            notifyStatusChanged(DeviceCheckResult.CheckStatus.INTERRUPTED);
            Log.i(TAG, "用户中断检测流程");
        }
    }

    /**
     * 重置检测结果
     */
    public void resetCheck() {
        initCheckResult();
        Log.i(TAG, "检测结果已重置");
    }

    /**
     * 获取当前检测结果
     */
    public DeviceCheckResult getCheckResult() {
        return checkResult;
    }

    /**
     * 获取上一次的检测结果
     */
    public DeviceCheckResult getLastCheckResult() {
        String json = sharedPreferences.getString(KEY_CHECK_RESULT, null);
        if (json != null) {
            try {
                return gson.fromJson(json, DeviceCheckResult.class);
            } catch (Exception e) {
                Log.e(TAG, "解析上次检测结果失败", e);
            }
        }
        return null;
    }

    // 私有方法

    /**
     * 初始化检测结果
     */
    private void initCheckResult() {
        checkResult = new DeviceCheckResult();
        checkResult.setStatus(DeviceCheckResult.CheckStatus.NOT_STARTED);
        nextLockNo = 1;
    }

    /**
     * 设置串口监听器
     */
    private void setupSerialListener() {
        lockCtl.setOnDataReceived(new LockCtlBoardUtil.OnDataReceived() {
            @Override
            public void onDataReceived(String json) {
                Log.d(TAG, "onDataReceived: " + json);
                handleResponseData(json);
            }
        });
    }

    /**
     * 发送开所有锁指令 (0x86)
     */
    private boolean sendOpenAllLocksCommand() {
        updateStatus(DeviceCheckResult.CheckStatus.IN_PROGRESS);
        updateStep("正在发送开所有锁指令 (0x86)");

        return lockCtl.openAllLocksSequentially();
    }

    /**
     * 发送查询所有状态指令 (0x84)
     */
    private boolean sendQueryAllStatusCommand() {
        updateStep("正在发送查询所有锁状态指令 (0x84)");
        return lockCtl.getAllLocksStatus();
    }

    /**
     * 处理响应数据
     */
    private void handleResponseData(String json) {
        try {
            Log.d(TAG, "收到响应数据: " + json);

            // 解析指令字
            int instruction = parseInstructionFromJson(json);
            if (instruction == -1) {
                Log.w(TAG, "无法解析指令字");
                return;
            }

            switch (instruction) {
                case 0x86:
                    handleOpenAllLocksResponse(json);
                    break;
                case 0x84:
                    handleQueryAllStatusResponse(json);
                    break;
                case 0x85:
                    handleLockStatusReport(json);
                    break;
                default:
                    Log.d(TAG, "忽略指令字: 0x" + Integer.toHexString(instruction));
            }
        } catch (Exception e) {
            Log.e(TAG, "处理响应数据异常", e);
        }
    }

    /**
     * 处理开所有锁响应 (0x86) - 第二步：所有锁已打开
     */
    private void handleOpenAllLocksResponse(String json) {
        updateStep("所有门锁已打开，请按顺序关门（从第1个门开始）");

        updateStatus(DeviceCheckResult.CheckStatus.WAITING_USER_ACTION);

        if (eventListener != null) {
            eventListener.onProgressUpdated(0, checkResult.getConnectedLocksCount());
        }
    }

    /**
     * 处理查询所有状态响应 (0x84) - 第一步：检测已连接的门锁
     */
    private void handleQueryAllStatusResponse(String json) {
        updateStep("正在分析已连接的门锁状态");

        try {
            // 解析0x84响应，提取通道数量和关闭状态（已连接的门锁）
            parseAllLocksStatusFromJson(json);

            // 立即通知Activity更新设备信息显示
            if (eventListener != null) {
                eventListener.onDeviceInfoUpdated(checkResult.getTotalChannels(), checkResult.getConnectedLocksCount());
            }

            if (checkResult.getConnectedLocksCount() > 0) {
                updateStep(String.format("检测到 %d 个已连接的门锁，正在打开所有门锁...", checkResult.getConnectedLocksCount()));

                // 步骤2: 发送0x86指令打开所有锁
                if (!sendOpenAllLocksCommand()) {
                    String error = "发送开所有锁指令失败";
                    Log.e(TAG, error);
                    if (eventListener != null) {
                        eventListener.onCheckFailed(error);
                    }
                }
            } else {
                String error = "未检测到任何连接的门锁，请确保所有门锁已正确连接并处于关闭状态";
                Log.w(TAG, error);
                if (eventListener != null) {
                    eventListener.onCheckFailed(error);
                }
            }
        } catch (Exception e) {
            String error = "解析锁状态失败: " + e.getMessage();
            Log.e(TAG, error, e);
            if (eventListener != null) {
                eventListener.onCheckFailed(error);
            }
        }
    }

    /**
     * 处理锁状态上报 (0x85)
     */
    private void handleLockStatusReport(String json) {
        if (checkResult.getStatus() != DeviceCheckResult.CheckStatus.WAITING_USER_ACTION) {
            return;
        }

        try {
            // 解析0x85状态上报，提取通道号和锁状态
            int channelNo = parseChannelFromStatusUpload(json);
            boolean isOpen = parseLockStatusFromStatusUpload(json);

            if (!isOpen) {
                // 门已关闭，分配门锁序号
                handleLockClosed(channelNo);
            }

            if (eventListener != null) {
                eventListener.onChannelDetected(channelNo, isOpen);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理锁状态上报异常", e);
        }
    }

    /**
     * 处理门关闭事件
     */
    private void handleLockClosed(int channelNo) {
        if (!checkResult.getChannelToLockMapping().containsKey(channelNo)) {
            // 分配新的门锁序号
            checkResult.addChannelMapping(channelNo, nextLockNo);

            Log.i(TAG, String.format("检测到通道 %d 关闭，分配门锁序号 %d", channelNo, nextLockNo));

            checkResult.setClosedLocksCount(checkResult.getClosedLocksCount() + 1);

            if (eventListener != null) {
                eventListener.onLockClosed(channelNo, nextLockNo);
                eventListener.onProgressUpdated(
                        checkResult.getClosedLocksCount(),
                        checkResult.getConnectedLocksCount()
                );
            }

            nextLockNo++;

            // 检查是否完成所有门锁的映射
            if (checkResult.isMappingComplete()) {
                completeCheck();
            } else {
                updateStep(String.format("请关闭第 %d 个门", nextLockNo));
            }
        }
    }

    /**
     * 完成检测
     */
    private void completeCheck() {
        checkResult.setStatus(DeviceCheckResult.CheckStatus.COMPLETED);
        checkResult.setCheckEndTime(new Date());
        updateStep("检测完成！所有门锁映射关系已建立");

        // 保存检测结果
        saveCheckResult();

        Log.i(TAG, "设备自检完成: " + checkResult.toString());

        if (eventListener != null) {
            eventListener.onCheckCompleted(checkResult);
        }
    }

    /**
     * 保存检测结果
     */
    private void saveCheckResult() {
        try {
            String json = gson.toJson(checkResult);
            sharedPreferences.edit()
                    .putString(KEY_CHECK_RESULT, json)
                    .apply();
            Log.i(TAG, "检测结果已保存");
        } catch (Exception e) {
            Log.e(TAG, "保存检测结果失败", e);
        }
    }

    /**
     * 更新检测状态
     */
    private void updateStatus(DeviceCheckResult.CheckStatus newStatus) {
        DeviceCheckResult.CheckStatus oldStatus = checkResult.getStatus();
        checkResult.setStatus(newStatus);

        if (newStatus == DeviceCheckResult.CheckStatus.IN_PROGRESS) {
            checkResult.setCheckStartTime(new Date());
        } else if (newStatus == DeviceCheckResult.CheckStatus.COMPLETED ||
                newStatus == DeviceCheckResult.CheckStatus.FAILED ||
                newStatus == DeviceCheckResult.CheckStatus.INTERRUPTED) {
            checkResult.setCheckEndTime(new Date());
        }

        notifyStatusChanged(newStatus);
    }

    /**
     * 更新当前步骤描述
     */
    private void updateStep(String stepDescription) {
        checkResult.setCurrentStep(stepDescription);
        Log.d(TAG, "检测步骤: " + stepDescription);

        if (eventListener != null) {
            eventListener.onStepChanged(stepDescription);
        }
    }

    /**
     * 通知状态变化
     */
    private void notifyStatusChanged(DeviceCheckResult.CheckStatus newStatus) {
        if (eventListener != null) {
            eventListener.onCheckStatusChanged(checkResult.getStatus(), newStatus);
        }
    }

    // JSON解析辅助方法

    /**
     * 从JSON响应中解析指令字
     */
    private int parseInstructionFromJson(String json) {
        try {
            com.google.gson.JsonObject jsonObject = gson.fromJson(json, com.google.gson.JsonObject.class);
            if (jsonObject != null && jsonObject.has("commandType")) {
                String commandType = jsonObject.get("commandType").getAsString();
                switch (commandType) {
                    case "open_all_locks":
                        return 0x86;
                    case "get_all_locks_status":
                        return 0x84;
                    case "status_upload":
                        return 0x85;
                    default:
                        return -1;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析指令字失败", e);
        }
        return -1;
    }

    /**
     * 从0x84响应中解析锁状态信息 - 检测已连接的门锁（关闭状态）
     */
    private void parseAllLocksStatusFromJson(String json) {
        try {
            LockCtlBoardResponseModels.AllLocksStatusResponse response =
                    gson.fromJson(json, LockCtlBoardResponseModels.AllLocksStatusResponse.class);

            if (response != null) {
                checkResult.setTotalChannels(response.getChannelCount());

                List<Integer> connectedChannels = new ArrayList<>();
                int connectedLocksCount = 0;

                if (response.getChannelStatusList() != null) {
                    for (LockCtlBoardResponseModels.ChannelStatus channelStatus : response.getChannelStatusList()) {
                        // 统计关闭状态的通道（只有已连接的门锁才能显示关闭状态）
                        if (channelStatus.getLockStatus() == 0x01) { // 0x01 = 关闭
                            connectedChannels.add(channelStatus.getChannelNo());
                            connectedLocksCount++;
                        }
                    }
                }

                checkResult.setConnectedLocksCount(connectedLocksCount);
                checkResult.setConnectedChannels(connectedChannels);

                Log.i(TAG, String.format("检测到 %d 个通道，其中 %d 个门锁已连接（关闭状态）",
                        response.getChannelCount(), connectedLocksCount));
            }
        } catch (Exception e) {
            Log.e(TAG, "解析0x84响应失败", e);
        }
    }

    /**
     * 从0x85状态上报中解析通道号
     */
    private int parseChannelFromStatusUpload(String json) {
        try {
            LockCtlBoardResponseModels.StatusUploadResponse response =
                    gson.fromJson(json, LockCtlBoardResponseModels.StatusUploadResponse.class);

            if (response != null) {
                return response.getChannelNo();
            }
        } catch (Exception e) {
            Log.e(TAG, "解析0x85通道号失败", e);
        }
        return -1;
    }

    /**
     * 从0x85状态上报中解析锁状态
     */
    private boolean parseLockStatusFromStatusUpload(String json) {
        try {
            LockCtlBoardResponseModels.StatusUploadResponse response =
                    gson.fromJson(json, LockCtlBoardResponseModels.StatusUploadResponse.class);

            if (response != null && response.getChannelStatusInfo() != null) {
                // 0x00 = 打开, 0x01 = 关闭
                return response.getChannelStatusInfo().getLockStatus() == 0x00;
            }
        } catch (Exception e) {
            Log.e(TAG, "解析0x85锁状态失败", e);
        }
        return false;
    }
}