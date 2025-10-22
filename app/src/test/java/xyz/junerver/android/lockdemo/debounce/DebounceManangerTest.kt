package xyz.junerver.android.lockdemo.debounce

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.LockCtlBoardCmdHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull


@RunWith(RobolectricTestRunner::class)
class DebounceManangerTest {

  private var mockSender: MockCommandSender? = null
  private var debounceManager: CommandDebounceManager? = null

  @After
  fun tearDown() {
    println("=== 清理测试资源 ===")
    debounceManager?.shutdown()
    mockSender?.shutdown()
    mockSender = null
    debounceManager = null
    println("✅ 资源清理完成")
  }

  @Test
  fun testBasicCommandExecution() {
    println("=== 开始基本指令执行测试 ===")

    mockSender = MockCommandSender()
    debounceManager = CommandDebounceManager(mockSender!!)

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    println("构建开锁指令，长度: ${command.size}")

    // 使用CountDownLatch等待异步操作完成
    val latch = CountDownLatch(1)
    var success = false
    var errorMessage: String? = null

    println("发送指令到防抖管理器...")

    debounceManager!!.sendCommand(command, object : OnCommandListener {
      override fun onSuccess() {
        println("✅ 收到成功回调")
        success = true
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("❌ 收到错误回调: $error")
        errorMessage = error
        latch.countDown()
      }
    })

    println("等待指令执行完成...")

    // 等待异步操作完成，最多等待10秒
    val completed = latch.await(10, TimeUnit.SECONDS)
    println("等待结果: completed=$completed")

    // 验证结果
    assertTrue("指令执行应该在10秒内完成", completed)
    assertTrue("指令应该成功执行", success)
    assertNull("不应该有错误消息", errorMessage)

    // 验证Mock发送器确实收到了指令
    val sentCommands = mockSender!!.getSentCommands()
    println("Mock发送器收到的指令数量: ${sentCommands.size}")
    assertEquals("应该发送了1条指令", 1, sentCommands.size)

    println("✅ 基本指令执行测试通过！")
  }

  @Test
  fun testSimpleCommandSend() {
    println("=== 开始简单指令发送测试 ===")

    mockSender = MockCommandSender()
    debounceManager = CommandDebounceManager(mockSender!!)

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 2)
    println("发送第二个开锁指令...")

    val latch = CountDownLatch(1)
    var callbackReceived = false

    debounceManager!!.sendCommand(command, object : OnCommandListener {
      override fun onSuccess() {
        println("✅ 第二个指令成功")
        callbackReceived = true
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("❌ 第二个指令失败: $error")
        latch.countDown()
      }
    })

    val completed = latch.await(5, TimeUnit.SECONDS)
    assertTrue("第二个指令应该在5秒内完成", completed)
    assertTrue("应该收到第二个指令的回调", callbackReceived)

