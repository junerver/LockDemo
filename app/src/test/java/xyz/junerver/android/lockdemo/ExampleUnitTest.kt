package xyz.junerver.android.lockdemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 门锁控制板指令构造单元测试
 * 测试各种门锁控制指令的构造功能，确保生成的指令符合通讯协议规范
 * 按照Demo顺序排列测试用例
 */
class ExampleUnitTest {
  
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  /**
   * 测试1: 同时开多锁
   * Demo: 同时打开0号板的1、2、3号门
   * 上位机发送: 57 4B 4C 59 0C 00 80 03 01 02 03 86
   */
  @Test
  fun testOpenMultipleLocksSimultaneously() {
    val bytes = LockCtlBoardUtil.getInstance().buildOpenMultipleLocksCommand(0x00.toByte(), 1, 2, 3)
    assertNotNull("同时开多锁指令不应为null", bytes)
    assertEquals("同时开多锁指令不匹配", "57 4B 4C 59 0C 00 80 03 01 02 03 86", bytesToHex(bytes))
  }
  
  /**
   * 测试2: 通道闪烁
   * Demo: 0号板的1号通道闪烁
   * 上位机发送: 57 4B 4C 59 09 00 81 01 80
   */
  @Test
  fun testFlashChannel() {
    val bytes = LockCtlBoardUtil.getInstance().buildFlashChannelCommand(0x00.toByte(), 1)
    assertNotNull("通道闪烁指令不应为null", bytes)
    assertEquals("通道闪烁指令不匹配", "57 4B 4C 59 09 00 81 01 80", bytesToHex(bytes))
  }
  
  /**
   * 测试3: 开单个锁
   * Demo: 打开0号板的1号门
   * 上位机发送: 57 4B 4C 59 09 00 82 01 83
   */
  @Test
  fun testOpenSingleLock() {
    val bytes = LockCtlBoardUtil.getInstance().buildOpenSingleLockCommand(0x00.toByte(), 1)
    assertNotNull("开单锁指令不应为null", bytes)
    assertEquals("开单锁指令不匹配", "57 4B 4C 59 09 00 82 01 83", bytesToHex(bytes))
  }
  
  /**
   * 测试4: 查询单个门状态
   * Demo: 查询0号板的1号门状态
   * 上位机发送: 57 4B 4C 59 09 00 83 01 82
   */
  @Test
  fun testGetSingleLockStatus() {
    val bytes = LockCtlBoardUtil.getInstance().buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    assertNotNull("查询单门状态指令不应为null", bytes)
    assertEquals("查询单门状态指令不匹配", "57 4B 4C 59 09 00 83 01 82", bytesToHex(bytes))
  }
  
  /**
   * 测试5: 查询所有门状态
   * Demo: 查询0号板的全部门状态（以24路锁控板为例）
   * 上位机发送: 57 4B 4C 59 08 00 84 85
   */
  @Test
  fun testGetAllLocksStatus() {
    val bytes = LockCtlBoardUtil.getInstance().buildGetAllLocksStatusCommand(0x00.toByte())
    assertNotNull("查询所有门状态指令不应为null", bytes)
    assertEquals("查询所有门状态指令不匹配", "57 4B 4C 59 08 00 84 85", bytesToHex(bytes))
  }
  
  /**
   * 测试6: 开所有门
   * Demo: 打开0号板的全部门
   * 上位机发送: 57 4B 4C 59 08 00 86 87
   */
  @Test
  fun testOpenAllLocks() {
    val bytes = LockCtlBoardUtil.getInstance().buildOpenAllLocksCommand(0x00.toByte())
    assertNotNull("开所有门指令不应为null", bytes)
    assertEquals("开所有门指令不匹配", "57 4B 4C 59 08 00 86 87", bytesToHex(bytes))
  }
  
  /**
   * 测试7: 逐一开多门
   * Demo: 打开0号板的1、2、3号门
   * 上位机发送: 57 4B 4C 59 0C 00 87 03 01 02 03 81
   */
  @Test
  fun testOpenMultipleLocksSequentially() {
    val bytes = LockCtlBoardUtil.getInstance().buildOpenMultipleSequentialCommand(0x00.toByte(), 1, 2, 3)
    assertNotNull("逐一开多门指令不应为null", bytes)
    assertEquals("逐一开多门指令不匹配", "57 4B 4C 59 0C 00 87 03 01 02 03 81", bytesToHex(bytes))
  }
  
  /**
   * 测试不同板地址的指令构造
   */
  @Test
  fun testDifferentBoardAddresses() {
    val lockCtl = LockCtlBoardUtil.getInstance()
    
    // 测试1号板的开单锁指令
    val board1Command = lockCtl.buildOpenSingleLockCommand(0x01.toByte(), 1)
    assertNotNull("1号板开单锁指令不应为null", board1Command)
    assertEquals("1号板开单锁指令不匹配", "57 4B 4C 59 09 01 82 01 82", bytesToHex(board1Command))
    
    // 测试10号板的开单锁指令
    val board10Command = lockCtl.buildOpenSingleLockCommand(0x0A.toByte(), 1)
    assertNotNull("10号板开单锁指令不应为null", board10Command)
    assertEquals("10号板开单锁指令不匹配", "57 4B 4C 59 09 0A 82 01 89", bytesToHex(board10Command))
  }
  
  /**
   * 测试错误参数处理
   */
  @Test
  fun testErrorHandling() {
    val lockCtl = LockCtlBoardUtil.getInstance()
    
    // 测试无效门锁ID
    val invalidLockCommand = lockCtl.buildOpenSingleLockCommand(0x00.toByte(), -1)
    assertEquals("无效门锁ID应返回null", null, invalidLockCommand)
    
    // 测试空门锁ID数组
    val emptyLocksCommand = lockCtl.buildOpenMultipleLocksCommand(0x00.toByte())
    assertEquals("空门锁ID数组应返回null", null, emptyLocksCommand)
    
    // 测试null门锁ID数组
    val nullLocksCommand = lockCtl.buildOpenMultipleLocksCommand(0x00.toByte(), *intArrayOf())
    assertEquals("null门锁ID数组应返回null", null, nullLocksCommand)
  }
  
  /**
   * 测试响应数据验证功能
   */
  @Test
  fun testResponseValidation() {
    val lockCtl = LockCtlBoardUtil.getInstance()
    
    // 测试有效的响应数据
    val validResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x00, 0x01, 0x83.toByte())
    assertEquals("有效响应数据应通过验证", true, lockCtl.validateResponse(validResponse))
    assertEquals("状态字节应为成功", 0x00.toByte(), lockCtl.parseResponseStatus(validResponse))
    
    // 测试无效的响应数据（错误的校验字节）
    val invalidResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x01, 0x00, 0x01, 0x00)
    assertEquals("无效响应数据应验证失败", false, lockCtl.validateResponse(invalidResponse))
  }
  
  /**
   * 字节数组转十六进制字符串工具方法
   */
  private fun bytesToHex(bytes: ByteArray?): String {
    if (bytes == null) return "null"
    
    return bytes.joinToString(" ") { String.format("%02X", it) }
  }
}