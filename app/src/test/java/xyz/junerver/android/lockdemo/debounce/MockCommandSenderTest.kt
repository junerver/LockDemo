package xyz.junerver.android.lockdemo.debounce

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.LockCtlBoardCmdHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MockCommandSender 测试类
 * 测试 Mock 发送器的连接管理、异步响应、错误模拟、记录功能等
 */
@RunWith(RobolectricTestRunner::class)
class MockCommandSenderTest {

  private var mockSender: MockCommandSender? = null

  @After
  fun tearDown() {
    println("=== 清理测试资源 ===")
    mockSender?.shutdown()
    mockSender = null
    println("✅ 资源清理完成\n")
  }

  @Test
  fun testCreate_AutoConnect() {
    println("=== 测试自动连接创建 ===")

    mockSender = MockCommandSender(true)

    println("连接状态: ${mockSender!!.isConnected}")
    assertTrue("自动连接时应该已连接", mockSender!!.isConnected)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCreate_NoAutoConnect() {
    println("=== 测试不自动连接创建 ===")

    mockSender = MockCommandSender(false)

    println("连接状态: ${mockSender!!.isConnected}")
    assertFalse("不自动连接时应该未连接", mockSender!!.isConnected)

    println("✅ 测试通过\n")
  }

  @Test
  fun testConnect() {
    println("=== 测试手动连接 ===")

    mockSender = MockCommandSender(false)
    println("初始连接状态: ${mockSender!!.isConnected}")
    assertFalse("初始应该未连接", mockSender!!.isConnected)

    mockSender!!.connect()
    println("连接后状态: ${mockSender!!.isConnected}")
    assertTrue("连接后应该已连接", mockSender!!.isConnected)

    println("✅ 测试通过\n")
  }

  @Test
  fun testDisconnect() {
    println("=== 测试断开连接 ===")

    mockSender = MockCommandSender(true)
    println("初始连接状态: ${mockSender!!.isConnected}")
    assertTrue("初始应该已连接", mockSender!!.isConnected)

    mockSender!!.disconnect()
    println("断开后状态: ${mockSender!!.isConnected}")
    assertFalse("断开后应该未连接", mockSender!!.isConnected)

    println("✅ 测试通过\n")
  }

  @Test
  fun testSendCommand_WhenNotConnected() {
    println("=== 测试未连接时发送指令 ===")

    mockSender = MockCommandSender(false)
    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)

    val latch = CountDownLatch(1)
    var errorReceived = false
    var errorMessage: String? = null

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(response: ByteArray?) {
        // 不应该收到响应
      }

      override fun onError(error: String?) {
        println("收到错误: $error")
        errorReceived = true
        errorMessage = error
        latch.countDown()
      }
    })

    mockSender!!.sendCommand(command)

    val completed = latch.await(2, TimeUnit.SECONDS)
    println("等待结果: completed=$completed")
    println("错误消息: $errorMessage")

    assertTrue("应该收到错误回调", errorReceived)
    assertNotNull("错误消息不应该为 null", errorMessage)
    assertTrue("错误消息应该包含 '未连接'", errorMessage!!.contains("未连接"))

    println("✅ 测试通过\n")
  }

  @Test
  fun testSendCommand_AsyncResponse() {
    println("=== 测试异步响应机制 ===")

    mockSender = MockCommandSender(true)
    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)

    val latch = CountDownLatch(1)
    var responseReceived = false
    var response: ByteArray? = null

    val startTime = System.currentTimeMillis()

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(res: ByteArray?) {
        val elapsedTime = System.currentTimeMillis() - startTime
        println("收到响应，耗时: ${elapsedTime}ms")
        responseReceived = true
        response = res
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("收到错误: $error")
        latch.countDown()
      }
    })

    println("发送指令...")
    mockSender!!.sendCommand(command)

    val completed = latch.await(2, TimeUnit.SECONDS)
    val totalTime = System.currentTimeMillis() - startTime

    println("等待结果: completed=$completed")
    println("总耗时: ${totalTime}ms")
    println("响应长度: ${response?.size}")

    assertTrue("应该收到响应", completed)
    assertTrue("应该收到响应回调", responseReceived)
    assertNotNull("响应不应该为 null", response)
    assertTrue("响应应该有延迟 (> 100ms)", totalTime > 100)

    println("✅ 测试通过\n")
  }

  @Test
  fun testSendCommand_MultipleCommands() {
    println("=== 测试发送多个指令 ===")

    mockSender = MockCommandSender(true)
    val commandCount = 3

    val latch = CountDownLatch(commandCount)
    val responses = mutableListOf<ByteArray>()

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(response: ByteArray?) {
        println("收到响应 ${responses.size + 1}")
        response?.let {
          synchronized(responses) {
            responses.add(it)
          }
        }
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("收到错误: $error")
        latch.countDown()
      }
    })

    println("发送 $commandCount 个指令...")
    for (i in 1..commandCount) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)
      mockSender!!.sendCommand(command)
    }

    val completed = latch.await(5, TimeUnit.SECONDS)

    println("等待结果: completed=$completed")
    println("收到响应数量: ${responses.size}")

    assertTrue("应该收到所有响应", completed)
    assertEquals("应该收到 $commandCount 个响应", commandCount, responses.size)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetSentCommands() {
    println("=== 测试获取已发送指令 ===")

    mockSender = MockCommandSender(true)

    val commands = listOf(
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1),
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 2),
      LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    )

    println("发送 ${commands.size} 个指令...")
    commands.forEach { mockSender!!.sendCommand(it) }

    // 稍等一下确保指令被记录
    Thread.sleep(100)

    val sentCommands = mockSender!!.getSentCommands()
    println("已发送指令数量: ${sentCommands.size}")

    assertEquals("应该记录 ${commands.size} 个指令", commands.size, sentCommands.size)

    // 验证是副本
    assertNotSame("应该返回副本", commands[0], sentCommands[0])

    println("✅ 测试通过\n")
  }

  @Test
  fun testClearRecords() {
    println("=== 测试清空记录 ===")

    mockSender = MockCommandSender(true)

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    mockSender!!.sendCommand(command)

    Thread.sleep(100)

    var sentCommands = mockSender!!.getSentCommands()
    println("清空前已发送指令数量: ${sentCommands.size}")
    assertEquals("清空前应该有 1 个指令", 1, sentCommands.size)

    mockSender!!.clearRecords()

    sentCommands = mockSender!!.getSentCommands()
    println("清空后已发送指令数量: ${sentCommands.size}")
    assertEquals("清空后应该没有指令", 0, sentCommands.size)

    println("✅ 测试通过\n")
  }

  @Test
  fun testSimulateError_Manual() {
    println("=== 测试手动触发错误 ===")

    mockSender = MockCommandSender(true)

    val latch = CountDownLatch(1)
    var errorReceived = false
    var errorMessage: String? = null

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(response: ByteArray?) {
        // 不应该收到响应
      }

      override fun onError(error: String?) {
        println("收到错误: $error")
        errorReceived = true
        errorMessage = error
        latch.countDown()
      }
    })

    mockSender!!.simulateError()

    val completed = latch.await(2, TimeUnit.SECONDS)

    println("等待结果: completed=$completed")
    println("错误消息: $errorMessage")

    assertTrue("应该收到错误回调", completed)
    assertTrue("应该收到错误", errorReceived)
    assertNotNull("错误消息不应该为 null", errorMessage)

    println("✅ 测试通过\n")
  }

  @Test
  fun testSetDefaultResponseDelay() {
    println("=== 测试设置默认响应延迟 ===")

    mockSender = MockCommandSender(true)
    val customDelay = 50L  // 设置较短的延迟

    mockSender!!.setDefaultResponseDelay(customDelay)

    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)

    val latch = CountDownLatch(1)
    val startTime = System.currentTimeMillis()

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(response: ByteArray?) {
        latch.countDown()
      }

      override fun onError(error: String?) {
        latch.countDown()
      }
    })

    mockSender!!.sendCommand(command)

    val completed = latch.await(2, TimeUnit.SECONDS)
    val elapsedTime = System.currentTimeMillis() - startTime

    println("设置延迟: ${customDelay}ms")
    println("实际耗时: ${elapsedTime}ms")

    assertTrue("应该收到响应", completed)
    // 由于查询指令有自己的延迟计算，实际延迟可能不完全等于设置值
    // 但应该在合理范围内
    assertTrue("耗时应该大于等于 50ms", elapsedTime >= 50)

    println("✅ 测试通过\n")
  }

  @Test
  fun testResponseContent_OpenSingleLock() {
    println("=== 测试开单个锁响应内容 ===")

    mockSender = MockCommandSender(true)
    val channelId = 3
    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), channelId)

    val latch = CountDownLatch(1)
    var response: ByteArray? = null

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(res: ByteArray?) {
        response = res
        latch.countDown()
      }

      override fun onError(error: String?) {
        latch.countDown()
      }
    })

    mockSender!!.sendCommand(command)

    val completed = latch.await(2, TimeUnit.SECONDS)

    println("等待结果: completed=$completed")
    println("响应长度: ${response?.size}")

    assertTrue("应该收到响应", completed)
    assertNotNull("响应不应该为 null", response)

    // 验证响应格式
    assertTrue("响应格式应该有效", ResponseMatcher.isValidResponseFormat(response))

    // 验证响应匹配指令
    assertTrue("响应应该匹配指令", ResponseMatcher.isResponseForCommand(response, command))

    // 验证通道ID
    val responseChannelId = ResponseMatcher.getChannelId(response)
    println("响应通道ID: $responseChannelId")
    assertEquals("响应通道ID应该匹配", channelId, responseChannelId)

    println("✅ 测试通过\n")
  }

  @Test
  fun testResponseContent_QueryStatus() {
    println("=== 测试查询状态响应内容 ===")

    mockSender = MockCommandSender(true)
    val channelId = 2
    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), channelId)

    val latch = CountDownLatch(1)
    var response: ByteArray? = null

    mockSender!!.setOnResponseListener(object : OnResponseListener {
      override fun onResponseReceived(res: ByteArray?) {
        response = res
        latch.countDown()
      }

      override fun onError(error: String?) {
        latch.countDown()
      }
    })

    mockSender!!.sendCommand(command)

    val completed = latch.await(2, TimeUnit.SECONDS)

    println("等待结果: completed=$completed")
    println("响应长度: ${response?.size}")

    assertTrue("应该收到响应", completed)
    assertNotNull("响应不应该为 null", response)

    // 验证响应格式
    assertTrue("响应格式应该有效", ResponseMatcher.isValidResponseFormat(response))

    // 验证响应匹配指令
    assertTrue("响应应该匹配指令", ResponseMatcher.isResponseForCommand(response, command))

    // 验证通道ID和锁状态
    val responseChannelId = ResponseMatcher.getChannelId(response)
    val lockStatus = ResponseMatcher.getLockStatus(response)
    println("响应通道ID: $responseChannelId")
    println("锁状态: 0x${String.format("%02X", lockStatus)}")

    assertEquals("响应通道ID应该匹配", channelId, responseChannelId)
    assertTrue("锁状态应该是有效值 (0x00 或 0x01)", lockStatus == 0x00 || lockStatus == 0x01)

    println("✅ 测试通过\n")
  }

  @Test
  fun testDifferentCommandTypes() {
    println("=== 测试不同类型指令的响应 ===")

    mockSender = MockCommandSender(true)

    val commands = listOf(
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1) to "开单个锁",
      LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1) to "查询单个门状态",
      LockCtlBoardCmdHelper.buildFlashChannelCommand(0x00.toByte(), 1) to "通道闪烁",
      LockCtlBoardCmdHelper.buildChannelKeepOpenCommand(0x00.toByte(), 1) to "通道常开",
      LockCtlBoardCmdHelper.buildCloseChannelCommand(0x00.toByte(), 1) to "关闭通道"
    )

    commands.forEach { (command, description) ->
      println("\n测试: $description")

      val latch = CountDownLatch(1)
      var response: ByteArray? = null

      mockSender!!.setOnResponseListener(object : OnResponseListener {
        override fun onResponseReceived(res: ByteArray?) {
          response = res
          latch.countDown()
        }

        override fun onError(error: String?) {
          println("错误: $error")
          latch.countDown()
        }
      })

      mockSender!!.sendCommand(command)

      val completed = latch.await(2, TimeUnit.SECONDS)

      println("  完成: $completed")
      println("  响应长度: ${response?.size}")
      println("  格式有效: ${ResponseMatcher.isValidResponseFormat(response)}")
      println("  指令匹配: ${ResponseMatcher.isResponseForCommand(response, command)}")

      assertTrue("$description 应该收到响应", completed)
      assertNotNull("$description 响应不应该为 null", response)
      assertTrue("$description 响应格式应该有效", ResponseMatcher.isValidResponseFormat(response))
      assertTrue(
        "$description 响应应该匹配指令",
        ResponseMatcher.isResponseForCommand(response, command)
      )
    }

    println("\n✅ 测试通过\n")
  }

  @Test
  fun testShutdown() {
    println("=== 测试关闭 Mock 发送器 ===")

    mockSender = MockCommandSender(true)

    println("关闭前连接状态: ${mockSender!!.isConnected}")
    assertTrue("关闭前应该已连接", mockSender!!.isConnected)

    mockSender!!.shutdown()

    println("关闭后连接状态: ${mockSender!!.isConnected}")
    assertFalse("关闭后应该未连接", mockSender!!.isConnected)

    println("✅ 测试通过\n")
  }
}
