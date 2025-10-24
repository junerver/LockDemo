# LockDemo - Android云川智能锁控板通信程序

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

一个用于Android设备通过串口与云川智能锁控板通信的演示程序。该应用提供了完整的门锁控制功能，支持单独和批量操作、LED控制、状态监控等特性。

## 项目概述

LockDemo是一个基于Kotlin和Java开发的Android门锁控制应用，专为平板设备优化设计。应用通过RS485串口与门锁控制板通信，实现了全面的门锁管理功能，包括实时数据响应、多种操作模式等。

## 主要功能

### 🔐 锁控功能

- **独立控制**：支持1-7号门锁的独立开启操作
- **批量操作**：支持同时开启多个门锁（最多2个）
- **顺序开启**：支持按顺序逐个开启门锁
- **全部门锁**：一键开启所有门锁功能
- **防抖处理**：支持指令防抖机制，避免重复操作和设备冲突

### 💡 LED控制

- 各通道独立的LED闪烁控制
- 可视化状态指示

### 📊 状态监控

- 实时查询单个门锁状态
- 批量查询所有门锁状态
- 状态实时显示与更新

### 🔗 通道管理

- 通道保持开启功能
- 通道关闭操作
- 支持定时控制

### 📱 用户界面

- 专为平板设备优化的界面设计
- 多Activity架构，功能模块分离
- 实时响应数据显示区域
- 自动串口连接
- 设备自检功能
- 防抖测试界面

## 技术特性

### 通信协议

- **串口通信**：RS485，波特率9600
- **设备路径**：自动探测或指定路径（默认/dev/ttyS4）
- **数据格式**：二进制协议，带XOR校验和
- **响应格式**：JSON结构化数据
- **自动探测**：支持串口设备自动检测和连接

### 锁控板协议支持

应用实现了完整的锁控板协议（指令0x80-0x89）：

| 指令   | 功能        | 描述              |
|------|-----------|-----------------|
| 0x80 | 批量同时开锁    | 同时开启多个门锁        |
| 0x81 | LED闪烁控制   | 控制指定通道LED闪烁     |
| 0x82 | 单个开锁      | 开启指定门锁          |
| 0x83 | 查询单锁状态    | 查询指定门锁状态        |
| 0x84 | 查询所有状态    | 批量查询所有门锁状态      |
| 0x85 | 主动上传门状态变化 | 锁控板主动上报门锁状态变化    |
| 0x86 | 顺序开锁全部    | 按顺序开启所有门锁       |
| 0x87 | 顺序开锁多个    | 按顺序开启多个门锁       |
| 0x88 | 通道保持开启    | 保持指定通道开启状态      |
| 0x89 | 关闭通道      | 关闭指定通道          |

### 数据处理

- **数据分片支持**：处理不完整的串口数据包
- **线程安全操作**：并发访问的同步数据处理
- **JSON响应解析**：结构化响应数据解析与错误处理
- **校验和验证**：XOR数据完整性验证

## 项目架构

### 核心组件

- **MainActivity.kt**：主活动，串口连接管理和综合锁控UI
- **SequentialOpenActivity.kt**：顺序/同时开锁活动，支持模式切换
- **LedFlashActivity.kt**：LED控制活动，独立通道LED操作
- **StatusQueryActivity.kt**：状态查询活动，门锁状态监控
- **DebounceTestActivity.kt**：防抖测试活动，指令防抖功能测试
- **DeviceCheckActivity.kt**：设备自检活动，系统状态检查
- **LockCtlBoardUtil.java**：核心串口通信和锁控工具（单例模式，支持防抖）
- **LockCtlBoardCmdHelper.java**：命令构建和响应解析助手
- **SerialPortDetector.java**：串口设备自动检测工具
- **CommandDebounceManager.java**：指令防抖管理器
- **LockCtlBoardSerialSender.java**：串口指令发送器

### 界面设计

- **主界面**：4列按钮网格，集成响应显示区域，包含通道持续打开和关闭功能
- **顺序开锁界面**：模式切换和多选功能
- **LED控制界面**：独立通道LED控制按钮
- **状态查询界面**：实时门锁状态监控和显示
- **防抖测试界面**：指令防抖机制测试和验证
- **设备自检界面**：系统状态全面检查和诊断
- **平板优化**：高效空间利用的布局设计

## 开发环境

- **编译SDK**：35
- **最低SDK**：24
- **目标SDK**：35
- **Kotlin版本**：2.0.21
- **Java兼容性**：版本11
- **应用ID**：xyz.junerver.android.lockdemo

## 依赖库

