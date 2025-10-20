package xyz.junerver.android.lockdemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 门锁控制板指令构造单元测试
 * 测试各种门锁控制指令的构造功能，确保生成的指令符合通讯协议规范
 * 按照Demo顺序排列测试用例
 * 注意：现在使用 LockCtlBoardCmdHelper 工具类进行指令构造
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
    val bytes = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(0x00.toByte(), 1, 2, 3)
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
    val bytes = LockCtlBoardCmdHelper.buildFlashChannelCommand(0x00.toByte(), 1)
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
    val bytes = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
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
    val bytes = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
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
    val bytes = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand(0x00.toByte())
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
    val bytes = LockCtlBoardCmdHelper.buildOpenAllLocksCommand(0x00.toByte())
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
    val bytes = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand(0x00.toByte(), 1, 2, 3)
    assertNotNull("逐一开多门指令不应为null", bytes)
    assertEquals("逐一开多门指令不匹配", "57 4B 4C 59 0C 00 87 03 01 02 03 81", bytesToHex(bytes))
  }
  
  /**
   * 测试不同板地址的指令构造
   */
  @Test
  fun testDifferentBoardAddresses() {
    // 测试1号板的开单锁指令
    val board1Command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x01.toByte(), 1)
    assertNotNull("1号板开单锁指令不应为null", board1Command)
    assertEquals("1号板开单锁指令不匹配", "57 4B 4C 59 09 01 82 01 82", bytesToHex(board1Command))
    
    // 测试10号板的开单锁指令
    val board10Command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x0A.toByte(), 1)
    assertNotNull("10号板开单锁指令不应为null", board10Command)
    assertEquals("10号板开单锁指令不匹配", "57 4B 4C 59 09 0A 82 01 89", bytesToHex(board10Command))
  }
  
  /**
   * 测试错误参数处理
   */
  @Test
  fun testErrorHandling() {
    // 测试无效门锁ID
    val invalidLockCommand = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), -1)
    assertEquals("无效门锁ID应返回null", null, invalidLockCommand)
    
    // 测试空门锁ID数组
    val emptyLocksCommand = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(0x00.toByte())
    assertEquals("空门锁ID数组应返回null", null, emptyLocksCommand)
    
    // 测试null门锁ID数组
    val nullLocksCommand = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(0x00.toByte(), *intArrayOf())
    assertEquals("null门锁ID数组应返回null", null, nullLocksCommand)
  }
  
  /**
   * 测试响应数据验证功能
   */
  @Test
  fun testResponseValidation() {
    // 测试有效的响应数据
    val validResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x00, 0x01, 0x83.toByte())
    assertEquals("有效响应数据应通过验证", true, LockCtlBoardCmdHelper.validateResponse(validResponse))
    assertEquals("状态字节应为成功", 0x00.toByte(), LockCtlBoardCmdHelper.parseResponseStatus(validResponse))
    
    // 测试无效的响应数据（错误的校验字节）
    val invalidResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x01, 0x00, 0x01, 0x00)
    assertEquals("无效响应数据应验证失败", false, LockCtlBoardCmdHelper.validateResponse(invalidResponse))
  }
  
  /**
   * 测试通用指令构造方法
   */
  @Test
  fun testBuildCommand() {
    // 测试构造自定义指令
    val customData = byteArrayOf(0x01, 0x02, 0x03)
    val customCommand = LockCtlBoardCmdHelper.buildCommand(0x01.toByte(), 0x80.toByte(), customData)
    assertNotNull("自定义指令不应为null", customCommand)
    
    // 验证指令格式
    assertEquals("指令长度应正确", 11, customCommand.size)
    assertEquals("起始符应正确", "57 4B 4C 59", bytesToHex(customCommand.copyOfRange(0, 4)))
    assertEquals("板地址应正确", 0x01.toByte(), customCommand[5])
    assertEquals("指令字应正确", 0x80.toByte(), customCommand[6])
  }
  
  /**
   * 测试单字节响应解析 (0x80, 0x86, 0x87)
   */
  @Test
  fun testParseSingleByteResponses() {
    // 测试同时开多锁响应 (0x80): 57 4B 4C 59 09 00 80 00 80
    val openMultipleResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x80.toByte(), 0x00, 0x80.toByte())
    val result1 = LockCtlBoardCmdHelper.parseResponse(openMultipleResponse)
    assertEquals("同时开多锁成功响应解析正确", "同时开多锁操作状态: 成功 (0x00)", result1)
    
    // 测试开全部锁响应 (0x86): 57 4B 4C 59 08 00 86 00 86
    val openAllResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x86.toByte(), 0x00, 0x86.toByte())
    val result2 = LockCtlBoardCmdHelper.parseResponse(openAllResponse)
    assertEquals("开全部锁成功响应解析正确", "开全部锁操作状态: 成功 (0x00)", result2)
    
    // 测试逐一开多锁响应 (0x87): 57 4B 4C 59 09 00 87 00 87
    val openSequentialResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x87.toByte(), 0x00, 0x87.toByte())
    val result3 = LockCtlBoardCmdHelper.parseResponse(openSequentialResponse)
    assertEquals("逐一开多锁成功响应解析正确", "逐一开多锁操作状态: 成功 (0x00)", result3)
  }
  
  /**
   * 测试双字节响应解析 (0x81, 0x88, 0x89)
   */
  @Test
  fun testParseTwoByteResponses() {
    // 测试通道闪烁响应 (0x81): 57 4B 4C 59 0A 00 81 00 01 83
    val flashResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x81.toByte(), 0x00, 0x01, 0x83.toByte())
    val result1 = LockCtlBoardCmdHelper.parseResponse(flashResponse)
    assertEquals("通道闪烁成功响应解析正确", "通道闪烁操作 - 通道1: 成功 (状态: 0x00)", result1)
    
    // 测试通道持续打开响应 (0x88): 57 4B 4C 59 0A 00 88 00 02 88
    val keepOpenResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x88.toByte(), 0x00, 0x02, 0x89.toByte())
    val result2 = LockCtlBoardCmdHelper.parseResponse(keepOpenResponse)
    assertEquals("通道持续打开成功响应解析正确", "通道持续打开操作 - 通道2: 成功 (状态: 0x00)", result2)
    
    // 测试通道关闭响应 (0x89): 57 4B 4C 59 0A 00 89 00 03 89
    val closeResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x89.toByte(), 0x00, 0x03, 0x89.toByte())
    val result3 = LockCtlBoardCmdHelper.parseResponse(closeResponse)
    assertEquals("通道关闭成功响应解析正确", "通道关闭操作 - 通道3: 成功 (状态: 0x00)", result3)
  }
  
  /**
   * 测试三字节响应解析 (0x82, 0x83)
   */
  @Test
  fun testParseThreeByteResponses() {
    // 测试开单锁响应 (0x82): 57 4B 4C 59 0B 00 82 00 05 01 84
    val openSingleResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0B, 0x00, 0x82.toByte(), 0x00, 0x05, 0x01, 0x84.toByte())
    val result1 = LockCtlBoardCmdHelper.parseResponse(openSingleResponse)
    assertEquals("开单锁成功响应解析正确", "开单锁操作 - 通道5: 成功, 锁状态: 关闭 (状态: 0x00, 锁状态: 0x01)", result1)
    
    // 测试查询单个门锁状态响应 (0x83): 57 4B 4C 59 0B 00 83 00 07 00 85
    val getSingleStatusResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0B, 0x00, 0x83.toByte(), 0x00, 0x07, 0x00, 0x86.toByte())
    val result2 = LockCtlBoardCmdHelper.parseResponse(getSingleStatusResponse)
    assertEquals("查询单个门锁状态响应解析正确", "查询单个门锁状态 - 通道7: 成功, 锁状态: 打开 (状态: 0x00, 锁状态: 0x00)", result2)
  }
  
  /**
   * 测试状态上传响应解析 (0x85)
   */
  @Test
  fun testParseStatusUploadResponse() {
    // 测试状态上传响应 (0x85): 57 4B 4C 59 0A 00 85 02 00 86
    // 根据协议修正：状态字节 0x00=成功，锁状态 0x00=门打开
    val statusUploadResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0A, 0x00, 0x85.toByte(), 0x02, 0x00, 0x84.toByte())
    val result = LockCtlBoardCmdHelper.parseResponse(statusUploadResponse)
    assertEquals("状态上传响应解析正确", "状态上传 - 通道2状态变化: 打开 (通道: 0x02, 状态: 0x00)", result)
  }
  
  /**
   * 测试查询所有门锁状态响应解析 (0x84)
   */
  @Test
  fun testParseGetAllStatusResponse() {
    // 测试查询所有门锁状态响应 (0x84): 57 4B 4C 59 0E 00 84 00 04 00 01 01 00 87
    // 根据协议修正：0x00=门打开，0x01=门关闭
    // 状态字节：0x00=成功，0x01=失败
    // 锁状态：0x00=打开，0x01=关闭，0xFF=失败
    val getAllStatusResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x0E, 0x00, 0x84.toByte(), 0x00, 0x04, 0x00, 0x01, 0x01, 0x00, 0x87.toByte())
    val result = LockCtlBoardCmdHelper.parseResponse(getAllStatusResponse)
    // 验证解析结果包含所有通道信息
    assertTrue("解析结果应包含通道总数", result.contains("通道总数: 4"))
    // 根据修正后的协议：0x00=门打开，0x01=门关闭
    assertTrue("解析结果应包含通道1状态为打开", result.contains("通道1: 打开"))
    assertTrue("解析结果应包含通道2状态为关闭", result.contains("通道2: 关闭"))
    assertTrue("解析结果应包含通道3状态为关闭", result.contains("通道3: 关闭"))
    assertTrue("解析结果应包含通道4状态为打开", result.contains("通道4: 打开"))
  }
  
  /**
   * 测试失败响应解析
   */
  @Test
  fun testParseFailureResponses() {
    // 测试同时开多锁失败响应 (0x80): 57 4B 4C 59 09 00 80 FF 7F
    val failureResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x80.toByte(), 0xFF.toByte(), 0x7F.toByte())
    val result = LockCtlBoardCmdHelper.parseResponse(failureResponse)
    assertEquals("失败响应解析正确", "同时开多锁操作状态: 失败 (0xFF)", result)
  }
  
  /**
   * 测试无效响应解析
   */
  @Test
  fun testParseInvalidResponses() {
    // 测试长度不足的响应
    val shortResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x05, 0x00, 0x80.toByte())
    val result1 = LockCtlBoardCmdHelper.parseResponse(shortResponse)
    assertEquals("短响应应返回错误信息", "响应数据格式错误", result1)
    
    // 测试错误校验的响应
    val invalidChecksumResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x80.toByte(), 0x00, 0x00.toByte())
    val result2 = LockCtlBoardCmdHelper.parseResponse(invalidChecksumResponse)
    assertEquals("校验错误响应应返回错误信息", "响应数据格式错误", result2)
    
    // 测试未知指令字的响应
    val unknownCommandResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x09, 0x00, 0x99.toByte(), 0x00, 0x99.toByte())
    val result3 = LockCtlBoardCmdHelper.parseResponse(unknownCommandResponse)
    assertEquals("未知指令字响应应返回提示信息", "未知指令字: 0x99", result3)
  }
  
  /**
   * 字节数组转十六进制字符串工具方法
   */
  private fun bytesToHex(bytes: ByteArray?): String {
    if (bytes == null) return "null"
    
    return bytes.joinToString(" ") { String.format("%02X", it) }
  }

  // 16进制转成字节数组，忽略空格、大小写
  private fun hexToBytes(hex: String): ByteArray {
    // 移除所有空格并转为大写（便于统一处理）
    val cleanHex = hex.replace("\\s+".toRegex(), "").uppercase()

    // 检查长度是否为偶数
    require(cleanHex.length % 2 == 0) { "Invalid hex string length: must be even" }

    // 每两个字符解析为一个字节
    return ByteArray(cleanHex.length / 2) { i ->
      cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
  }

  @Test
  fun testDocDemo() {
    val r1 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 09 00 80 00 80"))
    println("1:  $r1")
    val r2 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 0A 00 81 00 01 83"))
    println("2:  $r2")
    val r3 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 0B 00 82 00 01 00 81"))
    println("3:  $r3")
    val r4 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 0B 00 83 00 01 00 80"))
    println("4:  $r4")
    val r5 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 22 00 84 00 18 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 B7"))
    println("5:  $r5")
    val r6 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 0A 00 85 01 01 86"))
    println("6:  $r6")
    val r7 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 09 00 86 00 86"))
    println("7:  $r7")
    val r8 = LockCtlBoardCmdHelper.parseResponse(hexToBytes("57 4B 4C 59 09 00 87 00 87"))
    println("8:  $r8")
  }

}