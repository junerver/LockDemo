package xyz.junerver.android.lockdemo.debounce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.lockctl.LockCtlBoardCmdHelper
import xyz.junerver.android.lockdemo.lockctl.debounce.ResponseMatcher

/**
 * ResponseMatcher 测试类
 * 测试响应匹配器的格式验证、指令匹配、状态解析等功能
 */
@RunWith(RobolectricTestRunner::class)
class ResponseMatcherTest {

  /**
   * 创建标准响应
   */
  private fun createResponse(
    boardAddress: Byte,
    commandByte: Byte,
    data: ByteArray
  ): ByteArray {
    val startBytes = byteArrayOf(0x57, 0x4B, 0x4C, 0x59)
    val dataLength = data.size
    val frameLength = startBytes.size + 1 + 1 + 1 + dataLength + 1

    val response = ByteArray(frameLength)

    // 起始符
    System.arraycopy(startBytes, 0, response, 0, startBytes.size)

    // 帧长度
    response[4] = (frameLength and 0xFF).toByte()

    // 板地址
    response[5] = boardAddress

    // 指令字
    response[6] = commandByte

    // 数据域
    if (data.isNotEmpty()) {
      System.arraycopy(data, 0, response, 7, data.size)
    }

    // 校验字节
    var checksum: Byte = 0
    for (i in 0 until frameLength - 1) {
      checksum = (checksum.toInt() xor response[i].toInt()).toByte()
    }
    response[frameLength - 1] = checksum

    return response
  }

  @Test
  fun testIsValidResponseFormat_Valid() {
    println("=== 测试有效的响应格式 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))

    println("响应长度: ${response.size}")
    println(
      "响应数据: ${
        response.joinToString(" ") {
          "0x${
            String.format(
              "%02X",
              it.toInt() and 0xFF
            )
          }"
        }
      }"
    )

    val isValid = ResponseMatcher.isValidResponseFormat(response)
    println("格式验证结果: $isValid")

