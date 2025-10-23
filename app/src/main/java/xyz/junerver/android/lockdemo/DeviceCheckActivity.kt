package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.junerver.android.lockdemo.check.DeviceCheckManager
import xyz.junerver.android.lockdemo.check.DeviceCheckResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceCheckActivity : AppCompatActivity() {

  private lateinit var deviceCheckManager: DeviceCheckManager
  private lateinit var tvCheckStatus: TextView
  private lateinit var tvCurrentStep: TextView
  private lateinit var tvTotalChannels: TextView
  private lateinit var tvConnectedLocks: TextView
  private lateinit var tvClosedLocks: TextView
  private lateinit var tvMappingInfo: TextView
  private lateinit var tvProgress: TextView
  private lateinit var tvLogData: TextView
  private lateinit var tvGuide: TextView
  private lateinit var progressBar: ProgressBar
  private lateinit var layoutGuide: View
  private lateinit var btnStartCheck: Button
  private lateinit var btnStopCheck: Button
  private lateinit var btnClearLog: Button

  private val handler = Handler(Looper.getMainLooper())
  private val logData = StringBuilder()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_device_check)

    initViews()
    setupClickListeners()
    setupCheckManager()

    appendLog("设备自检页面已加载")
    appendLog("请点击'开始检测'按钮开始自检流程")
  }

  private fun initViews() {
    tvCheckStatus = findViewById(R.id.tvCheckStatus)
    tvCurrentStep = findViewById(R.id.tvCurrentStep)
    tvTotalChannels = findViewById(R.id.tvTotalChannels)
    tvConnectedLocks = findViewById(R.id.tvConnectedLocks)
    tvClosedLocks = findViewById(R.id.tvClosedLocks)
    tvMappingInfo = findViewById(R.id.tvMappingInfo)
    tvProgress = findViewById(R.id.tvProgress)
    tvLogData = findViewById(R.id.tvLogData)
    tvGuide = findViewById(R.id.tvGuide)
    progressBar = findViewById(R.id.progressBar)
    layoutGuide = findViewById(R.id.layoutGuide)
    btnStartCheck = findViewById(R.id.btnStartCheck)
    btnStopCheck = findViewById(R.id.btnStopCheck)
    btnClearLog = findViewById(R.id.btnClearLog)

    // 初始化UI状态
    updateCheckStatusUI(DeviceCheckResult.CheckStatus.NOT_STARTED)
    // 注意：updateDataDisplay() 需要在setupCheckManager()之后调用
  }

  private fun setupClickListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // 开始检测按钮
    btnStartCheck.setOnClickListener {
      startDeviceCheck()
    }

    // 停止检测按钮
    btnStopCheck.setOnClickListener {
      stopDeviceCheck()
    }

    // 清空日志按钮
    btnClearLog.setOnClickListener {
      clearLog()
    }
  }

  private fun setupCheckManager() {
    deviceCheckManager = DeviceCheckManager.getInstance(this)

    deviceCheckManager.setEventListener(object : DeviceCheckManager.CheckEventListener {
      override fun onCheckStatusChanged(
        oldStatus: DeviceCheckResult.CheckStatus,
        newStatus: DeviceCheckResult.CheckStatus
      ) {
        handler.post {
          updateCheckStatusUI(newStatus)
          appendLog("检测状态变更: ${getStatusText(oldStatus)} -> ${getStatusText(newStatus)}")
        }
      }

      override fun onStepChanged(stepDescription: String) {
        handler.post {
          tvCurrentStep.text = stepDescription
          appendLog("步骤: $stepDescription")
        }
      }

      override fun onChannelDetected(channelNo: Int, isOpen: Boolean) {
        handler.post {
          val status = if (isOpen) "开门" else "关门"
          appendLog("检测到通道 $channelNo: $status")
          updateDataDisplay()
        }
      }

      override fun onLockClosed(channelNo: Int, lockNo: Int) {
        handler.post {
          appendLog("映射建立: 通道 $channelNo -> 门锁 $lockNo")
          updateDataDisplay()
          updateMappingDisplay()
        }
      }

      override fun onCheckCompleted(result: DeviceCheckResult) {
        handler.post {
          appendLog("检测完成！")
          appendLog("总通道数: ${result.totalChannels}")
          appendLog("连接门锁数: ${result.connectedLocksCount}")
          appendLog("映射关系数: ${result.channelToLockMapping.size}")
          appendLog("检测耗时: ${result.checkDuration}ms")

          showCompletionMessage()
        }
      }

      override fun onCheckFailed(errorMessage: String) {
        handler.post {
          appendLog("检测失败: $errorMessage")
          Toast.makeText(this@DeviceCheckActivity, "检测失败: $errorMessage", Toast.LENGTH_LONG)
            .show()
        }
      }


      override fun onProgressUpdated(closedCount: Int, totalCount: Int) {
        handler.post {
          updateProgress(closedCount, totalCount)
        }
      }

      override fun onDeviceInfoUpdated(totalChannels: Int, connectedLocksCount: Int) {
        handler.post {
          appendLog("设备信息已更新: 总通道数=$totalChannels, 连接门锁数=$connectedLocksCount")
          updateDataDisplay()
        }
      }
    })

    // 初始化数据显示
    updateDataDisplay()
  }

  private fun startDeviceCheck() {
    appendLog("开始设备检测流程...")
    appendLog("请确保所有门锁已处于关闭状态")

    if (deviceCheckManager.startCheck()) {
      appendLog("检测流程已启动，正在检测已连接的门锁...")
    } else {
      appendLog("检测启动失败，请检查串口连接")
      Toast.makeText(this, "检测启动失败，请检查串口连接", Toast.LENGTH_SHORT).show()
    }
  }

  private fun stopDeviceCheck() {
    appendLog("用户请求停止检测")
    deviceCheckManager.stopCheck()
    Toast.makeText(this, "检测已停止", Toast.LENGTH_SHORT).show()
  }

  private fun updateCheckStatusUI(status: DeviceCheckResult.CheckStatus) {
    when (status) {
      DeviceCheckResult.CheckStatus.NOT_STARTED -> {
        tvCheckStatus.text = "未开始"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        btnStartCheck.isEnabled = true
        btnStopCheck.isEnabled = false
        layoutGuide.visibility = View.GONE
        progressBar.visibility = View.GONE
        tvProgress.visibility = View.GONE
      }

      DeviceCheckResult.CheckStatus.IN_PROGRESS -> {
        tvCheckStatus.text = "检测中"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        btnStartCheck.isEnabled = false
        btnStopCheck.isEnabled = true
        layoutGuide.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        updateGuideMessage("正在执行检测流程，请稍候...")
      }

      DeviceCheckResult.CheckStatus.WAITING_USER_ACTION -> {
        tvCheckStatus.text = "等待用户操作"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        btnStartCheck.isEnabled = false
        btnStopCheck.isEnabled = true
        layoutGuide.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        updateGuideMessage("请按照以下步骤操作：\n\n1. 所有门锁已打开，请从第1个门开始依次关闭\n2. 每关闭一个门，系统会自动建立映射关系\n3. 直到所有门都关闭完成")
      }

      DeviceCheckResult.CheckStatus.COMPLETED -> {
        tvCheckStatus.text = "检测完成"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        btnStartCheck.isEnabled = true
        btnStopCheck.isEnabled = false
        layoutGuide.visibility = View.VISIBLE
        updateGuideMessage("检测完成！\n\n系统已成功建立通道与门锁的映射关系。\n\n检测结果：\n• 总通道数：${deviceCheckManager.checkResult.totalChannels}\n• 连接门锁数：${deviceCheckManager.checkResult.connectedLocksCount}\n• 映射关系数：${deviceCheckManager.checkResult.channelToLockMapping.size}\n\n您现在可以正常使用门锁控制功能了。")
      }

      DeviceCheckResult.CheckStatus.FAILED -> {
        tvCheckStatus.text = "检测失败"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        btnStartCheck.isEnabled = true
        btnStopCheck.isEnabled = false
        layoutGuide.visibility = View.VISIBLE
        updateGuideMessage("检测失败！\n\n请检查以下项目：\n\n1. 串口连接是否正常\n2. 锁控板是否通电\n3. 门锁是否正确连接\n4. 确保所有门锁处于关闭状态后再重新尝试检测")
      }

      DeviceCheckResult.CheckStatus.INTERRUPTED -> {
        tvCheckStatus.text = "用户中断"
        tvCheckStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
        btnStartCheck.isEnabled = true
        btnStopCheck.isEnabled = false
        layoutGuide.visibility = View.VISIBLE
        updateGuideMessage("检测已被用户中断。\n\n您可以重新点击'开始检测'按钮来继续检测流程。")
      }
    }
  }

  private fun updateGuideMessage(message: String) {
    tvGuide.text = message
  }

  private fun updateDataDisplay() {
    try {
      val result = deviceCheckManager.checkResult

      tvTotalChannels.text = if (result.totalChannels > 0) result.totalChannels.toString() else "--"
      tvConnectedLocks.text =
        if (result.connectedLocksCount > 0) result.connectedLocksCount.toString() else "--"
      tvClosedLocks.text =
        if (result.closedLocksCount > 0) result.closedLocksCount.toString() else "--"
    } catch (e: Exception) {
      // 如果 deviceCheckManager 未初始化，显示默认值
      tvTotalChannels.text = "--"
      tvConnectedLocks.text = "--"
      tvClosedLocks.text = "--"
      appendLog("初始化数据显示: ${e.message}")
    }
  }

  private fun updateMappingDisplay() {
    val result = deviceCheckManager.checkResult

    if (result.channelToLockMapping.isNotEmpty()) {
      val mappingText = StringBuilder()
      mappingText.append("通道 -> 门锁 映射关系：\n\n")

      // 按门锁序号排序显示
      val sortedMapping = result.channelToLockMapping.entries
        .sortedBy { it.value }

      for ((channelNo, lockNo) in sortedMapping) {
        mappingText.append(String.format("通道 %2d -> 门锁 %2d\n", channelNo, lockNo))
      }

      tvMappingInfo.text = mappingText.toString()
    } else {
      tvMappingInfo.text = "等待检测完成..."
    }
  }

  private fun updateProgress(closedCount: Int, totalCount: Int) {
    if (totalCount > 0) {
      val progress = (closedCount * 100) / totalCount
      progressBar.progress = progress
      tvProgress.text = "$closedCount/$totalCount"
    }
  }

  private fun showCompletionMessage() {
    val result = deviceCheckManager.checkResult

    val message = StringBuilder()
    message.append("检测完成！\n\n")
    message.append("检测结果摘要：\n")
    message.append("• 总通道数：${result.totalChannels}\n")
    message.append("• 连接门锁数：${result.connectedLocksCount}\n")
    message.append("• 映射关系数：${result.channelToLockMapping.size}\n")
    message.append("• 检测耗时：${result.checkDuration}ms\n\n")

    if (result.isMappingComplete()) {
      message.append("✅ 所有门锁映射关系已建立完成！")
    } else {
      message.append("⚠️ 部分门锁未完成映射，可能存在连接问题")
    }

    Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show()
  }

  private fun appendLog(message: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    logData.append("[$timestamp] $message\n")

    // 限制日志长度
    val lines = logData.toString().split("\n")
    if (lines.size > 500) {
      logData.clear()
      for (i in lines.size - 500 until lines.size) {
        logData.append(lines[i]).append("\n")
      }
    }

    tvLogData.text = logData.toString()

    // 自动滚动到底部
    val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollLog)
    scrollView.post {
      scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
    }
  }

  private fun clearLog() {
    logData.clear()
    tvLogData.text = "日志已清空，等待新的操作...\n"
  }

  private fun getStatusText(status: DeviceCheckResult.CheckStatus): String {
    return when (status) {
      DeviceCheckResult.CheckStatus.NOT_STARTED -> "未开始"
      DeviceCheckResult.CheckStatus.IN_PROGRESS -> "检测中"
      DeviceCheckResult.CheckStatus.WAITING_USER_ACTION -> "等待用户操作"
      DeviceCheckResult.CheckStatus.COMPLETED -> "检测完成"
      DeviceCheckResult.CheckStatus.FAILED -> "检测失败"
      DeviceCheckResult.CheckStatus.INTERRUPTED -> "用户中断"
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // 停止检测（如果正在进行）
    if (deviceCheckManager.checkResult.status == DeviceCheckResult.CheckStatus.IN_PROGRESS ||
      deviceCheckManager.checkResult.status == DeviceCheckResult.CheckStatus.WAITING_USER_ACTION
    ) {
      deviceCheckManager.stopCheck()
    }
  }
}