package xyz.junerver.android.lockdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
  private lateinit var lockCtl: LockCtlBoardUtil

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

    // 测试指令构造功能
    testLockCommands()

    // 测试Helper类静态方法
    testHelperCommands()

    // 测试JSON响应解析功能
    testJsonResponseParsing()
  }

  /**
   * 测试门锁指令构造功能
   */
  private fun testLockCommands() {
    Log.d("MainActivity", "=== 开始测试门锁指令构造 ===")

    val boardAddress: Byte = 0x00 // 0号板

    // 示例1: 构造1号通道闪烁
    val flashCommand = LockCtlBoardCmdHelper.buildFlashChannelCommand(boardAddress, 1)
    Log.d("MainActivity", "1号通道闪烁指令: ${bytesToHex(flashCommand)}")

    // 示例2: 构造开启1号锁指令
    val openSingleCommand = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(boardAddress, 1)
    Log.d("MainActivity", "开启1号锁指令: ${bytesToHex(openSingleCommand)}")

    // 示例3: 构造同时开启1,2,3号锁指令
    val openMultipleCommand = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(boardAddress, 1, 2, 3)
    Log.d("MainActivity", "同时开启1,2,3号锁指令: ${bytesToHex(openMultipleCommand)}")

    // 示例4: 构造查询所有门锁状态指令
    val getAllStatusCommand = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand(boardAddress)
    Log.d("MainActivity", "查询所有门锁状态指令: ${bytesToHex(getAllStatusCommand)}")

    Log.d("MainActivity", "=== 门锁指令构造测试完成 ===")
  }

  /**
   * 测试LockCtlBoardCmdHelper静态方法
   */
  private fun testHelperCommands() {
    Log.d("MainActivity", "=== 测试Helper类静态方法 ===")

    val boardAddress: Byte = 0x00 // 0号板

    // 示例1: 使用Helper类构造通道闪烁指令
    val flashCommand = LockCtlBoardCmdHelper.buildFlashChannelCommand(boardAddress, 1)
    Log.d("MainActivity", "Helper类构造1号通道闪烁指令: ${bytesToHex(flashCommand)}")

    // 示例2: 使用Helper类构造开单锁指令
    val openSingleCommand = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(boardAddress, 1)
    Log.d("MainActivity", "Helper类构造开1号锁指令: ${bytesToHex(openSingleCommand)}")

    // 示例3: 使用Helper类构造同时开多锁指令
    val openMultipleCommand = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(boardAddress, 1, 2, 3)
    Log.d("MainActivity", "Helper类构造同时开1,2,3号锁指令: ${bytesToHex(openMultipleCommand)}")

    Log.d("MainActivity", "=== Helper类静态方法测试完成 ===")
  }

  /**
   * 测试JSON响应解析功能
   */
  private fun testJsonResponseParsing() {
    Log.d("MainActivity", "=== 开始测试JSON响应解析 ===")

    // 测试数据：同时开多锁成功响应
    val openMultipleResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x80.toByte(), 0x00, 0x80.toByte())
    val jsonResult1 = LockCtlBoardCmdHelper.parseResponseToJson(openMultipleResponse)
    Log.d("MainActivity", "同时开多锁JSON响应: $jsonResult1")

    // 测试数据：通道闪烁成功响应
    val flashResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x00, 0x01, 0x83.toByte())
    val jsonResult2 = LockCtlBoardCmdHelper.parseResponseToJson(flashResponse)
    Log.d("MainActivity", "通道闪烁JSON响应: $jsonResult2")

    // 测试数据：开单锁成功响应
    val openSingleResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0B, 0x00, 0x82.toByte(), 0x00, 0x01, 0x00, 0x81.toByte())
    val jsonResult3 = LockCtlBoardCmdHelper.parseResponseToJson(openSingleResponse)
    Log.d("MainActivity", "开单锁JSON响应: $jsonResult3")

    // 测试数据：查询所有门锁状态响应（4个通道）
    val getAllStatusResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0E, 0x00, 0x84.toByte(), 0x00, 0x04, 0x00, 0x01, 0x01, 0x00, 0x87.toByte())
    val jsonResult4 = LockCtlBoardCmdHelper.parseResponseToJson(getAllStatusResponse)
    Log.d("MainActivity", "查询所有门锁状态JSON响应: $jsonResult4")

    // 测试数据：状态上传响应
    val statusUploadResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x85.toByte(), 0x01, 0x01, 0x86.toByte())
    val jsonResult5 = LockCtlBoardCmdHelper.parseResponseToJson(statusUploadResponse)
    Log.d("MainActivity", "状态上传JSON响应: $jsonResult5")

    // 测试错误响应处理
    val invalidResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x05, 0x00, 0x80.toByte())
    val jsonResult6 = LockCtlBoardCmdHelper.parseResponseToJson(invalidResponse)
    Log.d("MainActivity", "无效响应JSON解析结果: $jsonResult6")

    Log.d("MainActivity", "=== JSON响应解析测试完成 ===")
  }

  /**
   * 字节数组转十六进制字符串
   */
  private fun bytesToHex(bytes: ByteArray?): String {
    if (bytes == null) return "null"

    return bytes.joinToString(" ") { String.format("%02X", it) }
  }
}