- **AndroidX Core KTX**、AppCompat、Activity
- **Material Design**组件
- **串口通信库**：com.kongqw.serialportlibrary
- **JSON解析库**：com.google.code.gson:gson
- **标准测试库**：JUnit、Espresso

## 安装与使用

### 系统要求

- Android 7.0（API 24）或更高版本
- 支持串口通信的Android设备
- 建议使用平板设备以获得最佳体验

### 编译安装

1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 连接Android设备或启动模拟器
4. 点击运行或生成APK文件

### 使用说明

1. **启动应用**：应用启动后会自动检测并连接串口设备
2. **主界面操作**：
   - 点击数字按钮控制对应门锁（1-7号锁）
    - 使用批量操作按钮控制多个门锁
   - 通道持续打开/关闭功能
    - 查看实时响应数据显示区域
   - 长按连接按钮可强制重新初始化
3. **功能模块**：
   - **依次开锁**：顺序或同时开锁模式切换
   - **LED闪烁**：各通道独立LED控制
   - **状态查询**：实时门锁状态监控
   - **并发防抖**：指令防抖机制测试
   - **设备自检**：系统状态全面检查
4. **状态监控**：实时查看门锁状态、连接状态和响应信息
5. **串口管理**：
   - 支持自动串口探测
   - 支持手动指定串口路径
   - 连接状态实时显示

## 测试

项目包含完整的单元测试，覆盖所有锁控指令：

- **指令构建测试**：验证所有指令（0x80-0x89）的字节级构建
- **响应解析测试**：测试所有支持响应类型的JSON解析
- **校验和测试**：验证XOR校验和计算和验证
- **边界条件测试**：测试边界条件和错误场景

运行测试：

```bash
./gradlew test
```

## 开源许可

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个Pull Request

## 技术支持

如有问题或建议，请通过以下方式联系：

- 提交GitHub Issue
- 发送邮件至项目维护者

## 核心API说明

### LockCtlBoardUtil 主要方法

#### 初始化和连接管理

```java
// 自动检测并初始化
LockCtlBoardUtil.getInstance().

initialize(context);

// 指定串口路径初始化
LockCtlBoardUtil.

getInstance().

initialize(context, portPath);

// 强制重新初始化
LockCtlBoardUtil.

getInstance().

forceReinitialize(context);

// 设置防抖模式
LockCtlBoardUtil.

getInstance().

setUseDebounce(true);
```

#### 锁控操作

```java
// 开启单个门锁
boolean openSingleLock(int channelId);

// 同时开启多个门锁（最多2个）
boolean openMultipleLocksSimultaneously(int... lockIds);

// 顺序开启多个门锁
boolean openMultipleLocksSequentially(int... lockIds);

// 开启所有门锁
boolean openAllLocksSequentially();

// LED闪烁控制
boolean flashLockLed(int channelId);

// 通道持续打开（适用于继电器、灯具等）
boolean keepChannelOpen(int channelId);

// 关闭通道
boolean closeChannel(int channelId);
```

#### 状态查询

```java
// 查询单个门锁状态
boolean getSingleLockStatus(int channelId);

// 查询所有门锁状态
boolean getAllLocksStatus();
```

#### 串口管理

```java
// 检查串口连接状态
boolean isSerialPortOpen();

// 检查初始化状态
boolean isInitialized();

// 获取已保存的串口路径
String getSavedPortPath();

// 清除已保存的串口路径
void clearSavedPortPath();
```

### 事件监听

#### 连接状态监听

```java
LockCtlBoardUtil.getInstance().setOnDataReceived(new LockCtlBoardUtil.OnDataReceived() {
    @Override
    public void onDataReceived(String json) {
        // 处理响应数据，包括连接状态通知和设备响应
        // 连接状态通知格式：{"type":"connection","event":"connect_successes","message":"..."}
    }
});
```

## 更新日志

### v1.1.0

- **防抖测试界面**：添加指令防抖机制测试和验证
- **响应数据优化**：改进JSON响应解析和显示
- **错误处理增强**：提升异常情况的处理能力
- **设备自检功能**：添加系统状态全面检查和诊断功能
- **串口自动探测**：支持自动检测串口设备路径
- **连接状态优化**：改进连接管理和错误处理机制

### v1.0.0

- 初始版本发布
- 实现基本的串口通信功能
- 支持所有锁控板协议指令（0x80-0x89）
- 优化平板设备界面
- 完整的单元测试覆盖

---

**注意**：本演示程序仅用于与云川智能锁控板的通信展示，实际部署时请确保设备兼容性和安全性。