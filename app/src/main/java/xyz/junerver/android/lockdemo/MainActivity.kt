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
  }
  
  /**
   * 测试门锁指令构造功能
   */
  private fun testLockCommands() {
    Log.d("MainActivity", "=== 开始测试门锁指令构造 ===")
    
    val boardAddress: Byte = 0x00 // 0号板
    
    // 示例1: 构造1号通道闪烁
    val flashCommand = lockCtl.buildFlashChannelCommand(boardAddress, 1)
    Log.d("MainActivity", "1号通道闪烁指令: ${bytesToHex(flashCommand)}")
    
    // 示例2: 构造开启1号锁指令
    val openSingleCommand = lockCtl.buildOpenSingleLockCommand(boardAddress, 1)
    Log.d("MainActivity", "开启1号锁指令: ${bytesToHex(openSingleCommand)}")
    
    // 示例3: 构造同时开启1,2,3号锁指令
    val openMultipleCommand = lockCtl.buildOpenMultipleLocksCommand(boardAddress, 1, 2, 3)
    Log.d("MainActivity", "同时开启1,2,3号锁指令: ${bytesToHex(openMultipleCommand)}")
    
    // 示例4: 构造查询所有门锁状态指令
    val getAllStatusCommand = lockCtl.buildGetAllLocksStatusCommand(boardAddress)
    Log.d("MainActivity", "查询所有门锁状态指令: ${bytesToHex(getAllStatusCommand)}")
    
    // 测试响应解析功能
    testResponseParsing()
    
    Log.d("MainActivity", "=== 门锁指令构造测试完成 ===")
  }
  
  /**
   * 测试响应数据解析功能
   */
  private fun testResponseParsing() {
    Log.d("MainActivity", "=== 测试响应解析 ===")
    
    // 模拟1号通道闪烁的成功响应数据
    // 57 4B 4C 59 0A 00 81 00 01 83
    val successResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x00, 0x01, 0x83.toByte())
    
    val isValid = lockCtl.validateResponse(successResponse)
    Log.d("MainActivity", "响应验证结果: ${if (isValid) "通过" else "失败"}")
    
    if (isValid) {
      val status = lockCtl.parseResponseStatus(successResponse)
      Log.d("MainActivity", "操作状态: ${if (status == 0x00.toByte()) "成功" else "失败"}")
    }
  }
  
  /**
   * 字节数组转十六进制字符串
   */
  private fun bytesToHex(bytes: ByteArray?): String {
    if (bytes == null) return "null"
    
    return bytes.joinToString(" ") { String.format("%02X", it) }
  }
}