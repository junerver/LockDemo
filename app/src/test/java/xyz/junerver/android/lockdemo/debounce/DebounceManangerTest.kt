package xyz.junerver.android.lockdemo.debounce

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.LockCtlBoardCmdHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


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
}