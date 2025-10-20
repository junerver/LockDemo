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
 * 只保留指令构造测试和JSON响应解析测试
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

  // ==================== JSON解析测试 ====================

  /**
   * 测试JSON格式响应解析 - 单纯状态响应 (0x80)
   */
  @Test
  fun testParseOpenMultipleLocksJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 09 00 80 00 80")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("同时开多锁JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"open_multiple_locks\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
    assertTrue("JSON应包含成功消息", jsonResult.contains("成功"))
  }

  /**
   * 测试JSON格式响应解析 - 状态+通道响应 (0x81)
   */
  @Test
  fun testParseFlashChannelJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 0A 00 81 00 01 83")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("通道闪烁JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"flash_channel\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
    assertTrue("JSON应包含channelNo字段", jsonResult.contains("\"channelNo\": 1"))
  }

  /**
   * 测试JSON格式响应解析 - 状态+通道+锁状态响应 (0x82)
   */
  @Test
  fun testParseOpenSingleLockJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 0B 00 82 00 01 00 81")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("开单锁JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"open_single_lock\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
    assertTrue("JSON应包含channelNo字段", jsonResult.contains("\"channelNo\": 1"))
    assertTrue("JSON应包含channelStatus字段", jsonResult.contains("\"channelStatus\""))
    assertTrue("JSON应包含isLocked字段", jsonResult.contains("\"isLocked\": false")) // 0x00表示打开，所以isLocked为false
  }

  /**
   * 测试JSON格式响应解析 - 查询单个门锁状态 (0x83)
   */
  @Test
  fun testParseGetSingleLockStatusJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 0B 00 83 00 01 00 80")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("查询单个门锁状态JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"get_single_lock_status\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
    assertTrue("JSON应包含channelNo字段", jsonResult.contains("\"channelNo\": 1"))
    assertTrue("JSON应包含channelStatus字段", jsonResult.contains("\"channelStatus\""))
  }

  /**
   * 测试JSON格式响应解析 - 查询所有门锁状态 (0x84)
   */
  @Test
  fun testParseGetAllLocksStatusJsonResponse() {
    // 模拟4个通道的状态响应
    val response = hexToBytes("57 4B 4C 59 0E 00 84 00 04 00 01 01 00 87")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("查询所有门锁状态JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"get_all_locks_status\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
    assertTrue("JSON应包含channelCount字段", jsonResult.contains("\"channelCount\": 4"))
    assertTrue("JSON应包含channelStatus数组", jsonResult.contains("\"channelStatus\": ["))
  }

  /**
   * 测试JSON格式响应解析 - 状态上传 (0x85)
   */
  @Test
  fun testParseStatusUploadJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 0A 00 85 01 01 86")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("状态上传JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"status_upload\""))
    assertTrue("JSON应包含channelNo字段", jsonResult.contains("\"channelNo\": 1"))
    assertTrue("JSON应包含channelStatus字段", jsonResult.contains("\"channelStatus\""))
    assertTrue("JSON应包含isLocked字段", jsonResult.contains("\"isLocked\": true")) // 0x01表示关闭，所以isLocked为true
  }

  /**
   * 测试JSON格式响应解析 - 开全部锁 (0x86)
   */
  @Test
  fun testParseOpenAllLocksJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 09 00 86 00 86")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("开全部锁JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"open_all_locks\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
  }

  /**
   * 测试JSON格式响应解析 - 逐一开多锁 (0x87)
   */
  @Test
  fun testParseOpenMultipleSequentialJsonResponse() {
    val response = hexToBytes("57 4B 4C 59 09 00 87 00 87")
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(response)
    println("逐一开多锁JSON响应: $jsonResult")

    // 验证JSON包含必要字段
    assertTrue("JSON应包含commandType字段", jsonResult.contains("\"commandType\": \"open_multiple_sequential\""))
    assertTrue("JSON应包含status字段", jsonResult.contains("\"status\": 0"))
  }

  /**
   * 测试JSON格式响应解析 - 错误响应处理
   */
  @Test
  fun testParseInvalidJsonResponse() {
    // 测试无效响应数据
    val invalidResponse = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x05, 0x00, 0x80.toByte())
    val jsonResult = LockCtlBoardCmdHelper.parseResponseToJson(invalidResponse)
    println("无效响应JSON解析结果: $jsonResult")

    // 验证错误响应格式
    assertTrue("错误响应应包含error命令类型", jsonResult.contains("\"commandType\": \"error\""))
    assertTrue("错误响应应包含错误状态", jsonResult.contains("\"status\": -1"))
    assertTrue("错误响应应包含错误消息", jsonResult.contains("响应数据格式错误"))
  }

  /**
   * 测试JSON响应与字符串响应对比
   */
  @Test
  fun testDocDemo() {
    val j1 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 09 00 80 00 80"))
    println("1-JSON:     $j1")

    val j2 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 0A 00 81 00 01 83"))
    println("2-JSON:     $j2")

    val j3 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 0B 00 82 00 01 00 81"))
    println("3-JSON:     $j3")

    val j4 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 0B 00 83 00 01 00 80"))
    println("4-JSON:     $j4")

    val j5 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 22 00 84 00 18 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 B7"))
    println("5-JSON:     $j5")

    val j6 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 0A 00 85 01 01 86"))
    println("6-JSON:     $j6")

    val j7 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 09 00 86 00 86"))
    println("7-JSON:     $j7")

    val j8 = LockCtlBoardCmdHelper.parseResponseToJson(hexToBytes("57 4B 4C 59 09 00 87 00 87"))
    println("8-JSON:     $j8")
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

}