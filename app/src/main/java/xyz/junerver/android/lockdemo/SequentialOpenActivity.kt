package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SequentialOpenActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var tvSelectedLocks: TextView
  private lateinit var tvResponseData: TextView
  private lateinit var tvCurrentMode: TextView
  private lateinit var tvDescription: TextView
  private lateinit var gridLocks: GridLayout

  private val selectedLocks = mutableSetOf<Int>()
  private val responseLog = StringBuilder()
  private val handler = Handler(Looper.getMainLooper())

  // 开锁模式：true=依次开锁，false=同步开锁
  private var isSequentialMode = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_sequential_open)

    lockCtl = LockCtlBoardUtil.getInstance()

    initViews()
    setupLockButtons()
    setupButtonListeners()
    setupSerialListener()
  }

  private fun initViews() {
    tvSelectedLocks = findViewById(R.id.tvSelectedLocks)
    tvResponseData = findViewById(R.id.tvResponseData)
    tvCurrentMode = findViewById(R.id.tvCurrentMode)
    tvDescription = findViewById(R.id.tvDescription)
    gridLocks = findViewById(R.id.gridLocks)

    // 初始化UI显示
    updateModeDisplay()

    createLockButtons()
  }

  private fun createLockButtons() {
    // 创建7个锁的按钮
    for (i in 1..7) {
      val button = Button(this).apply {
        text = "锁 $i"
        id = View.generateViewId()
        setOnClickListener {
          toggleLockSelection(i)
        }
        layoutParams = GridLayout.LayoutParams().apply {
          width = 0
          height = GridLayout.LayoutParams.WRAP_CONTENT
          columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
          setMargins(4, 4, 4, 4)
        }
      }

      gridLocks.addView(button)
    }
  }

  private fun setupLockButtons() {
    // 为每个按钮设置初始状态
    for (i in 0 until gridLocks.childCount) {
      val button = gridLocks.getChildAt(i) as Button
      updateButtonState(button, i + 1, false)
    }
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // 全选按钮
    findViewById<Button>(R.id.btnSelectAll).setOnClickListener {
      for (i in 1..7) {
        selectedLocks.add(i)
      }
      updateSelectedLocksDisplay()
      updateAllButtonStates()
    }

    // 清空按钮
    findViewById<Button>(R.id.btnClearAll).setOnClickListener {
      selectedLocks.clear()
      updateSelectedLocksDisplay()
      updateAllButtonStates()
    }

    // 切换模式按钮
    findViewById<Button>(R.id.btnToggleMode).setOnClickListener {
      toggleOpenMode()
    }

    // 执行开锁按钮
    findViewById<Button>(R.id.btnExecuteOpen).setOnClickListener {
      executeOpen()
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

  private fun toggleLockSelection(lockId: Int) {
    if (selectedLocks.contains(lockId)) {
      selectedLocks.remove(lockId)
    } else {
      selectedLocks.add(lockId)
    }

    updateSelectedLocksDisplay()
    updateButtonState(
      gridLocks.getChildAt(lockId - 1) as Button,
      lockId,
      selectedLocks.contains(lockId)
    )
  }

  private fun updateButtonState(button: Button, lockId: Int, isSelected: Boolean) {
    if (isSelected) {
      button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
      button.setTextColor(resources.getColor(android.R.color.white))
    } else {
      button.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
      button.setTextColor(resources.getColor(android.R.color.white))
    }
  }

  private fun updateAllButtonStates() {
    for (i in 0 until gridLocks.childCount) {
      val button = gridLocks.getChildAt(i) as Button
      val lockId = i + 1
      updateButtonState(button, lockId, selectedLocks.contains(lockId))
    }
  }

  private fun updateSelectedLocksDisplay() {
    if (selectedLocks.isEmpty()) {
      tvSelectedLocks.text = "已选择：无"
    } else {
      val sortedLocks = selectedLocks.sorted()
      tvSelectedLocks.text = "已选择：${sortedLocks.joinToString(", ")}"
    }
  }

  private fun executeOpen() {
    if (selectedLocks.isEmpty()) {
      showToast("请选择要开启的锁")
      return
    }

    val lockIds = selectedLocks.toIntArray()
    val success = if (isSequentialMode) {
      lockCtl.openMultipleLocksSequentially(*lockIds)
    } else {
      lockCtl.openMultipleLocksSimultaneously(*lockIds)
    }

    if (success) {
      val modeText = if (isSequentialMode) "依次开锁" else "同步开锁"
      showToast("正在${modeText}：${selectedLocks.joinToString(", ")}")
      appendResponseData("开始${modeText}：${lockIds.joinToString(", ")}")
    } else {
      val modeText = if (isSequentialMode) "依次开锁" else "同步开锁"
      showToast("${modeText}失败")
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

  /**
   * 切换开锁模式
   */
  private fun toggleOpenMode() {
    isSequentialMode = !isSequentialMode
    updateModeDisplay()

    val modeText = if (isSequentialMode) "依次开锁" else "同步开锁"
    showToast("已切换到${modeText}模式")
  }

  /**
   * 更新模式显示
   */
  private fun updateModeDisplay() {
    val modeText = if (isSequentialMode) "依次开锁" else "同步开锁"
    val descriptionText = if (isSequentialMode) {
      "选择要依次开启的锁（按顺序逐一打开）"
    } else {
      "选择要同时开启的锁（同时打开多个锁）"
    }

    tvCurrentMode.text = modeText
    tvDescription.text = descriptionText
  }
}