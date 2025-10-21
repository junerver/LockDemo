package xyz.junerver.android.lockdemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var lockCtl: LockCtlBoardUtil
    private lateinit var tvSerialStatus: TextView
    private lateinit var tvResponseData: TextView
    private lateinit var btnConnectSerial: Button

    private var isSerialConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private val responseLog = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化门锁控制板工具类
        lockCtl = LockCtlBoardUtil.getInstance()

        // 初始化UI组件
        initViews()

        // 设置串口数据监听器
        setupSerialListener()

        // 设置按钮点击事件
        setupButtonListeners()

        // 自动连接串口
        autoConnectSerial()
    }

    private fun initViews() {
        tvSerialStatus = findViewById(R.id.tvSerialStatus)
        tvResponseData = findViewById(R.id.tvResponseData)
        btnConnectSerial = findViewById(R.id.btnConnectSerial)
    }

    private fun setupSerialListener() {
        lockCtl.setOnDataReceived(object : LockCtlBoardUtil.OnDataReceived {
            override fun onDataReceived(json: String) {
                // 在主线程更新UI
                handler.post {
                    appendResponseData(json)
                }
            }
        })
    }

    private fun setupButtonListeners() {
        // 串口连接按钮
        btnConnectSerial.setOnClickListener {
            if (isSerialConnected) {
                disconnectSerial()
            } else {
                connectSerial()
            }
        }

        // 开启全部锁按钮
        findViewById<Button>(R.id.btnOpenAllLocks).setOnClickListener {
            if (checkSerialConnection()) {
                val success = lockCtl.openAllLocksSequentially()
                if (success) {
                    showToast("正在开启全部锁...")
                } else {
                    showToast("开启全部锁失败")
                }
            }
        }

        // 查询所有状态按钮
        findViewById<Button>(R.id.btnQueryAllStatus).setOnClickListener {
            if (checkSerialConnection()) {
                val success = lockCtl.getAllLocksStatus()
                if (success) {
                    showToast("正在查询所有锁状态...")
                } else {
                    showToast("查询状态失败")
                }
            }
        }

        // 依次开锁按钮
        findViewById<Button>(R.id.btnSequentialOpen).setOnClickListener {
            val intent = Intent(this, SequentialOpenActivity::class.java)
            startActivity(intent)
        }

        // LED闪烁按钮
        findViewById<Button>(R.id.btnLEDFlash).setOnClickListener {
            val intent = Intent(this, LedFlashActivity::class.java)
            startActivity(intent)
        }

        // 单个锁操作按钮
        setupSingleLockButtons()

        // 持续打开按钮
        findViewById<Button>(R.id.btnKeepOpen).setOnClickListener {
            showChannelDialog("持续打开", true)
        }

        // 关闭通道按钮
        findViewById<Button>(R.id.btnCloseChannel).setOnClickListener {
            showChannelDialog("关闭通道", false)
        }
    }

    private fun setupSingleLockButtons() {
        for (i in 1..7) {
            val buttonId = resources.getIdentifier("btnLock$i", "id", packageName)
            val button = findViewById<Button>(buttonId)
            button.setOnClickListener {
                if (checkSerialConnection()) {
                    val success = lockCtl.openSingleLock(i)
                    if (success) {
                        showToast("正在开启锁 $i...")
                    } else {
                        showToast("开启锁 $i 失败")
                    }
                }
            }
        }
    }

    private fun showChannelDialog(title: String, isKeepOpen: Boolean) {
        // 创建一个简单的通道选择对话框
        val channels = arrayOf("通道 1", "通道 2", "通道 3", "通道 4", "通道 5", "通道 6", "通道 7")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("选择 $title 通道")

        val checkedItems = BooleanArray(7)
        builder.setMultiChoiceItems(channels, null) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }

        builder.setPositiveButton("确定") { _, _ ->
            val selectedChannels = mutableListOf<Int>()
            for (i in checkedItems.indices) {
                if (checkedItems[i]) {
                    selectedChannels.add(i + 1)
                }
            }

            if (selectedChannels.isNotEmpty()) {
                executeChannelOperation(selectedChannels.toIntArray(), isKeepOpen)
            } else {
                showToast("请选择至少一个通道")
            }
        }

        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun executeChannelOperation(channels: IntArray, isKeepOpen: Boolean) {
        if (!checkSerialConnection()) return

        // 这里可以实现批量通道操作
        // 目前我们只处理单个通道
        val channelId = channels[0]
        val success = if (isKeepOpen) {
            // 这里需要添加通道持续打开的方法到LockCtlBoardUtil
            showToast("通道 $channelId 持续打开功能需要实现")
            false
        } else {
            // 这里需要添加通道关闭的方法到LockCtlBoardUtil
            showToast("通道 $channelId 关闭功能需要实现")
            false
        }

        if (!success) {
            showToast("操作失败")
        }
    }

    private fun connectSerial() {
        try {
            lockCtl.openSerialPort()
            // 模拟连接成功（实际连接状态会在监听器中更新）
            handler.postDelayed({
                updateSerialStatus(true)
                showToast("串口连接成功")
            }, 1000)
        } catch (e: Exception) {
            Log.e("MainActivity", "串口连接失败", e)
            showToast("串口连接失败: ${e.message}")
        }
    }

    private fun disconnectSerial() {
        lockCtl.closeSerialPort()
        updateSerialStatus(false)
        showToast("串口已断开")
    }

    private fun updateSerialStatus(connected: Boolean) {
        isSerialConnected = connected
        if (connected) {
            tvSerialStatus.text = "已连接"
            tvSerialStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            btnConnectSerial.text = "断开"
        } else {
            tvSerialStatus.text = "未连接"
            tvSerialStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            btnConnectSerial.text = "连接"
        }
    }

    private fun checkSerialConnection(): Boolean {
        if (!isSerialConnected) {
            showToast("请先连接串口")
            return false
        }
        return true
    }

    private fun appendResponseData(data: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())
        responseLog.append("[$timestamp] $data\n")

        // 限制日志长度，保留最近100行
        val lines = responseLog.toString().split("\n")
        if (lines.size > 100) {
            responseLog.clear()
            for (i in lines.size - 100 until lines.size) {
                responseLog.append(lines[i]).append("\n")
            }
        }

        tvResponseData.text = responseLog.toString()
        // 滚动到底部
        val scrollView = tvResponseData.parent as? android.widget.ScrollView
        scrollView?.post {
            scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 自动连接串口
     */
    private fun autoConnectSerial() {
        // 检查串口是否已经连接
        if (lockCtl.isSerialPortOpen) {
            Log.d("MainActivity", "串口已连接，跳过自动连接")
            updateSerialStatus(true)
            showToast("串口已连接")
            return
        }

        Log.d("MainActivity", "开始自动连接串口...")
        appendResponseData("正在自动连接串口...")

        // 延迟一小段时间再连接，确保UI完全初始化
        handler.postDelayed({
            try {
                lockCtl.openSerialPort()
                // 连接成功后的状态更新（实际连接状态会在监听器中更新）
                handler.postDelayed({
                    updateSerialStatus(true)
                    appendResponseData("串口自动连接成功")
                    showToast("串口自动连接成功")
                }, 1000)
            } catch (e: Exception) {
                Log.e("MainActivity", "串口自动连接失败", e)
                appendResponseData("串口自动连接失败: ${e.message}")
                showToast("串口自动连接失败: ${e.message}")
            }
        }, 500) // 延迟500ms连接，确保界面完全加载
    }

    override fun onDestroy() {
        super.onDestroy()
        // 断开串口连接
        if (isSerialConnected) {
            disconnectSerial()
        }
    }
}