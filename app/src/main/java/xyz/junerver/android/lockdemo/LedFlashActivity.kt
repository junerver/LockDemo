package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedFlashActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var tvResponseData: TextView

  private val responseLog = StringBuilder()
  private val handler = Handler(Looper.getMainLooper())

  // LED状态管理：false=关闭，true=闪烁
  private val ledStates = mutableMapOf<Int, Boolean>()

  // 按钮缓存
  private val ledButtons = mutableMapOf<Int, Button>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_led_flash)

    lockCtl = LockCtlBoardUtil.getInstance()

    initViews()
    setupButtonListeners()
    setupSerialListener()
  }

  private fun initViews() {
    tvResponseData = findViewById(R.id.tvResponseData)

    // 初始化LED按钮缓存
    ledButtons[1] = findViewById(R.id.btnLED1)
    ledButtons[2] = findViewById(R.id.btnLED2)
    ledButtons[3] = findViewById(R.id.btnLED3)
    ledButtons[4] = findViewById(R.id.btnLED4)
    ledButtons[5] = findViewById(R.id.btnLED5)
    ledButtons[6] = findViewById(R.id.btnLED6)
    ledButtons[7] = findViewById(R.id.btnLED7)

    // 初始化LED状态（全部关闭）
    for (i in 1..7) {
      ledStates[i] = false
    }
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // LED闪烁按钮 - 统一处理
    for (i in 1..7) {
      ledButtons[i]?.setOnClickListener {
        toggleLEDFlash(i)
      }
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

  private fun toggleLEDFlash(ledId: Int) {
    val currentState = ledStates[ledId] == true
    val newState = !currentState

    if (newState) {
      // 开始闪烁
      val success = lockCtl.flashLockLed(ledId)
      if (success) {
        ledStates[ledId] = true
        updateLEDButtonState(ledId, true)
        appendResponseData("LED $ledId 闪烁命令发送成功")
        showToast("LED $ledId 正在闪烁")
      } else {
        appendResponseData("LED $ledId 闪烁失败")
        showToast("LED $ledId 闪烁失败")
      }
    } else {
      // 关闭闪烁（使用closeChannel关闭通道）
      val success = lockCtl.closeChannel(ledId)
      if (success) {
        ledStates[ledId] = false
        updateLEDButtonState(ledId, false)
        appendResponseData("LED $ledId 通道关闭命令发送成功")
        showToast("LED $ledId 已停止闪烁")
      } else {
        appendResponseData("LED $ledId 通道关闭失败")
        showToast("LED $ledId 通道关闭失败")
      }
    }
  }

  private fun updateLEDButtonState(ledId: Int, isFlashing: Boolean) {
    val button = ledButtons[ledId] ?: return

    if (isFlashing) {
      // 闪烁状态：改变背景色和文字
      button.setBackgroundColor(resources.getColor(android.R.color.holo_orange_dark))
      button.setTextColor(resources.getColor(android.R.color.white))
      button.text = "关闭 LED $ledId"
    } else {
      // 关闭状态：恢复默认样式
      button.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
      button.setTextColor(resources.getColor(android.R.color.white))
      button.text = "LED $ledId 闪烁"
    }
  }

  private fun appendResponseData(data: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      .format(Date())
    responseLog.append("[$timestamp] $data\n")

    // 限制日志长度
    val lines = responseLog.toString().split("\n")
    if (lines.size > 250) {
      responseLog.clear()
      for (i in lines.size - 250 until lines.size) {
        responseLog.append(lines[i]).append("\n")
      }
    }

    tvResponseData.text = responseLog.toString()
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }
}