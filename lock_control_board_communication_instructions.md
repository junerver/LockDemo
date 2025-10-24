# LockDemo 前端通信接口文档

## 文档说明

本文档描述了 LockDemo Android 应用与前端应用的通信接口规范，包括门锁控制、状态查询等功能的 API 接口。

## 通用说明

### 通信协议

- **数据格式：** JSON
- **字符编码：** UTF-8
- **响应方式：** 回调通知

### 状态码说明

| 状态码 | 说明 |
|-----|----|
| 0   | 成功 |
| 255 | 失败 |

### 锁状态值说明

| 状态值 | 说明    |
|-----|-------|
| 0   | 打开    |
| 1   | 关闭    |
| 255 | 失败/未知 |

### 通用响应字段

所有接口响应都包含以下通用字段：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，用于标识不同的操作 |
| `status` | Integer | 指令执行状态，0=成功，255=失败 |
| `message` | String | 操作结果描述信息 |

---

## 对外提供的接口

### 1. 同时打开多个锁（0x80）

**指令码：** 0x80

**接口名称：** `openMultipleLocksSimultaneously`

**功能描述：** 同时打开多个指定通道的门锁

**请求参数：**

- `channelNos` (int[]): 通道编号数组 (1-7)

**响应数据：**

```json
{
  "commandType": "open_multiple_locks",
  "message": "同时开多锁操作成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "open_multiple_locks" |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

### 2. 通道LED闪烁（0x81）

**指令码：** 0x81

**接口名称：** `flashChannelLED`

**功能描述：** 控制指定通道的LED进行闪烁

**请求参数：**

- `channelNo` (int): 通道编号 (1-7)

**响应数据：**

```json
{
  "commandType": "flash_channel",
  "channelNo": 1,
  "message": "通道1 LED闪烁成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "flash_channel" |
| `channelNo` | Integer | LED闪烁的通道编号 |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

### 3. 开单个锁（0x82）

**指令码：** 0x82

**接口名称：** `openSingleLock`

**功能描述：** 打开指定通道的门锁

**请求参数：**

- `channelNo` (int): 通道编号 (1-7)

**响应数据：**

```json
{
  "channelStatus": {
    "channelNo": 1,
    "isLocked": false,
    "lockStatus": 0
  },
  "channelNo": 1,
  "commandType": "open_single_lock",
  "message": "通道1开锁成功，锁状态：打开",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "open_single_lock" |
| `channelNo` | Integer | 上位机指定的通道编号 |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |
| `channelStatus` | Object | 通道状态详细信息 |
| `channelStatus.channelNo` | Integer | 通道编号 |
| `channelStatus.isLocked` | Boolean | 锁状态，true=关闭，false=打开 |
| `channelStatus.lockStatus` | Integer | 锁状态值，0=打开，1=关闭，255=失败 |

---

### 4. 查询指定通道门锁状态（0x83）

**指令码：** 0x83

**接口名称：** `getSingleLockStatus`

**功能描述：** 查询指定通道的门锁状态

**请求参数：**

- `channelNo` (int): 通道编号 (1-7)

**响应数据：**

```json
{
  "channelStatus": {
    "channelNo": 1,
    "isLocked": false,
    "lockStatus": 0
  },
  "channelNo": 1,
  "commandType": "get_single_lock_status",
  "message": "查询通道1状态成功，锁状态：打开",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "get_single_lock_status" |
| `channelNo` | Integer | 查询的通道编号 |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |
| `channelStatus` | Object | 通道状态详细信息 |
| `channelStatus.channelNo` | Integer | 通道编号 |
| `channelStatus.isLocked` | Boolean | 锁状态，true=关闭，false=打开 |
| `channelStatus.lockStatus` | Integer | 锁状态值，0=打开，1=关闭，255=失败 |

---

### 5. 查询当前锁控板全部通道状态（0x84）

**指令码：** 0x84

**接口名称：** `getAllLocksStatus`

**功能描述：** 查询锁控板所有通道的门锁状态

**请求参数：** 无

**响应数据：**

```json
{
  "channelCount": 24,
  "channelStatus": [
    {
      "channelNo": 1,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 2,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 3,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 4,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 5,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 6,
      "isLocked": true,
      "lockStatus": 1
    },
    {
      "channelNo": 7,
      "isLocked": true,
      "lockStatus": 1
    }
    // ...... 后面省略其他通道
  ],
  "commandType": "get_all_locks_status",
  "message": "查询全部门锁状态",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "get_all_locks_status" |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |
| `channelCount` | Integer | 通道总数 |
| `channelStatus` | Array | 所有通道状态数组 |
| `channelStatus[].channelNo` | Integer | 通道编号 |
| `channelStatus[].isLocked` | Boolean | 锁状态，true=关闭，false=打开 |
| `channelStatus[].lockStatus` | Integer | 锁状态值，0=打开，1=关闭，255=失败 |

---

### 6. 锁状态主动上报（0x85）

**指令码：** 0x85

**功能描述：** 锁控板主动上报门锁状态变化（当门锁打开或关闭时自动触发）

**请求参数：** 无（锁控板主动触发）

**响应数据：**

```json
{
  "commandType": "status_upload",
  "channelNo": 1,
  "channelStatus": {
    "channelNo": 1,
    "isLocked": false,
    "lockStatus": 0
  },
  "message": "通道1状态变化：打开"
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "status_upload" |
| `channelNo` | Integer | 状态变化的通道编号 |
| `channelStatus` | Object | 变化的通道状态信息 |
| `channelStatus.channelNo` | Integer | 通道编号 |
| `channelStatus.isLocked` | Boolean | 当前锁状态，true=关闭，false=打开 |
| `channelStatus.lockStatus` | Integer | 锁状态值，0=打开，1=关闭，255=失败 |
| `message` | String | 状态变化描述信息 |

---

### 7. 顺序打开全部锁（0x86）

**指令码：** 0x86

**接口名称：** `openAllLocksSequentially`

**功能描述：** 逐次打开所有通道连接的门锁

**请求参数：** 无

**响应数据：**

```json
{
  "commandType": "open_all_locks",
  "message": "开全部锁操作成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "open_all_locks" |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

### 8. 顺序打开指定多个通道的锁（0x87）

**指令码：** 0x87

**接口名称：** `openMultipleLocksSequentially`

**功能描述：** 按顺序逐个打开指定通道的门锁

**请求参数：**

- `channelNos` (int[]): 通道编号数组

**响应数据：**

```json
{
  "commandType": "open_multiple_sequential",
  "message": "逐一开多锁操作成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "open_multiple_sequential" |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

### 9. 通道保持开启（0x88）

**指令码：** 0x88

**接口名称：** `keepChannelOpen`

**功能描述：** 保持指定通道开启状态一段时间

**请求参数：**

- `channelNo` (int): 通道编号 (1-7)
- `duration` (long): 保持开启时间（毫秒）

**响应数据：**

```json
{
  "commandType": "channel_keep_open",
  "channelNo": 1,
  "message": "通道1持续打开操作成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "channel_keep_open" |
| `channelNo` | Integer | 保持开启的通道编号 |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

### 10. 关闭通道（0x89）

**指令码：** 0x89

**接口名称：** `closeChannel`

**功能描述：** 关闭指定通道

**请求参数：**

- `channelNo` (int): 通道编号 (1-7)

**响应数据：**

```json
{
  "commandType": "close_channel",
  "channelNo": 1,
  "message": "通道1关闭成功",
  "status": 0
}
```

**字段说明：**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| `commandType` | String | 指令类型，固定为 "close_channel" |
| `channelNo` | Integer | 关闭的通道编号 |
| `status` | Integer | 指令执行状态，0表示成功，255表示失败 |
| `message` | String | 操作结果描述信息 |

---

## 附录

### 接口汇总

| 指令码  | 序号 | 接口名称                              | 功能描述          |
|------|----|-----------------------------------|---------------|
| 0x80 | 1  | `openMultipleLocksSimultaneously` | 同时打开多个锁       |
| 0x81 | 2  | `flashChannelLED`                 | 通道LED闪烁       |
| 0x82 | 3  | `openSingleLock`                  | 开单个锁          |
| 0x83 | 4  | `getSingleLockStatus`             | 查询指定通道门锁状态    |
| 0x84 | 5  | `getAllLocksStatus`               | 查询当前锁控板全部通道状态 |
| 0x85 | 6  | `status_upload`                   | 锁状态主动上报       |
| 0x86 | 7  | `openAllLocksSequentially`        | 顺序打开全部锁       |
| 0x87 | 8  | `openMultipleLocksSequentially`   | 顺序打开指定多个通道的锁  |
| 0x88 | 9  | `keepChannelOpen`                 | 通道保持开启        |
| 0x89 | 10 | `closeChannel`                    | 关闭通道          |

### 注意事项

1. **通道编号范围：** 支持的通道编号为 1-7
2. **异步处理：** 所有操作都是异步执行，结果通过回调返回
3. **状态监控：** 建议定期查询门锁状态以确保数据准确性
4. **错误处理：** 前端应根据 `status` 字段判断操作是否成功，失败时可参考 `message` 字段获取错误信息



 