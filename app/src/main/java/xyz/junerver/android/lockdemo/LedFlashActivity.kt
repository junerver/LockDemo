package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedFlashActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil
  private lateinit var tvResponseData: TextView

  private val responseLog = StringBuilder()
  private val handler = Handler(Looper.getMainLooper())

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
  }

  private fun setupButtonListeners() {
    // 返回按钮
    findViewById<Button>(R.id.btnBack).setOnClickListener {
      finish()
    }

    // LED闪烁按钮
    findViewById<Button>(R.id.btnLED1).setOnClickListener {
      executeLEDFlash(1)
    }

    findViewById<Button>(R.id.btnLED2).setOnClickListener {
      executeLEDFlash(2)
    }

    findViewById<Button>(R.id.btnLED3).setOnClickListener {
      executeLEDFlash(3)
    }

    findViewById<Button>(R.id.btnLED4).setOnClickListener {
      executeLEDFlash(4)
    }

    findViewById<Button>(R.id.btnLED5).setOnClickListener {
      executeLEDFlash(5)
    }

    findViewById<Button>(R.id.btnLED6).setOnClickListener {
      executeLEDFlash(6)
    }

    findViewById<Button>(R.id.btnLED7).setOnClickListener {
      executeLEDFlash(7)
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

  private fun executeLEDFlash(ledId: Int) {
    val success = lockCtl.flashLockLed(ledId)
    if (success) {
      appendResponseData("LED $ledId 闪烁命令发送成功")
      showToast("LED $ledId 正在闪烁")
    } else {
      appendResponseData("LED $ledId 闪烁失败")
      showToast("LED $ledId 闪烁失败")
    }
  }

  private fun appendResponseData(data: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
      .format(Date())
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