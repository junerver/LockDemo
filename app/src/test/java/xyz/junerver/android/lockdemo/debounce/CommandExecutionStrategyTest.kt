package xyz.junerver.android.lockdemo.debounce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.junerver.android.lockdemo.LockCtlBoardCmdHelper

/**
 * CommandExecutionStrategy 测试类
 * 测试指令执行策略的超时计算、指令描述等功能
 */
@RunWith(RobolectricTestRunner::class)
class CommandExecutionStrategyTest {

  @Test
  fun testCalculateTimeout_OpenSingleLock() {
    println("=== 测试开单个锁指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand(0x00.toByte(), 1)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 开单个锁 (0x82)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 700ms (350ms × 2)")

    // 开单个锁基础时间 350ms，安全系数 2，超时时间应该是 700ms
    assertEquals("开单个锁超时时间应该是 700ms", 700L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_QuerySingleStatus() {
    println("=== 测试查询单个门状态指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildGetSingleLockStatusCommand(0x00.toByte(), 1)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 查询单个门状态 (0x83)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 200ms (100ms × 2)")

    // 查询单个门状态基础时间 100ms，安全系数 2，超时时间应该是 200ms
    assertEquals("查询单个门状态超时时间应该是 200ms", 200L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_QueryAllStatus() {
    println("=== 测试查询所有门状态指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildGetAllLocksStatusCommand(0x00.toByte())
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 查询所有门状态 (0x84)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 400ms (200ms × 2)")

    // 查询所有门状态基础时间 200ms，安全系数 2，超时时间应该是 400ms
    assertEquals("查询所有门状态超时时间应该是 400ms", 400L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_SimultaneousLocks() {
    println("=== 测试同时开多锁指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildOpenMultipleLocksCommand(0x00.toByte(), 1, 2)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 同时开多锁 (0x80)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 700ms (350ms × 2)")

    // 同时开多锁基础时间 350ms，安全系数 2，超时时间应该是 700ms
    assertEquals("同时开多锁超时时间应该是 700ms", 700L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_SequentialMultipleLocks() {
    println("=== 测试逐一开多锁指令超时计算 ===")

    val lockCount = 3
    val command = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand(0x00.toByte(), 1, 2, 3)

    println("指令类型: 逐一开多锁 (0x86)")
    println("锁数量: $lockCount")
    println("指令长度: ${command?.size}")
    if (command != null && command.size >= 8) {
      println("指令字: 0x${String.format("%02X", command[6].toInt() and 0xFF)}")
      println("数据域首字节(锁数量): ${command[7].toInt() and 0xFF}")
    }

    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: ${350 * lockCount * 2}ms (350ms × $lockCount × 2)")

    // 逐一开多锁基础时间 350ms × 锁数量，安全系数 2
    val expectedTimeout = 350L * lockCount * 2
    assertEquals("逐一开多锁超时时间应该是 ${expectedTimeout}ms", expectedTimeout, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_ChannelFlash() {
    println("=== 测试通道闪烁指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildFlashChannelCommand(0x00.toByte(), 1)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 通道闪烁 (0x81)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 200ms (100ms × 2)")

    // 通道控制基础时间 100ms，安全系数 2，超时时间应该是 200ms
    assertEquals("通道闪烁超时时间应该是 200ms", 200L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_ChannelKeepOpen() {
    println("=== 测试通道常开指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildChannelKeepOpenCommand(0x00.toByte(), 1)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 通道常开 (0x88)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 200ms (100ms × 2)")

    // 通道控制基础时间 100ms，安全系数 2，超时时间应该是 200ms
    assertEquals("通道常开超时时间应该是 200ms", 200L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_CloseChannel() {
    println("=== 测试关闭通道指令超时计算 ===")

    val command = LockCtlBoardCmdHelper.buildCloseChannelCommand(0x00.toByte(), 1)
    val timeout = CommandExecutionStrategy.calculateTimeout(command)

    println("指令类型: 关闭通道 (0x89)")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 200ms (100ms × 2)")

    // 通道控制基础时间 100ms，安全系数 2，超时时间应该是 200ms
    assertEquals("关闭通道超时时间应该是 200ms", 200L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_InvalidCommand_Null() {
    println("=== 测试无效指令 (null) 超时计算 ===")

    val timeout = CommandExecutionStrategy.calculateTimeout(null)

    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 1000ms (默认超时)")

    // null 指令应该返回默认超时 1000ms
    assertEquals("null 指令应该返回默认超时 1000ms", 1000L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_InvalidCommand_TooShort() {
    println("=== 测试无效指令 (长度不足) 超时计算 ===")

    val invalidCommand = byteArrayOf(0x57, 0x4B, 0x4C, 0x59, 0x06, 0x00)  // 只有6字节
    val timeout = CommandExecutionStrategy.calculateTimeout(invalidCommand)

    println("指令长度: ${invalidCommand.size}")
    println("计算超时时间: ${timeout}ms")
    println("预期超时时间: 1000ms (默认超时)")

    // 长度不足的指令应该返回默认超时 1000ms
    assertEquals("长度不足的指令应该返回默认超时 1000ms", 1000L, timeout)

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetCommandDescription_AllKnownCommands() {
    println("=== 测试所有已知指令的描述 ===")

    val commandDescriptions = mapOf(
      0x80.toByte() to "同时开多锁",
      0x81.toByte() to "通道闪烁",
      0x82.toByte() to "开单个锁",
      0x83.toByte() to "查询单个门状态",
      0x84.toByte() to "查询所有门状态",
      0x85.toByte() to "锁控板主动上报数据",
      0x86.toByte() to "开全部锁",
      0x88.toByte() to "通道常开",
      0x89.toByte() to "通道关闭"
    )

    commandDescriptions.forEach { (commandByte, expectedDesc) ->
      val actualDesc = CommandExecutionStrategy.getCommandDescription(commandByte)
      println("指令字: 0x${String.format("%02X", commandByte.toInt() and 0xFF)} -> $actualDesc")
      assertEquals(
        "指令字 0x${
          String.format(
            "%02X",
            commandByte.toInt() and 0xFF
          )
        } 的描述应该是 '$expectedDesc'",
        expectedDesc, actualDesc
      )
    }

    println("✅ 测试通过\n")
  }

  @Test
  fun testGetCommandDescription_UnknownCommand() {
    println("=== 测试未知指令的描述 ===")

    val unknownCommand = 0xFF.toByte()
    val description = CommandExecutionStrategy.getCommandDescription(unknownCommand)

    println("指令字: 0x${String.format("%02X", unknownCommand.toInt() and 0xFF)}")
    println("描述: $description")

    assertTrue("未知指令应该包含 '未知指令' 字样", description.contains("未知指令"))
    assertTrue("未知指令描述应该包含指令字", description.contains("0xFF"))

    println("✅ 测试通过\n")
  }

  @Test
  fun testCalculateTimeout_DifferentLockCounts() {
    println("=== 测试不同锁数量的超时计算 ===")

    val lockCounts = listOf(1, 2, 3, 5, 7)

    lockCounts.forEach { count ->
      val locks = IntArray(count) { it + 1 }
      val command = LockCtlBoardCmdHelper.buildOpenMultipleSequentialCommand(0x00.toByte(), *locks)

      // 打印指令详细信息用于调试
      println("锁数量: $count")
      println("指令长度: ${command?.size}")
      if (command != null && command.size >= 8) {
        println("指令字: 0x${String.format("%02X", command[6].toInt() and 0xFF)}")
        println("数据域首字节(锁数量): ${command[7].toInt() and 0xFF}")
      }

      val timeout = CommandExecutionStrategy.calculateTimeout(command)
      val expectedTimeout = 350L * count * 2

      println("计算超时时间: ${timeout}ms, 预期: ${expectedTimeout}ms")
      assertEquals("锁数量为 $count 时超时应该是 ${expectedTimeout}ms", expectedTimeout, timeout)
    }

    println("✅ 测试通过\n")
  }
}
