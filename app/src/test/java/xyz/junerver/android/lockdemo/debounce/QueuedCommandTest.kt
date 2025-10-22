package xyz.junerver.android.lockdemo.debounce

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.LockCtlBoardCmdHelper

/**
 * QueuedCommand 测试类
 * 测试队列指令的创建、状态管理、时间计算等功能
 */
@RunWith(RobolectricTestRunner::class)
class QueuedCommandTest {

  @Test
  fun testCreate_ValidCommand() {
    println("=== 测试创建有效的队列指令 ===")

    val commandData = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(commandData, listener)

    println("指令字: 0x${String.format("%02X", queuedCommand.commandByte.toInt() and 0xFF)}")
    println("板地址: 0x${String.format("%02X", queuedCommand.boardAddress.toInt() and 0xFF)}")
    println("超时时间: ${queuedCommand.timeout}ms")
    println("指令描述: ${queuedCommand.description}")

    assertNotNull("队列指令不应该为 null", queuedCommand)
    assertEquals("指令字应该是 0x82", 0x82.toByte(), queuedCommand.commandByte)
    assertEquals("板地址应该是 0x00", 0x00.toByte(), queuedCommand.boardAddress)
    assertEquals("超时时间应该是 700ms", 700L, queuedCommand.timeout)
    assertFalse("初始状态不应该完成", queuedCommand.isCompleted)
    assertFalse("初始状态不应该超时", queuedCommand.isTimedOut)

    println("✅ 测试通过\n")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testCreate_NullCommand() {
    println("=== 测试创建 null 指令 ===")

    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    // 应该抛出 IllegalArgumentException
    QueuedCommand(null, listener)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testCreate_CommandTooShort() {
    println("=== 测试创建长度不足的指令 ===")

    val invalidCommand = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x06, 0x00)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    // 应该抛出 IllegalArgumentException
    QueuedCommand(invalidCommand, listener)
  }

  @Test
  fun testGetCommandData_ReturnsClone() {
    println("=== 测试 getCommandData 返回副本 ===")

    val originalCommand = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(originalCommand, listener)
    val returnedCommand = queuedCommand.commandData

    println("原始指令地址: ${System.identityHashCode(originalCommand)}")
    println("返回指令地址: ${System.identityHashCode(returnedCommand)}")

    // 验证返回的是副本，不是原始数组
    assertNotSame("应该返回副本而不是原始数组", originalCommand, returnedCommand)
    assertArrayEquals("副本内容应该与原始内容相同", originalCommand, returnedCommand)

    // 修改返回的数组不应该影响队列指令
    returnedCommand[0] = 0xFF.toByte()
    val secondReturnedCommand = queuedCommand.commandData
    assertNotEquals("修改副本不应该影响队列指令", 0xFF.toByte(), secondReturnedCommand[0])

    println("✅ 测试通过\n")
  }

  @Test
  fun testCompletedState() {
    println("=== 测试完成状态管理 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    println("初始状态: completed=${queuedCommand.isCompleted}")
    assertFalse("初始状态不应该完成", queuedCommand.isCompleted)

    // 标记为完成
    queuedCommand.setCompleted(true)
    println("设置完成后: completed=${queuedCommand.isCompleted}")
    assertTrue("设置后应该完成", queuedCommand.isCompleted)

    // 重新标记为未完成
    queuedCommand.setCompleted(false)
    println("重新设置后: completed=${queuedCommand.isCompleted}")
    assertFalse("重新设置后不应该完成", queuedCommand.isCompleted)

    println("✅ 测试通过\n")
  }

  @Test
  fun testTimedOutState() {
    println("=== 测试超时状态管理 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    println("初始状态: timedOut=${queuedCommand.isTimedOut}")
    assertFalse("初始状态不应该超时", queuedCommand.isTimedOut)

    // 标记为超时
    queuedCommand.setTimedOut(true)
    println("设置超时后: timedOut=${queuedCommand.isTimedOut}")
    assertTrue("设置后应该超时", queuedCommand.isTimedOut)

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsExpired_NotExpired() {
    println("=== 测试未超时的指令 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    println("超时时间: ${queuedCommand.timeout}ms")
    println("是否超时: ${queuedCommand.isExpired()}")

    // 刚创建的指令不应该超时
    assertFalse("刚创建的指令不应该超时", queuedCommand.isExpired())

    println("✅ 测试通过\n")
  }

  @Test
  fun testIsExpired_Expired() {
    println("=== 测试已超时的指令 ===")

    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    println("超时时间: ${queuedCommand.timeout}ms")

    // 等待超时时间 + 100ms
    Thread.sleep(queuedCommand.timeout + 100)

    println("等待 ${queuedCommand.timeout + 100}ms 后")
    println("是否超时: ${queuedCommand.isExpired()}")

    assertTrue("等待超时时间后应该超时", queuedCommand.isExpired())

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetRemainingTimeout() {
    println("=== 测试剩余超时时间计算 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    val initialRemaining = queuedCommand.getRemainingTimeout()
    println("初始剩余超时时间: ${initialRemaining}ms")
    println("配置超时时间: ${queuedCommand.timeout}ms")

    // 初始剩余时间应该接近配置的超时时间
    assertTrue("初始剩余时间应该大于 0", initialRemaining > 0)
    assertTrue(
      "初始剩余时间应该接近配置超时时间",
      Math.abs(initialRemaining - queuedCommand.timeout) < 50
    )

    // 等待一段时间
    val waitTime = 100L
    Thread.sleep(waitTime)

    val afterWaitRemaining = queuedCommand.getRemainingTimeout()
    println("等待 ${waitTime}ms 后剩余时间: ${afterWaitRemaining}ms")

    // 剩余时间应该减少
    assertTrue("等待后剩余时间应该减少", afterWaitRemaining < initialRemaining)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetRemainingTimeout_AfterExpired() {
    println("=== 测试超时后的剩余时间 ===")

    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    println("超时时间: ${queuedCommand.timeout}ms")

    // 等待超时时间 + 100ms
    Thread.sleep(queuedCommand.timeout + 100)

    val remaining = queuedCommand.getRemainingTimeout()
    println("超时后剩余时间: ${remaining}ms")

    // 超时后剩余时间应该是 0
    assertEquals("超时后剩余时间应该是 0", 0L, remaining)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetDescription() {
    println("=== 测试获取指令描述 ===")

    val testCases = listOf(
      LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1) to "开单个锁",
      LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1) to "查询单个门状态",
      LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand(0x00.toByte()) to "查询所有门状态",
      LockCtlBoardCmdHelper.buildFlashChannelCommand(0x00.toByte(), 1) to "通道闪烁"
    )

    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    testCases.forEach { (command, expectedDesc) ->
      val queuedCommand = QueuedCommand(command, listener)
      val description = queuedCommand.description

      println(
        "指令字: 0x${
          String.format(
            "%02X",
            queuedCommand.commandByte.toInt() and 0xFF
          )
        } -> $description"
      )
      assertEquals("描述应该是 '$expectedDesc'", expectedDesc, description)
    }

    println("✅ 测试通过\n")
  }

  @Test
  fun testToString() {
    println("=== 测试 toString 方法 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)
    val toString = queuedCommand.toString()

    println("toString 输出:")
    println(toString)

    // 验证 toString 包含关键信息
    assertTrue("toString 应该包含 'QueuedCommand'", toString.contains("QueuedCommand"))
    assertTrue("toString 应该包含指令字", toString.contains("cmd="))
    assertTrue("toString 应该包含板地址", toString.contains("addr="))
    assertTrue("toString 应该包含超时时间", toString.contains("timeout="))
    assertTrue("toString 应该包含完成状态", toString.contains("completed="))
    assertTrue("toString 应该包含超时状态", toString.contains("timedOut="))
    assertTrue("toString 应该包含描述", toString.contains("desc="))

    println("✅ 测试通过\n")
  }

  @Test
  fun testTimestamp() {
    println("=== 测试时间戳记录 ===")

    val beforeCreate = System.currentTimeMillis()

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {}
      override fun onError(error: String?) {}
    }

    val queuedCommand = QueuedCommand(command, listener)

    val afterCreate = System.currentTimeMillis()
    val timestamp = queuedCommand.timestamp

    println("创建前时间: $beforeCreate")
    println("创建时间戳: $timestamp")
    println("创建后时间: $afterCreate")

    // 时间戳应该在创建前后之间
    assertTrue("时间戳应该大于等于创建前时间", timestamp >= beforeCreate)
    assertTrue("时间戳应该小于等于创建后时间", timestamp <= afterCreate)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetListener() {
    println("=== 测试获取监听器 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val listener = object : OnCommandListener {
      override fun onSuccess() {
        println("Success callback")
      }

      override fun onError(error: String?) {
        println("Error callback: $error")
      }
    }

    val queuedCommand = QueuedCommand(command, listener)
    val returnedListener = queuedCommand.listener

    println("原始监听器: $listener")
    println("返回监听器: $returnedListener")

    // 验证返回的是同一个监听器
    assertSame("应该返回同一个监听器实例", listener, returnedListener)

    println("✅ 测试通过\n")
  }
}
