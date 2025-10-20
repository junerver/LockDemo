package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import xyz.junerver.android.lockdemo.LockCtlBoardResponseModels.ChannelStatus

class StatusQueryActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var gridStatusCards: GridLayout
  private lateinit var tvRawResponseData: TextView

  private val handler = Handler(Looper.getMainLooper())
  private val gson = Gson()
  private val lockStatusMap = mutableMapOf<Int, ChannelStatus>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_status_query)

    lockCtl = LockCtlBoardUtil.getInstance()

    initViews()
    createStatusCards()
    setupButtonListeners()
    setupSerialListener()

    // 自动查询一次状态
    queryAllStatus()
  }

  private fun initViews() {
    gridStatusCards = findViewById(R.id.gridStatusCards)
    tvRawResponseData = findViewById(R.id.tvRawResponseData)

    createStatusCards()
  }

  private fun createStatusCards() {
    // 清空现有卡片
    gridStatusCards.removeAllViews()

    // 创建7个锁的状态卡片
    for (i in 1..7) {
      val cardView = createStatusCard(i)
      gridStatusCards.addView(cardView)
    }
  }

  private fun createStatusCard(lockId: Int): View {
    val card = layoutInflater.inflate(android.R.layout.simple_list_item_2, null) as TextView
    card.apply {
      text = "锁 $lockId: 未知状态"
      textSize = 14f
      setPadding(16, 16, 16, 16)
      setBackgroundResource(android.R.drawable.btn_default)
      layoutParams = GridLayout.LayoutParams().apply {
        width = 0
        height = GridLayout.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(8, 8, 8, 8)
      }
    }

    card.setOnClickListener {
      querySingleLockStatus(lockId)
    }

    return card
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // 查询全部状态按钮
    findViewById<Button>(R.id.btnQueryAll).setOnClickListener {
      queryAllStatus()
    }

    // 刷新状态按钮
    findViewById<Button>(R.id.btnRefresh).setOnClickListener {
      queryAllStatus()
    }
  }

  private fun setupSerialListener() {
    lockCtl.setOnDataReceived(object : LockCtlBoardUtil.OnDataReceived {
      override fun onDataReceived(json: String) {
        handler.post {
          appendRawResponseData(json)
          processStatusResponse(json)
        }
      }
    })
  }

  private fun queryAllStatus() {
    val success = lockCtl.getAllLocksStatus()
    if (success) {
      appendRawResponseData("发送查询全部状态指令")
      showToast("正在查询所有锁状态...")
    } else {
      showToast("查询状态失败")
    }
  }

  private fun querySingleLockStatus(lockId: Int) {
    val success = lockCtl.getSingleLockStatus(lockId)
    if (success) {
      appendRawResponseData("发送查询锁 $lockId 状态指令")
      showToast("正在查询锁 $lockId 状态...")
    } else {
      showToast("查询锁 $lockId 状态失败")
    }
  }

  private fun processStatusResponse(json: String) {
    try {
      val jsonObject = gson.fromJson(json, JsonObject::class.java)
      val commandType = jsonObject.get("commandType")?.asString

      when (commandType) {
        "get_all_locks_status" -> {
          processGetAllStatusResponse(jsonObject)
        }

        "get_single_lock_status" -> {
          processGetSingleStatusResponse(jsonObject)
        }

        "status_upload" -> {
          processStatusUploadResponse(jsonObject)
        }

        else -> {
          // 其他响应类型的处理
        }
      }
    } catch (e: JsonSyntaxException) {
      Log.e("StatusQueryActivity", "JSON解析失败", e)
    }
  }

  private fun processGetAllStatusResponse(jsonObject: JsonObject) {
    try {
      val channelStatus =
        gson.fromJson(jsonObject.get("channelStatus").toString(), Array<ChannelStatus>::class.java)

      for (channelStatus in channelStatus) {
        val lockId = channelStatus.channelNo
        if (lockId <= 7) {
          lockStatusMap[lockId] = channelStatus
          updateStatusCard(lockId, channelStatus)
        }
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理全部门锁状态响应失败", e)
    }
  }

  private fun processGetSingleStatusResponse(jsonObject: JsonObject) {
    try {
      val channelStatusInfo =
        gson.fromJson(jsonObject.get("channelStatusInfo").toString(), ChannelStatus::class.java)
      val lockId = channelStatusInfo.channelNo

      if (lockId <= 7) {
        lockStatusMap[lockId] = channelStatusInfo
        updateStatusCard(lockId, channelStatusInfo)
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理单个门锁状态响应失败", e)
    }
  }

  private fun processStatusUploadResponse(jsonObject: JsonObject) {
    try {
      val channelStatusInfo =
        gson.fromJson(jsonObject.get("channelStatusInfo").toString(), ChannelStatus::class.java)
      val lockId = channelStatusInfo.channelNo

      if (lockId <= 7) {
        lockStatusMap[lockId] = channelStatusInfo
        updateStatusCard(lockId, channelStatusInfo)

        // 显示状态变化通知
        val statusText = if (channelStatusInfo.isLocked) "关闭" else "打开"
        showToast("锁 $lockId 状态变化：$statusText")
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理状态上传响应失败", e)
    }
  }

  private fun updateStatusCard(lockId: Int, channelStatus: ChannelStatus) {
    val cardIndex = lockId - 1
    if (cardIndex < gridStatusCards.childCount) {
      val card = gridStatusCards.getChildAt(cardIndex) as TextView

      val statusText = when (channelStatus.lockStatus) {
        0x00 -> "打开"
        0x01 -> "关闭"
        0xFF -> "错误"
        else -> "未知"
      }

      val isLocked = channelStatus.isLocked
      val color = when {
        isLocked -> resources.getColor(android.R.color.holo_red_light)
        !isLocked -> resources.getColor(android.R.color.holo_green_light)
        else -> resources.getColor(android.R.color.darker_gray)
      }

      card.text = "锁 $lockId: $statusText"
      card.setBackgroundColor(color)
    }
  }

  private fun appendRawResponseData(data: String) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
      .format(java.util.Date())
    val logEntry = "[$timestamp] $data\n"

    // 限制显示长度
    val currentText = tvRawResponseData.text.toString()
    val lines = currentText.split("\n")
    if (lines.size > 30) {
      val newText = StringBuilder()
      for (i in lines.size - 30 until lines.size) {
        newText.append(lines[i]).append("\n")
      }
      tvRawResponseData.text = newText.toString()
    }

    tvRawResponseData.append(logEntry)
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }
}