package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LedFlashActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var gridLEDs: GridLayout
  private lateinit var etFlashDuration: EditText
  private lateinit var tvResponseData: TextView

  private val selectedLEDs = mutableSetOf<Int>()
  private val responseLog = StringBuilder()
  private val handler = Handler(Looper.getMainLooper())
  private var flashDuration = 1000L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_led_flash)

    lockCtl = LockCtlBoardUtil.getInstance()

    initViews()
    createLEDButtons()
    setupButtonListeners()
    setupSerialListener()
  }

  private fun initViews() {
    gridLEDs = findViewById(R.id.gridLEDs)
    etFlashDuration = findViewById(R.id.etFlashDuration)
    tvResponseData = findViewById(R.id.tvResponseData)

    // 设置默认闪烁时间
    etFlashDuration.setText("1000")
  }

  private fun createLEDButtons() {
    // 创建7个LED控制按钮
    for (i in 1..7) {
      val button = Button(this).apply {
        text = "LED $i"
        id = View.generateViewId()
        setOnClickListener {
          toggleLEDSelection(i)
        }
        layoutParams = GridLayout.LayoutParams().apply {
          width = 0
          height = GridLayout.LayoutParams.WRAP_CONTENT
          columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
          setMargins(4, 4, 4, 4)
        }
      }

      gridLEDs.addView(button)
    }
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // 预设时间按钮
    findViewById<Button>(R.id.btnTime500).setOnClickListener {
      setFlashDuration(500)
    }

    findViewById<Button>(R.id.btnTime1000).setOnClickListener {
      setFlashDuration(1000)
    }

    findViewById<Button>(R.id.btnTime2000).setOnClickListener {
      setFlashDuration(2000)
    }

    findViewById<Button>(R.id.btnTime5000).setOnClickListener {
      setFlashDuration(5000)
    }

    // 全选按钮
    findViewById<Button>(R.id.btnSelectAll).setOnClickListener {
      for (i in 1..7) {
        selectedLEDs.add(i)
      }
      updateAllButtonStates()
    }

    // 清空按钮
    findViewById<Button>(R.id.btnClearAll).setOnClickListener {
      selectedLEDs.clear()
      updateAllButtonStates()
    }

    // 执行LED闪烁按钮
    findViewById<Button>(R.id.btnExecuteFlash).setOnClickListener {
      executeLEDFlash()
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

  private fun toggleLEDSelection(ledId: Int) {
    if (selectedLEDs.contains(ledId)) {
      selectedLEDs.remove(ledId)
    } else {
      selectedLEDs.add(ledId)
    }

    updateButtonState(gridLEDs.getChildAt(ledId - 1) as Button, selectedLEDs.contains(ledId))
  }

  private fun setFlashDuration(duration: Int) {
    flashDuration = duration.toLong()
    etFlashDuration.setText(duration.toString())
    showToast("闪烁时间设置为：${duration}ms")
  }

  private fun updateButtonState(button: Button, isSelected: Boolean) {
    if (isSelected) {
      button.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
      button.setTextColor(resources.getColor(android.R.color.white))
    } else {
      button.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
      button.setTextColor(resources.getColor(android.R.color.white))
    }
  }

  private fun updateAllButtonStates() {
    for (i in 0 until gridLEDs.childCount) {
      val button = gridLEDs.getChildAt(i) as Button
      updateButtonState(button, selectedLEDs.contains(i + 1))
    }
  }

  private fun executeLEDFlash() {
    if (selectedLEDs.isEmpty()) {
      showToast("请选择要闪烁的LED")
      return
    }

    // 验证闪烁时间
    try {
      flashDuration = etFlashDuration.text.toString().toLong()
      if (flashDuration <= 0) {
        showToast("请输入有效的闪烁时间")
        return
      }
    } catch (e: NumberFormatException) {
      showToast("请输入有效的数字")
      return
    }

    // 为每个选中的LED执行闪烁
    val ledIds = selectedLEDs.toIntArray()
    for (ledId in ledIds) {
      val success = lockCtl.flashLockLed(ledId, flashDuration)
      if (success) {
        appendResponseData("LED $ledId 开始闪烁，持续时间：${flashDuration}ms")
      } else {
        appendResponseData("LED $ledId 闪烁失败")
      }
    }

    showToast("正在闪烁LED：${selectedLEDs.joinToString(", ")}，时间：${flashDuration}ms")
  }

  private fun appendResponseData(data: String) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
      .format(java.util.Date())
    responseLog.append("[$timestamp] $data\n")

    // 限制日志长度
    val lines = responseLog.toString().split("\n")
    if (lines.size > 50) {
      responseLog.clear()
      for (i in lines.size - 50 until lines.size) {
        responseLog.append(lines[i]).append("\n")
      }
    }

    tvResponseData.text = responseLog.toString()
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }
}