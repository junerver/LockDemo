package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebounceTestActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var tvDebounceStatus: TextView
  private lateinit var tvResponseData: TextView
  private lateinit var switchDebounce: Switch
  private lateinit var btnBatchSend: Button
  private lateinit var btnClearLog: Button

  private val responseLog = StringBuilder()
  private val handler = Handler(Looper.getMainLooper())
  private var isDebounceEnabled = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_debounce_test)

    lockCtl = LockCtlBoardUtil.getInstance()

    initViews()
    setupButtonListeners()
    setupSerialListener()
    updateDebounceStatus()
  }

  private fun initViews() {
    tvDebounceStatus = findViewById(R.id.tvDebounceStatus)
    tvResponseData = findViewById(R.id.tvResponseData)
    switchDebounce = findViewById(R.id.switchDebounce)
    btnBatchSend = findViewById(R.id.btnBatchSend)
    btnClearLog = findViewById(R.id.btnClearLog)

    // 初始化防抖开关状态
    switchDebounce.isChecked = isDebounceEnabled
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // 防抖开关
    switchDebounce.setOnCheckedChangeListener { _, isChecked ->
      isDebounceEnabled = isChecked
      lockCtl.isUseDebounce = isDebounceEnabled
      updateDebounceStatus()

      val status = if (isDebounceEnabled) "已启用" else "已禁用"
      showToast("防抖功能$status")
      appendResponseData("系统信息：防抖功能$status")
    }

    // 批量发送按钮
    btnBatchSend.setOnClickListener {
      performBatchLockOperation()
    }

    // 清空日志按钮
    btnClearLog.setOnClickListener {
      clearLog()
    }
  }

  private fun setupSerialListener() {
    lockCtl.setOnDataReceived(object : LockCtlBoardUtil.OnDataReceived {
      override fun onDataReceived(json: String) {
        handler.post {
          appendResponseData(json)
        }
      }
    })
  }

  private fun updateDebounceStatus() {
    val statusText = if (isDebounceEnabled) {
      "防抖功能：已启用 (指令将按顺序执行，避免冲突)"
    } else {
      "防抖功能：已禁用 (指令将直接发送)"
    }

    tvDebounceStatus.text = statusText

    // 更新按钮颜色
    if (isDebounceEnabled) {
      tvDebounceStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
      btnBatchSend.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
    } else {
      tvDebounceStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
      btnBatchSend.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark))
    }
  }

  private fun performBatchLockOperation() {
    appendResponseData("开始批量操作：循环发送1-7号锁开锁指令 (0x82)")

    // 循环发送1-7号锁的开锁指令 (0x82)
    for (lockId in 1..7) {
      val success = lockCtl.openSingleLock(lockId)

      if (success) {
        appendResponseData("发送成功：锁 $lockId 开锁指令")
      } else {
        appendResponseData("发送失败：锁 $lockId 开锁指令")
      }

      // 添加短暂延迟，避免发送过快
      try {
        Thread.sleep(100)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        break
      }
    }

    appendResponseData("批量操作完成：共发送7条开锁指令")
    showToast("批量开锁指令发送完成")
  }

  private fun appendResponseData(data: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      .format(Date())
    responseLog.append("[$timestamp] $data\n")

    // 限制日志长度
    val lines = responseLog.toString().split("\n")
    if (lines.size > 500) {
      responseLog.clear()
      for (i in lines.size - 500 until lines.size) {
        responseLog.append(lines[i]).append("\n")
      }
    }

    tvResponseData.text = responseLog.toString()

    // 自动滚动到底部
    val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
    scrollView.post {
      scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
    }
  }

  private fun clearLog() {
    responseLog.clear()
    tvResponseData.text = "日志已清空，等待新的操作..."
    showToast("日志已清空")
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }
}