    assertTrue("有效响应应该通过格式验证", isValid)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsValidResponseFormat_Null() {
    println("=== 测试 null 响应 ===")

    val isValid = ResponseMatcher.isValidResponseFormat(null)
    println("格式验证结果: $isValid")

    assertFalse("null 响应不应该通过格式验证", isValid)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsValidResponseFormat_TooShort() {
    println("=== 测试长度不足的响应 ===")

    val response = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x06, 0x00)

    println("响应长度: ${response.size}")
    val isValid = ResponseMatcher.isValidResponseFormat(response)
    println("格式验证结果: $isValid")

    assertFalse("长度不足的响应不应该通过格式验证", isValid)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsValidResponseFormat_InvalidStartBytes() {
    println("=== 测试起始符错误的响应 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))
    response[0] = 0xFF.toByte()  // 破坏起始符

    println(
      "起始符: ${
        response.slice(0..3).joinToString(" ") { "0x${String.format("%02X", it.toInt() and 0xFF)}" }
      }"
    )
    val isValid = ResponseMatcher.isValidResponseFormat(response)
    println("格式验证结果: $isValid")

    assertFalse("起始符错误的响应不应该通过格式验证", isValid)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsValidResponseFormat_InvalidFrameLength() {
    println("=== 测试帧长度错误的响应 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))
    response[4] = 0xFF.toByte()  // 破坏帧长度

    println("帧长度字段: 0x${String.format("%02X", response[4].toInt() and 0xFF)}")
    println("实际长度: ${response.size}")
    val isValid = ResponseMatcher.isValidResponseFormat(response)
    println("格式验证结果: $isValid")

    assertFalse("帧长度错误的响应不应该通过格式验证", isValid)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsResponseForCommand_Match() {
    println("=== 测试匹配的指令和响应 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))

    println("指令字: 0x${String.format("%02X", command[6].toInt() and 0xFF)}")
    println("板地址: 0x${String.format("%02X", command[5].toInt() and 0xFF)}")
    println("响应指令字: 0x${String.format("%02X", response[6].toInt() and 0xFF)}")
    println("响应板地址: 0x${String.format("%02X", response[5].toInt() and 0xFF)}")

    val isMatch = ResponseMatcher.isResponseForCommand(response, command)
    println("匹配结果: $isMatch")

    assertTrue("指令和响应应该匹配", isMatch)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsResponseForCommand_CommandByteMismatch() {
    println("=== 测试指令字不匹配 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val response = createResponse(0x00, 0x83.toByte(), byteArrayOf(0x00, 0x01, 0x00))  // 不同的指令字

    println("指令字: 0x${String.format("%02X", command[6].toInt() and 0xFF)}")
    println("响应指令字: 0x${String.format("%02X", response[6].toInt() and 0xFF)}")

    val isMatch = ResponseMatcher.isResponseForCommand(response, command)
    println("匹配结果: $isMatch")

    assertFalse("指令字不同时不应该匹配", isMatch)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsResponseForCommand_BoardAddressMismatch() {
    println("=== 测试板地址不匹配 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val response = createResponse(0x01, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))  // 不同的板地址

    println("板地址: 0x${String.format("%02X", command[5].toInt() and 0xFF)}")
    println("响应板地址: 0x${String.format("%02X", response[5].toInt() and 0xFF)}")

    val isMatch = ResponseMatcher.isResponseForCommand(response, command)
    println("匹配结果: $isMatch")

    assertFalse("板地址不同时不应该匹配", isMatch)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsResponseForCommand_NullResponse() {
    println("=== 测试 null 响应匹配 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)

    val isMatch = ResponseMatcher.isResponseForCommand(null, command)
    println("匹配结果: $isMatch")

    assertFalse("null 响应不应该匹配", isMatch)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsResponseForCommand_NullCommand() {
    println("=== 测试 null 指令匹配 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))

    val isMatch = ResponseMatcher.isResponseForCommand(response, null)
    println("匹配结果: $isMatch")

    assertFalse("null 指令不应该匹配", isMatch)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsSuccessResponse_Success() {
    println("=== 测试成功响应 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))

    println("状态字节: 0x${String.format("%02X", response[7].toInt() and 0xFF)}")

    val isSuccess = ResponseMatcher.isSuccessResponse(response)
    println("是否成功: $isSuccess")

    assertTrue("状态字节为 0x00 应该是成功", isSuccess)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsSuccessResponse_Failed() {
    println("=== 测试失败响应 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0xFF.toByte(), 0x01, 0x00))

    println("状态字节: 0x${String.format("%02X", response[7].toInt() and 0xFF)}")

    val isSuccess = ResponseMatcher.isSuccessResponse(response)
    println("是否成功: $isSuccess")

    assertFalse("状态字节为 0xFF 应该是失败", isSuccess)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsSuccessResponse_Null() {
    println("=== 测试 null 响应的成功判断 ===")

    val isSuccess = ResponseMatcher.isSuccessResponse(null)
    println("是否成功: $isSuccess")

    assertFalse("null 响应不应该是成功", isSuccess)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsSuccessResponse_TooShort() {
    println("=== 测试长度不足响应的成功判断 ===")

    val response = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x07, 0x00, 0x82.toByte())

    println("响应长度: ${response.size}")

    val isSuccess = ResponseMatcher.isSuccessResponse(response)
    println("是否成功: $isSuccess")

    assertFalse("长度不足的响应不应该是成功", isSuccess)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetResponseStatusDescription_Success() {
    println("=== 测试成功状态描述 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, 0x00))

    val description = ResponseMatcher.getResponseStatusDescription(response)
    println("状态描述: $description")

    assertEquals("成功状态描述应该是 '执行成功'", "执行成功", description)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetResponseStatusDescription_Failed() {
    println("=== 测试失败状态描述 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0xFF.toByte(), 0x01, 0x00))

    val description = ResponseMatcher.getResponseStatusDescription(response)
    println("状态描述: $description")

    assertEquals("失败状态描述应该是 '执行失败'", "执行失败", description)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetResponseStatusDescription_Unknown() {
    println("=== 测试未知状态描述 ===")

    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x88.toByte(), 0x01, 0x00))

    val description = ResponseMatcher.getResponseStatusDescription(response)
    println("状态描述: $description")

    assertTrue("未知状态描述应该包含 '未知状态'", description.contains("未知状态"))
    assertTrue("未知状态描述应该包含状态码", description.contains("0x88"))

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetChannelId_OpenSingleLock() {
    println("=== 测试开单个锁响应的通道ID提取 ===")

    val channelId = 5
    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, channelId.toByte(), 0x00))

    val extractedChannelId = ResponseMatcher.getChannelId(response)
    println("通道ID: $extractedChannelId")

    assertEquals("应该提取到通道ID $channelId", channelId, extractedChannelId)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetChannelId_QuerySingleStatus() {
    println("=== 测试查询单个门状态响应的通道ID提取 ===")

    val channelId = 3
    val response = createResponse(0x00, 0x83.toByte(), byteArrayOf(0x00, channelId.toByte(), 0x01))

    val extractedChannelId = ResponseMatcher.getChannelId(response)
    println("通道ID: $extractedChannelId")

    assertEquals("应该提取到通道ID $channelId", channelId, extractedChannelId)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetChannelId_NoChannelInfo() {
    println("=== 测试没有通道信息的响应 ===")

    val response = createResponse(0x00, 0x84.toByte(), byteArrayOf(0x00))  // 查询所有门状态

    val channelId = ResponseMatcher.getChannelId(response)
    println("通道ID: $channelId")

    assertEquals("没有通道信息时应该返回 -1", -1, channelId)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetLockStatus_OpenSingleLock() {
    println("=== 测试开单个锁响应的锁状态提取 ===")

    val lockStatus = 0x00  // 锁打开
    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x01, lockStatus.toByte()))

    val extractedStatus = ResponseMatcher.getLockStatus(response)
    println("锁状态: 0x${String.format("%02X", extractedStatus)}")

    assertEquals("应该提取到锁状态 0x00", lockStatus, extractedStatus)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetLockStatus_QuerySingleStatus() {
    println("=== 测试查询单个门状态响应的锁状态提取 ===")

    val lockStatus = 0x01  // 锁关闭
    val response = createResponse(0x00, 0x83.toByte(), byteArrayOf(0x00, 0x02, lockStatus.toByte()))

    val extractedStatus = ResponseMatcher.getLockStatus(response)
    println("锁状态: 0x${String.format("%02X", extractedStatus)}")

    assertEquals("应该提取到锁状态 0x01", lockStatus, extractedStatus)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetLockStatus_NoLockInfo() {
    println("=== 测试没有锁状态信息的响应 ===")

    val response = createResponse(0x00, 0x81.toByte(), byteArrayOf(0x00, 0x01))  // 通道闪烁

    val lockStatus = ResponseMatcher.getLockStatus(response)
    println("锁状态: $lockStatus")

    assertEquals("没有锁状态信息时应该返回 -1", -1, lockStatus)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetLockStatus_ResponseTooShort() {
    println("=== 测试长度不足的响应提取锁状态 ===")

    val response = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x08, 0x00, 0x82.toByte(), 0x00, 0x01)

    println("响应长度: ${response.size}")

    val lockStatus = ResponseMatcher.getLockStatus(response)
    println("锁状态: $lockStatus")

    assertEquals("长度不足时应该返回 -1", -1, lockStatus)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCompleteFlow_OpenLock() {
    println("=== 测试开锁完整流程 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 2)
    val response = createResponse(0x00, 0x82.toByte(), byteArrayOf(0x00, 0x02, 0x00))

    println("1. 验证响应格式")
    val isValid = ResponseMatcher.isValidResponseFormat(response)
    println("   格式有效: $isValid")
    assertTrue("响应格式应该有效", isValid)

    println("2. 匹配指令和响应")
    val isMatch = ResponseMatcher.isResponseForCommand(response, command)
    println("   指令匹配: $isMatch")
    assertTrue("指令应该匹配", isMatch)

    println("3. 检查执行状态")
    val isSuccess = ResponseMatcher.isSuccessResponse(response)
    println("   执行成功: $isSuccess")
    assertTrue("应该执行成功", isSuccess)

    println("4. 提取详细信息")
    val channelId = ResponseMatcher.getChannelId(response)
    val lockStatus = ResponseMatcher.getLockStatus(response)
    val statusDesc = ResponseMatcher.getResponseStatusDescription(response)
    println("   通道ID: $channelId")
    println("   锁状态: 0x${String.format("%02X", lockStatus)}")
    println("   状态描述: $statusDesc")

    assertEquals("通道ID应该是 2", 2, channelId)
    assertEquals("锁状态应该是 0x00 (打开)", 0x00, lockStatus)
    assertEquals("状态描述应该是 '执行成功'", "执行成功", statusDesc)

    println("✅ 完整流程测试通过\n")
  }
}
