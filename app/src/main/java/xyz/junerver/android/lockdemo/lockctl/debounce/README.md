# 指令防抖系统 (Command Debounce System)

## 概述

指令防抖系统是为锁控板通信设计的核心组件，解决了上位机与锁控板通信中的指令冲突和时序控制问题。系统采用基于响应确认的防抖机制，确保指令按正确顺序执行，避免通信冲突。

## 核心特性

### ✅ 响应确认机制

- 通过匹配响应指令字确认指令执行完成
- 支持板地址和指令字双重验证
- 确保指令执行结果的准确性

### ⏱️ 智能超时控制

- 基于不同指令类型的动态超时计算
- 安全系数设计，避免系统卡死
- 支持超时重试和错误处理

### 📦 指令队列管理

- 先进先出（FIFO）队列设计
- 自动处理指令执行时序
- 支持队列状态监控和管理

### 🧪 完全可测试

- 依赖注入设计，支持Mock测试
- 提供完整的测试套件
- 生产环境和测试环境无缝切换

### 🔧 高度可配置

- 支持不同通信方式（串口、网络等）
- 可配置的执行时间参数
- 灵活的错误处理策略

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application Layer)                │
├─────────────────────────────────────────────────────────────┤
│                指令防抖管理器 (CommandDebounceManager)       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   指令队列      │  │   超时控制      │  │   响应匹配      │ │
│  │   (Queue)       │  │   (Timeout)     │  │   (Matcher)     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                指令发送器接口 (CommandSender)                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐              ┌─────────────────┐       │
│  │  真实串口发送器  │              │  Mock发送器     │       │
│  │ (SerialSender)  │              │ (MockSender)   │       │
│  └─────────────────┘              └─────────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                   硬件层 (Hardware Layer)                   │
└─────────────────────────────────────────────────────────────┘
```

## 组件说明

### 核心接口

#### CommandSender

指令发送接口，抽象底层通信方式：

```java
public interface CommandSender {
    void sendCommand(byte[] command);

    void setOnResponseListener(OnResponseListener listener);

    boolean isConnected();

    void disconnect();
}
```

#### OnResponseListener

响应监听接口：

```java
public interface OnResponseListener {
    void onResponseReceived(byte[] response);

    void onError(String error);
}
```

#### OnCommandListener

指令执行监听接口：

```java
public interface OnCommandListener {
    void onSuccess();

    void onError(String error);
}
```

### 核心组件

#### CommandDebounceManager

防抖管理器，系统核心：

- 指令队列管理
- 执行时序控制
- 超时处理
- 响应匹配

#### CommandExecutionStrategy

指令执行策略：

- 定义不同指令的执行时间
- 计算动态超时时间
- 提供指令描述信息

#### ResponseMatcher

响应匹配器：

- 验证响应格式
- 匹配指令和响应
- 解析响应状态

#### QueuedCommand

队列指令项：

- 封装指令数据
- 管理执行状态
- 超时控制

### 发送器实现

#### LockCtlBoardSerialSender

真实串口发送器，用于生产环境：

- 串口通信管理
- 数据缓冲和分包处理
- 连接状态监控

#### MockCommandSender

Mock发送器，用于测试环境：

- 模拟真实响应延迟
- 支持错误模拟
- 详细的执行记录

## 使用方法

### 基本使用

#### 1. 初始化系统

```java
// 生产环境 - 使用真实串口
CommandSender sender = new LockCtlBoardSerialSender();
CommandDebounceManager debounceManager = new CommandDebounceManager(sender);

// 测试环境 - 使用Mock发送器
MockCommandSender mockSender = new MockCommandSender();
CommandDebounceManager debounceManager = new CommandDebounceManager(mockSender);
```

#### 2. 发送指令

```java
byte[] openLockCommand = LockCtlBoardCmdHelper.buildOpenSingleLockCommand((byte) 0x00, 1);

debounceManager.

sendCommand(openLockCommand, new OnCommandListener() {
    @Override
    public void onSuccess () {
        Log.i(TAG, "开锁成功");
    }

    @Override
    public void onError (String error){
        Log.e(TAG, "开锁失败: " + error);
    }
});
```

#### 3. 监控状态

```java
CommandDebounceManager.QueueStatus status = debounceManager.getStatus();
Log.