    println("✅ 简单指令发送测试通过！")
  }

  @Test
  fun testContinuousSendWithDebounce() {
    println("=== 开始连续发送防抖测试 ===")

    mockSender = MockCommandSender()
    debounceManager = CommandDebounceManager(mockSender!!)

    val commandCount = 5
    val latch = CountDownLatch(commandCount)
    val successResults = mutableListOf<Int>()
    val errorResults = mutableListOf<String>()

    println("准备连续发送 $commandCount 个指令...")

    // 连续发送多个指令
    for (i in 1..commandCount) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)
      println("[$i] 发送开锁指令: 通道=$i")

      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {
          println("[$i] ✅ 指令成功完成")
          synchronized(successResults) {
            successResults.add(i)
          }
          latch.countDown()
        }

        override fun onError(error: String?) {
          println("[$i] ❌ 指令失败: $error")
          synchronized(errorResults) {
            errorResults.add("[$i] $error")
          }
          latch.countDown()
        }
      })

      // 打印队列状态
      val status = debounceManager!!.getStatus()
      println("[$i] 队列状态: ${status}")
    }

    println("所有指令已入队，等待执行完成...")

    // 等待所有指令完成，给予充足时间 (每个指令约350-450ms，5个指令约2-3秒)
    val completed = latch.await(15, TimeUnit.SECONDS)
    println("等待结果: completed=$completed")

    // 打印最终结果
    println("=== 执行结果汇总 ===")
    println("成功指令数: ${successResults.size}")
    println("成功指令列表: $successResults")
    println("失败指令数: ${errorResults.size}")
    if (errorResults.isNotEmpty()) {
      println("失败指令详情:")
      errorResults.forEach { println("  $it") }
    }

    // 验证最终队列状态
    val finalStatus = debounceManager!!.getStatus()
    println("最终队列状态: $finalStatus")

    // 验证结果
    assertTrue("所有指令应该在15秒内完成", completed)
    assertEquals("应该成功执行$commandCount 个指令", commandCount, successResults.size)
    assertEquals("不应该有失败的指令", 0, errorResults.size)

    // 验证指令按顺序执行
    for (i in 1..commandCount) {
      assertTrue("指令$i 应该被成功执行", successResults.contains(i))
    }

    // 验证Mock发送器确实收到了所有指令
    val sentCommands = mockSender!!.getSentCommands()
    println("Mock发送器总共收到的指令数量: ${sentCommands.size}")
    assertEquals("应该发送了$commandCount 条指令", commandCount, sentCommands.size)

    println("✅ 连续发送防抖测试通过！")
  }

  @Test
  fun testRapidSendWithDebounceAndTiming() {
    println("=== 开始快速连续发送时序测试 ===")

    mockSender = MockCommandSender()
    debounceManager = CommandDebounceManager(mockSender!!)

    val commandCount = 3
    val latch = CountDownLatch(commandCount)
    val executionTimes = mutableListOf<Pair<Int, Long>>() // 记录每个指令的完成时间

    println("快速连续发送 $commandCount 个指令，观察防抖机制...")

    val startTime = System.currentTimeMillis()

    // 快速连续发送指令（模拟用户快速点击）
    for (i in 1..commandCount) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)

      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {
          val elapsedTime = System.currentTimeMillis() - startTime
          synchronized(executionTimes) {
            executionTimes.add(Pair(i, elapsedTime))
          }
          println("[$i] ✅ 完成时间: ${elapsedTime}ms")
          latch.countDown()
        }

        override fun onError(error: String?) {
          println("[$i] ❌ 失败: $error")
          latch.countDown()
        }
      })

      // 打印入队时间
      val queueTime = System.currentTimeMillis() - startTime
      println("[$i] 入队时间: ${queueTime}ms, 队列大小: ${debounceManager!!.getStatus().queueSize}")
    }

    println("等待所有指令执行完成...")
    val completed = latch.await(10, TimeUnit.SECONDS)
    val totalTime = System.currentTimeMillis() - startTime

    // 分析执行时序
    println("\n=== 执行时序分析 ===")
    println("总耗时: ${totalTime}ms")
    executionTimes.sortedBy { it.first }.forEach { (index, time) ->
      println("指令[$index] 完成于: ${time}ms")
    }

    // 验证指令是顺序执行的（每个指令之间应该有间隔）
    if (executionTimes.size >= 2) {
      val sortedTimes = executionTimes.sortedBy { it.first }
      for (i in 0 until sortedTimes.size - 1) {
        val currentTime = sortedTimes[i].second
        val nextTime = sortedTimes[i + 1].second
        val interval = nextTime - currentTime
        println("指令间隔: [${i + 1}]->[${i + 2}] = ${interval}ms")

        // 验证间隔合理（应该在300-600ms之间，因为每个指令需要350-450ms响应时间）
        assertTrue("指令间隔应该大于100ms (实际: ${interval}ms)", interval > 100)
      }
    }

    // 验证结果
    assertTrue("所有指令应该完成", completed)
    assertEquals("应该执行$commandCount 个指令", commandCount, executionTimes.size)

    println("✅ 快速连续发送时序测试通过！")
  }

  @Test
  fun testCommandTimeout() {
    println("=== 测试指令超时场景 ===")

    // 创建一个不会发送响应的 Mock 发送器
    val mockSender = MockCommandSender(true)
    mockSender.setDefaultResponseDelay(10000)  // 设置很长的延迟,超过超时时间

    debounceManager = CommandDebounceManager(mockSender)

    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    println("发送查询指令（超时时间: 200ms）...")

    val latch = CountDownLatch(1)
    var errorReceived = false
    var errorMessage: String? = null

    debounceManager!!.sendCommand(command, object : OnCommandListener {
      override fun onSuccess() {
        println("❌ 不应该收到成功回调")
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("✅ 收到超时错误: $error")
        errorReceived = true
        errorMessage = error
        latch.countDown()
      }
    })

    val startTime = System.currentTimeMillis()
    val completed = latch.await(5, TimeUnit.SECONDS)
    val elapsedTime = System.currentTimeMillis() - startTime

    println("等待结果: completed=$completed")
    println("耗时: ${elapsedTime}ms")
    println("错误消息: $errorMessage")

    assertTrue("应该收到错误回调", completed)
    assertTrue("应该检测到超时", errorReceived)
    assertNotNull("错误消息不应该为 null", errorMessage)
    assertTrue("错误消息应该包含 '超时'", errorMessage!!.contains("超时"))

    // 验证超时统计
    val status = debounceManager!!.getStatus()
    println("队列状态: $status")
    assertTrue("超时计数应该大于 0", status.totalTimeouts > 0)

    println("✅ 指令超时测试通过！")
  }

  @Test
  fun testTimeoutThenContinue() {
    println("=== 测试超时后继续执行下一个指令 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    val latch = CountDownLatch(2)
    val results = mutableListOf<String>()

    // 第一个指令 - 模拟超时（设置很长的延迟）
    mockSender!!.setDefaultResponseDelay(10000)
    val command1 = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)

    debounceManager!!.sendCommand(command1, object : OnCommandListener {
      override fun onSuccess() {
        synchronized(results) {
          results.add("1-success")
        }
        latch.countDown()
      }

      override fun onError(error: String?) {
        synchronized(results) {
          results.add("1-timeout")
        }
        println("[1] 超时: $error")
        latch.countDown()
      }
    })

    // 稍等一下再发送第二个指令
    Thread.sleep(100)

    // 第二个指令 - 正常响应（恢复正常延迟）
    mockSender!!.setDefaultResponseDelay(100)
    val command2 = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 2)

    debounceManager!!.sendCommand(command2, object : OnCommandListener {
      override fun onSuccess() {
        synchronized(results) {
          results.add("2-success")
        }
        println("[2] 成功")
        latch.countDown()
      }

      override fun onError(error: String?) {
        synchronized(results) {
          results.add("2-error")
        }
        println("[2] 失败: $error")
        latch.countDown()
      }
    })

    println("等待两个指令执行...")
    val completed = latch.await(10, TimeUnit.SECONDS)

    println("执行结果: $results")

    assertTrue("两个指令应该都完成", completed)
    assertEquals("应该有 2 个结果", 2, results.size)
    assertEquals("第一个指令应该超时", "1-timeout", results[0])
    assertEquals("第二个指令应该成功", "2-success", results[1])

    println("✅ 超时后继续执行测试通过！")
  }

  @Test
  fun testCommunicationError() {
    println("=== 测试通信错误场景 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)

    val latch = CountDownLatch(1)
    var errorReceived = false
    var errorMessage: String? = null

    debounceManager!!.sendCommand(command, object : OnCommandListener {
      override fun onSuccess() {
        println("❌ 不应该收到成功回调")
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("✅ 收到通信错误: $error")
        errorReceived = true
        errorMessage = error
        latch.countDown()
      }
    })

    // 模拟通信错误
    Thread.sleep(100)
    mockSender!!.simulateError()

    val completed = latch.await(5, TimeUnit.SECONDS)

    println("等待结果: completed=$completed")
    println("错误消息: $errorMessage")

    assertTrue("应该收到错误回调", completed)
    assertTrue("应该检测到错误", errorReceived)
    assertNotNull("错误消息不应该为 null", errorMessage)

    // 验证错误统计
    val status = debounceManager!!.getStatus()
    println("队列状态: $status")
    assertTrue("错误计数应该大于 0", status.totalErrors > 0)

    println("✅ 通信错误测试通过！")
  }

  @Test
  fun testQueueStatus() {
    println("=== 测试队列状态查询 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    // 初始状态
    var status = debounceManager!!.getStatus()
    println("初始状态: $status")
    assertEquals("初始队列应该为空", 0, status.queueSize)
    assertEquals("初始发送数应该为 0", 0L, status.totalCommandsSent)

    // 快速发送多个指令
    val commandCount = 3
    for (i in 1..commandCount) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)
      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {}
        override fun onError(error: String?) {}
      })
    }

    // 立即查询状态
    status = debounceManager!!.getStatus()
    println("发送后状态: $status")
    assertEquals("发送数应该是 $commandCount", commandCount.toLong(), status.totalCommandsSent)
    assertTrue("队列大小应该大于 0", status.queueSize > 0 || status.isExecuting)

    // 等待所有指令完成
    Thread.sleep(3000)

    status = debounceManager!!.getStatus()
    println("完成后状态: $status")
    assertEquals("完成数应该是 $commandCount", commandCount.toLong(), status.totalCommandsCompleted)
    assertEquals("队列应该为空", 0, status.queueSize)
    assertFalse("不应该有正在执行的指令", status.isExecuting)

    println("✅ 队列状态测试通过！")
  }

  @Test
  fun testClearQueue() {
    println("=== 测试清空队列 ===")

    mockSender = MockCommandSender(true)
    mockSender!!.setDefaultResponseDelay(1000)  // 设置较长延迟
    debounceManager = CommandDebounceManager(mockSender!!)

    // 快速发送多个指令
    val commandCount = 5
    for (i in 1..commandCount) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)
      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {}
        override fun onError(error: String?) {}
      })
    }

    Thread.sleep(200)  // 等待指令入队

    var status = debounceManager!!.getStatus()
    println("清空前队列大小: ${status.queueSize}")
    assertTrue("清空前队列应该不为空", status.queueSize > 0)

    // 清空队列
    debounceManager!!.clearQueue()

    status = debounceManager!!.getStatus()
    println("清空后队列大小: ${status.queueSize}")
    assertEquals("清空后队列应该为空", 0, status.queueSize)

    println("✅ 清空队列测试通过！")
  }

  @Test
  fun testShutdownAfterCommands() {
    println("=== 测试发送指令后关闭管理器 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    val latch = CountDownLatch(2)
    var successCount = 0

    // 发送两个指令
    for (i in 1..2) {
      val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), i)
      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {
          successCount++
          println("指令 $i 成功")
          latch.countDown()
        }

        override fun onError(error: String?) {
          println("指令 $i 失败: $error")
          latch.countDown()
        }
      })
    }

    // 等待指令完成
    val completed = latch.await(5, TimeUnit.SECONDS)
    println("指令执行结果: completed=$completed, successCount=$successCount")

    // 关闭管理器
    debounceManager!!.shutdown()
    println("管理器已关闭")

    // 尝试再发送指令
    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 3)
    val errorLatch = CountDownLatch(1)
    var errorMessage: String? = null

    debounceManager!!.sendCommand(command, object : OnCommandListener {
      override fun onSuccess() {
        println("❌ 不应该成功")
      }

      override fun onError(error: String?) {
        println("✅ 收到预期错误: $error")
        errorMessage = error
        errorLatch.countDown()
      }
    })

    val errorReceived = errorLatch.await(2, TimeUnit.SECONDS)

    assertTrue("关闭前的指令应该完成", completed)
    assertTrue("关闭后的指令应该收到错误", errorReceived)
    assertNotNull("错误消息不应该为 null", errorMessage)
    assertTrue("错误消息应该包含 '关闭'", errorMessage!!.contains("关闭"))

    println("✅ 关闭后发送指令测试通过！")
  }

  @Test
  fun testInvalidCommand() {
    println("=== 测试发送无效指令 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    val invalidCommand = byteArrayOf(0x57, 0x4B, 0x4C)  // 太短的指令

    val latch = CountDownLatch(1)
    var errorMessage: String? = null

    debounceManager!!.sendCommand(invalidCommand, object : OnCommandListener {
      override fun onSuccess() {
        println("❌ 不应该成功")
        latch.countDown()
      }

      override fun onError(error: String?) {
        println("✅ 收到错误: $error")
        errorMessage = error
        latch.countDown()
      }
    })

    val completed = latch.await(2, TimeUnit.SECONDS)

    println("错误消息: $errorMessage")

    assertTrue("应该收到错误回调", completed)
    assertNotNull("错误消息不应该为 null", errorMessage)
    assertTrue("错误消息应该包含 '格式错误'", errorMessage!!.contains("格式错误"))

    println("✅ 无效指令测试通过！")
  }

  @Test
  fun testMixedOperations() {
    println("=== 测试混合操作场景 ===")

    mockSender = MockCommandSender(true)
    debounceManager = CommandDebounceManager(mockSender!!)

    val totalCommands = 6
    val latch = CountDownLatch(totalCommands)
    val results = mutableListOf<Pair<Int, Boolean>>()

    // 混合不同类型的指令
    val commands = listOf(
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1),
      LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1),
      LockCtlBoardCmdHelper.buildFlashChannelCommand(0x00.toByte(), 2),
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 3),
      LockCtlBoardCmdHelper.buildChannelKeepOpenCommand(0x00.toByte(), 4),
      LockCtlBoardCmdHelper.buildCloseChannelCommand(0x00.toByte(), 5)
    )

    commands.forEachIndexed { index, command ->
      debounceManager!!.sendCommand(command, object : OnCommandListener {
        override fun onSuccess() {
          synchronized(results) {
            results.add(Pair(index + 1, true))
          }
          println("指令 ${index + 1} 成功")
          latch.countDown()
        }

        override fun onError(error: String?) {
          synchronized(results) {
            results.add(Pair(index + 1, false))
          }
          println("指令 ${index + 1} 失败: $error")
          latch.countDown()
        }
      })
    }

    val completed = latch.await(15, TimeUnit.SECONDS)

    println("执行结果:")
    results.forEach { (index, success) ->
      println("  指令 $index: ${if (success) "成功" else "失败"}")
    }

    val status = debounceManager!!.getStatus()
    println("最终状态: $status")

    assertTrue("所有指令应该完成", completed)
    assertEquals("应该执行 $totalCommands 个指令", totalCommands, results.size)

    val successCount = results.count { it.second }
    println("成功数量: $successCount / $totalCommands")
    assertTrue("大部分指令应该成功", successCount >= totalCommands * 0.8)

    println("✅ 混合操作测试通过！")
  }
}