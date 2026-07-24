# PSM config 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: ⚪ P3（系统配置 KV，5 文件）

---

## 1. 业务定位

### 1.1 解决什么问题

config 模块是 PSM 的**系统配置 KV 存储**：

- **配置项管理**：`system_config` 表（id/configName/configKey/configValue）
- **配置变更**：批量更新所有配置项（需前后 size 一致）
- **配置查询**：按 configKey 列表查询（供 alarm 模块读取音效 URI 等）

### 1.2 与其他模块的依赖关系

```
config ──→ alarm (AlarmRecordServiceImpl.sendAlarmSoundWsMessage)  # 读取音效 URI + 播放次数
```

---

## 2. 类清单（5 个 java）

### 2.1 mapper/ (1)
| DAO | 备注 |
|---|---|
| `SystemConfigDAO` | 默认 CRUD |

### 2.2 model/ (1)
| PO | 表 | 字段 |
|---|---|---|
| `SystemConfigPO` | `system_config` | id/configName/configKey/configValue/updateTime/createTime |

### 2.3 service/ (1) + service/imp/ (1)
| 接口 | 实现 | 责任 |
|---|---|---|
| `ISystemConfigService` | `SystemConfigServiceImpl` (38 行) | **⚪ P3** 简单 CRUD |

### 2.4 web/ (1)
| Controller | 端点 |
|---|---|
| `SystemConfigController` | `/web/system-config` (GET/PUT) |

---

## 3. 核心流程

### 3.1 批量更新配置

```
Web → SystemConfigController.chgSystemConfig(List<SystemConfigPO>)
  │
  └─→ SystemConfigServiceImpl.handleSystemConfigChg(form)
        │
        ├─→ list() → 获取当前所有配置（existConfig）
        │
        ├─→ if form == null OR existConfig.size() != form.size():
        │     └─→ return error 20601  (数量不一致，必须全量更新)
        │
        └─→ updateBatchById(form) → ok / error 20001
```

**⚠️ 限制**：必须**全量更新**，不能增量修改。前端必须先 GET 全部，修改后再 PUT 全部。

### 3.2 按 configKey 查询（被 alarm 模块调用）

```
AlarmRecordServiceImpl.sendAlarmSoundWsMessage(defectType)
  │
  └─→ systemConfigService.listByConfigKey([soundConfigKey, "sound_play_count"])
        │
        └─→ SELECT * FROM system_config WHERE config_key IN (...)
```

**已知 configKey**（从 alarm 模块推断）:
- `defect_alarm_sound_uri` / `system_alarm_sound_uri` / `device_alarm_sound_uri` — 音效 URI
- `sound_play_count` — 播放次数

---

## 4. 关键类逐个解析

### 4.1 ⚪ P3: `SystemConfigServiceImpl` (38 行)

**核心方法**:
```java
@Override BaseResult handleSystemConfigChg(List<SystemConfigPO> form)
@Override List<SystemConfigPO> listByConfigKey(List<String> configKeys)
```

### 4.2 ⚪ P3: `SystemConfigController` (37 行)

**端点**:
- `GET /web/system-config` — 获取全部配置
- `PUT /web/system-config` — 批量更新（私有方法 `private`，但 Spring MVC 仍能反射调用）

---

## 5. 数据库交互

### 5.1 涉及表（1 张）

| 表 | 用途 | 字段 | retention |
|---|---|---|---|
| `system_config` | KV 配置 | id/configName/configKey/configValue | 无 |

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `system_config` 表 | ✅ EdgeHost 已有 | W-A6 |
| 基础 CRUD | ✅ | W-A6 |

### 6.2 缺口

无（config 模块很简单）

---

## 7. 风险 / 注意点

### 7.1 ⚠️ 全量更新限制

`handleSystemConfigChg` 必须 `existConfig.size() == form.size()`，否则报错。意味着：
- 不能新增配置项（只能改值）
- 不能删除配置项
- 这是硬编码的 schema 保护，不是数据库约束

### 7.2 ⚠️ Controller `private` 方法

```java
@PutMapping
private BaseResult chgSystemConfig(...)
```

`private` 方法被 Spring MVC 调用是可行的（Spring 用反射绕过访问修饰符），但**不符合规范**。可能是开发笔误。

### 7.3 配置值类型

`configValue` 是 String，但 alarm 模块用 `Integer.parseInt(sortConfigByKey.get("sound_play_count").getConfigValue())` 强转 Integer。如果配置了非数字值会抛 `NumberFormatException`。

### 7.4 configKey 约束

没有 UNIQUE 约束到 `config_key`，可能存在重复 config_key 的风险（实际不会，因为 handleSystemConfigChg 限制全量更新）。

---

## 8. 总结

config 模块是 PSM 系统配置，5 文件，主要责任：
1. KV 配置存储
2. 全量更新（不支持增删）
3. 按 configKey 批量查询（被 alarm 调用）

EdgeHost 已有等价功能，无需额外移植。