d(TAG, "队列状态: "+status.toString());
```

### 高级使用

#### 批量操作

```java
// 快速发送多个指令，自动防抖处理
for(int i = 1;
i <=5;i++){
byte[] command = LockCtlBoardCmdHelper.buildOpenSingleLockCommand((byte) 0x00, i);
    debounceManager.

sendCommand(command, listener);
}
```

#### 错误重试

```java
private void sendWithRetry(byte[] command, int maxRetries, int currentRetry) {
    debounceManager.sendCommand(command, new OnCommandListener() {
        @Override
        public void onSuccess() {
            Log.i(TAG, "操作成功");
        }

        @Override
        public void onError(String error) {
            if (currentRetry < maxRetries) {
                // 延迟重试
                new Handler().postDelayed(() -> {
                    sendWithRetry(command, maxRetries, currentRetry + 1);
                }, 1000);
            } else {
                Log.e(TAG, "操作失败，达到最大重试次数");
            }
        }
    });
}
```

## 指令执行时间配置

系统根据不同指令类型自动计算执行时间和超时时间：

| 指令类型    | 指令字  | 执行时间  | 超时时间  | 说明          |
|---------|------|-------|-------|-------------|
| 开单个锁    | 0x82 | 350ms | 700ms | 开单个门锁       |
| 同时开多锁   | 0x80 | 350ms | 700ms | 最多同时开2个锁    |
| 查询单个门状态 | 0x83 | 100ms | 200ms | 查询单个门锁状态    |
| 查询所有门状态 | 0x84 | 200ms | 400ms | 查询所有门锁状态    |
| 开全部锁    | 0x85 | 动态计算  | 动态×2  | 350ms × 锁数量 |
| 逐一开多锁   | 0x86 | 动态计算  | 动态×2  | 350ms × 锁数量 |
| 通道闪烁    | 0x81 | 100ms | 200ms | LED闪烁功能     |
| 通道常开    | 0x88 | 100ms | 200ms | 继电器持续打开     |
| 通道关闭    | 0x89 | 100ms | 200ms | 关闭继电器       |

## 测试

### 单元测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "*.CommandDebounceManagerTest"
```

### 集成测试

```bash
# 运行集成测试
./gradlew test --tests "*.LockControlIntegrationTest"
```

### 测试覆盖

- ✅ 基本指令执行
- ✅ 指令队列顺序
- ✅ 响应确认机制
- ✅ 超时处理
- ✅ 错误处理
- ✅ 队列状态监控
- ✅ 高频指令处理
- ✅ 混合操作场景
- ✅ 真实世界场景

## 性能特点

### 执行效率

- 基于响应确认，避免不必要的延时
- 指令完成后立即执行下一个
- 平均执行时间比固定延时减少30-50%

### 可靠性

- 双重保障：响应确认 + 超时控制
- 完整的错误处理和恢复机制
- 线程安全的队列管理

### 可扩展性

- 支持任意指令类型
- 可配置的执行参数
- 插件化的发送器架构

## 最佳实践

### 1. 错误处理

```java
debounceManager.sendCommand(command, new OnCommandListener() {
    @Override
    public void onSuccess () {
        // 处理成功情况
    }

    @Override
    public void onError (String error){
        // 记录错误日志
        Log.e(TAG, "指令执行失败: " + error);

        // 根据错误类型决定是否重试
        if (error.contains("超时")) {
            // 超时重试逻辑
        } else {
            // 其他错误处理
        }
    }
});
```

### 2. 资源管理

```java
// 在Activity/Service的onDestroy中清理资源
@Override
protected void onDestroy() {
    super.onDestroy();
    if (debounceManager != null) {
        debounceManager.shutdown();
    }
}
```

### 3. 状态监控

```java
// 定期检查队列状态
private void monitorQueueStatus() {
    CommandDebounceManager.QueueStatus status = debounceManager.getStatus();

    if (status.queueSize > 10) {
        Log.w(TAG, "指令队列过长: " + status.queueSize);
    }

    if (status.totalErrors > 0) {
        Log.w(TAG, "检测到错误: " + status.totalErrors);
    }
}
```

## 故障排除

### 常见问题

#### 1. 指令执行超时

**原因**: 硬件响应时间超过预期
**解决**:

- 检查硬件连接
- 调整超时参数
- 监控硬件状态

#### 2. 指令队列积压

**原因**: 发送指令速度过快
**解决**:

- 控制发送频率
- 增加队列容量监控
- 优化指令逻辑

#### 3. 响应匹配失败

**原因**: 响应格式错误或乱序
**解决**:

- 检查通信协议
- 验证硬件固件
- 增加响应日志

### 调试技巧

#### 1. 启用详细日志

```java
// 在测试环境启用详细日志
MockCommandSender mockSender = new MockCommandSender();
mockSender.

setDefaultResponseDelay(100); // 快速响应
```

#### 2. 监控队列状态

```java
// 定期打印队列状态
Timer timer = new Timer();
timer.

scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run () {
        Log.d(TAG, debounceManager.getStatus().toString());
    }
},0,1000);
```

#### 3. 模拟错误场景

```java
// 在测试中模拟错误
mockSender.setSimulateErrors(true);
mockSender.

setErrorRate(0.1); // 10%错误率
```

## 版本历史

### v1.0.0

- ✅ 基于响应确认的防抖机制
- ✅ 智能超时控制
- ✅ 完整的测试套件
- ✅ 生产环境支持

## 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 GitHub Issue
- 发送邮件至开发团队

---

**注意**: 本系统专为锁控板通信设计，使用前请确保了解相关硬件协议和操作规范。