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
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardResponseModels.AllLocksStatusResponse
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardResponseModels.ChannelStatus
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardResponseModels.LockStatusResponse
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardResponseModels.StatusUploadResponse
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
//    queryAllStatus()
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
    val card = layoutInflater.inflate(android.R.layout.simple_list_item_1, null) as TextView
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
      Log.d("StatusQueryActivity", "收到响应数据: $json")

      val jsonObject = gson.fromJson(json, JsonObject::class.java)

      // 打印所有可用字段，用于调试
      val keys = jsonObject.keySet()
      Log.d("StatusQueryActivity", "JSON响应包含的字段: $keys")

      val commandType = jsonObject.get("commandType")?.asString
      Log.d("StatusQueryActivity", "指令类型: $commandType")

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
          // 尝试根据响应内容自动推断类型
          Log.d("StatusQueryActivity", "未知指令类型，尝试自动推断")
          if (jsonObject.has("channelStatus") || jsonObject.has("channelStatusInfo")) {
            // 包含状态信息，可能是状态查询响应
            if (jsonObject.has("channelStatus") && jsonObject.get("channelStatus").isJsonArray) {
              Log.d("StatusQueryActivity", "推断为全部状态查询响应")
              processGetAllStatusResponse(jsonObject)
            } else if (jsonObject.has("channelStatusInfo") || jsonObject.has("channelStatus")) {
              Log.d("StatusQueryActivity", "推断为单个状态查询响应")
              processGetSingleStatusResponse(jsonObject)
            }
          } else {
            Log.d("StatusQueryActivity", "未知响应类型，跳过处理")
          }
        }
      }
    } catch (e: JsonSyntaxException) {
      Log.e("StatusQueryActivity", "JSON解析失败，原始数据: $json", e)
      // 尝试显示原始数据
      appendRawResponseData("JSON解析失败: $json")
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理响应时发生异常", e)
      appendRawResponseData("处理响应异常: ${e.message}")
    }
  }

  private fun processGetAllStatusResponse(jsonObject: JsonObject) {
    try {
      // 打印JSON结构用于调试
      Log.d("StatusQueryActivity", "全部门锁状态响应JSON: $jsonObject")

      // 使用正确的响应模型
      val allLocksResponse = gson.fromJson(jsonObject, AllLocksStatusResponse::class.java)

      if (allLocksResponse?.channelStatusList != null) {
        Log.d(
          "StatusQueryActivity",
          "成功解析全部状态响应，通道数量: ${allLocksResponse.channelStatusList.size}"
        )

        for (channelStatus in allLocksResponse.channelStatusList) {
          val lockId = channelStatus.channelNo
          if (lockId <= 7) {
            lockStatusMap[lockId] = channelStatus
            updateStatusCard(lockId, channelStatus)
            Log.d(
              "StatusQueryActivity",
              "更新锁 $lockId 状态: ${channelStatus.getLockStatusText()}"
            )
          }
        }
      } else {
        // 如果模型解析失败，尝试直接解析字段
        Log.w("StatusQueryActivity", "模型解析失败，尝试直接解析字段")
        val channelStatusElement = jsonObject.get("channelStatus")
        if (channelStatusElement != null) {
          parseAndUpdateAllStatus(channelStatusElement, "channelStatus")
        } else {
          // 尝试其他可能的字段名
          val alternativeFields = listOf("channelStatusInfo", "status", "data", "channels")
          for (field in alternativeFields) {
            val element = jsonObject.get(field)
            if (element != null) {
              Log.d("StatusQueryActivity", "全部状态查询使用字段名: $field")
              parseAndUpdateAllStatus(element, field)
              return
            }
          }
          Log.e("StatusQueryActivity", "全部状态查询响应中无法找到状态信息字段")
        }
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理全部门锁状态响应失败", e)
    }
  }

  private fun parseAndUpdateAllStatus(element: com.google.gson.JsonElement, fieldName: String) {
    try {
      val channelStatus = gson.fromJson(element.toString(), Array<ChannelStatus>::class.java)

      for (channelStatus in channelStatus) {
        val lockId = channelStatus.channelNo
        if (lockId <= 7) {
          lockStatusMap[lockId] = channelStatus
          updateStatusCard(lockId, channelStatus)
        }
      }
      Log.d("StatusQueryActivity", "全部状态查询成功解析 $fieldName 字段，更新状态")
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "全部状态查询解析字段 $fieldName 失败", e)
    }
  }

  private fun processGetSingleStatusResponse(jsonObject: JsonObject) {
    try {
      // 打印JSON结构用于调试
      Log.d("StatusQueryActivity", "单个门锁状态响应JSON: $jsonObject")

      // 使用正确的响应模型
      val lockStatusResponse = gson.fromJson(jsonObject, LockStatusResponse::class.java)

      if (lockStatusResponse?.channelStatusInfo != null) {
        val channelStatusInfo = lockStatusResponse.channelStatusInfo
        val lockId = channelStatusInfo.channelNo

        Log.d(
          "StatusQueryActivity",
          "成功解析单个状态响应，锁 $lockId 状态: ${channelStatusInfo.getLockStatusText()}"
        )

        if (lockId <= 7) {
          lockStatusMap[lockId] = channelStatusInfo
          updateStatusCard(lockId, channelStatusInfo)
        }
      } else {
        // 如果模型解析失败，尝试直接解析字段
        Log.w("StatusQueryActivity", "模型解析失败，尝试直接解析字段")
        val channelStatusElement = jsonObject.get("channelStatusInfo")
        if (channelStatusElement != null) {
          parseAndStatusUpdate(channelStatusElement, "channelStatusInfo")
        } else {
          // 尝试其他可能的字段名
          val alternativeFields = listOf("channelStatus", "status", "data")
          for (field in alternativeFields) {
            val element = jsonObject.get(field)
            if (element != null) {
              Log.d("StatusQueryActivity", "使用字段名: $field")
              parseAndStatusUpdate(element, field)
              return
            }
          }
          Log.e("StatusQueryActivity", "无法找到状态信息字段")
        }
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理单个门锁状态响应失败", e)
    }
  }

  private fun parseAndStatusUpdate(element: com.google.gson.JsonElement, fieldName: String) {
    try {
      val channelStatusInfo = gson.fromJson(element.toString(), ChannelStatus::class.java)
      val lockId = channelStatusInfo.channelNo

      if (lockId <= 7) {
        lockStatusMap[lockId] = channelStatusInfo
        updateStatusCard(lockId, channelStatusInfo)
        Log.d("StatusQueryActivity", "成功解析 $fieldName 字段，更新锁 $lockId 状态")
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "解析字段 $fieldName 失败", e)
    }
  }

  private fun processStatusUploadResponse(jsonObject: JsonObject) {
    try {
      // 打印JSON结构用于调试
      Log.d("StatusQueryActivity", "状态上传响应JSON: $jsonObject")

      // 使用正确的响应模型
      val statusUploadResponse = gson.fromJson(jsonObject, StatusUploadResponse::class.java)

      if (statusUploadResponse?.channelStatusInfo != null) {
        val channelStatusInfo = statusUploadResponse.channelStatusInfo
        val lockId = channelStatusInfo.channelNo

        Log.d(
          "StatusQueryActivity",
          "成功解析状态上传响应，锁 $lockId 状态变化: ${channelStatusInfo.getLockStatusText()}"
        )

        if (lockId <= 7) {
          lockStatusMap[lockId] = channelStatusInfo
          updateStatusCard(lockId, channelStatusInfo)

          // 显示状态变化通知
          val statusText = if (channelStatusInfo.isLocked) "关闭" else "打开"
          showToast("锁 $lockId 状态变化：$statusText")
        }
      } else {
        // 如果模型解析失败，尝试直接解析字段
        Log.w("StatusQueryActivity", "模型解析失败，尝试直接解析字段")
        val channelStatusElement = jsonObject.get("channelStatusInfo")
        if (channelStatusElement != null) {
          parseAndUpdateStatusUpload(channelStatusElement, "channelStatusInfo")
        } else {
          // 尝试其他可能的字段名
          val alternativeFields = listOf("channelStatus", "status", "data")
          for (field in alternativeFields) {
            val element = jsonObject.get(field)
            if (element != null) {
              Log.d("StatusQueryActivity", "状态上传使用字段名: $field")
              parseAndUpdateStatusUpload(element, field)
              return
            }
          }
          Log.e("StatusQueryActivity", "状态上传响应中无法找到状态信息字段")
        }
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "处理状态上传响应失败", e)
    }
  }

  private fun parseAndUpdateStatusUpload(element: com.google.gson.JsonElement, fieldName: String) {
    try {
      val channelStatusInfo = gson.fromJson(element.toString(), ChannelStatus::class.java)
      val lockId = channelStatusInfo.channelNo

      if (lockId <= 7) {
        lockStatusMap[lockId] = channelStatusInfo
        updateStatusCard(lockId, channelStatusInfo)

        // 显示状态变化通知
        val statusText = if (channelStatusInfo.isLocked) "关闭" else "打开"
        showToast("锁 $lockId 状态变化：$statusText")
        Log.d("StatusQueryActivity", "状态上传成功解析 $fieldName 字段，更新锁 $lockId 状态")
      }
    } catch (e: Exception) {
      Log.e("StatusQueryActivity", "状态上传解析字段 $fieldName 失败", e)
    }
  }

  private fun updateStatusCard(lockId: Int, channelStatus: ChannelStatus) {
    val cardIndex = lockId - 1
    if (cardIndex < gridStatusCards.childCount) {
      val card = gridStatusCards.getChildAt(cardIndex) as TextView

      // 使用模型中定义的状态文本方法
      val statusText = channelStatus.getLockStatusText()

      val color = when {
        channelStatus.isLocked -> resources.getColor(android.R.color.holo_red_light)
        !channelStatus.isLocked -> resources.getColor(android.R.color.holo_green_light)
        else -> resources.getColor(android.R.color.darker_gray)
      }

      card.text = "锁 $lockId: $statusText"
      card.setBackgroundColor(color)

      Log.d("StatusQueryActivity", "更新锁 $lockId 状态卡片: $statusText")
    }
  }

  private fun appendRawResponseData(data: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      .format(Date())
    val logEntry = "[$timestamp] $data\n"

    // 限制显示长度
    val currentText = tvRawResponseData.text.toString()
    val lines = currentText.split("\n")
    if (lines.size > 250) {
      val newText = StringBuilder()
      for (i in lines.size - 250 until lines.size) {
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