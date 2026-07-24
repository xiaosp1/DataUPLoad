# ADR-0011 — alarm sound 服务端零节流（前端约定）

**状态**: 已确认（PSM + DataupLoad 行为一致）
**日期**: 2026-07-24 21:35 (W-X30 W-ALM-06 工单)
**决策人**: 锋卫 + W-ALM-06 worker

## 背景
W-X28 P1 冲刺 W-ALM-05 工单：
> 实装 `sendAlarmMessage` WS 推送 + `sendAlarmMessage` 声音 + AlarmConstants

当时派工描述有"声音间隔 `SOUND_PLAY_INTERVAL` 是否生效"的疑问。

W-X30 W-ALM-06 端到端测试发现：

### 实测数据
- **测试时长**: 8m21s
- **alarm 推送数**: 414 条（414/414 alarm + sound 配对成功）
- **sound 推送数**: 414 条
- **sound 间隔分布**:
  - p50 = 0.92s
  - p99 = ?
  - max = 8.99s
  - **0/413 ≥ 30s**（没有任何两条 sound 间隔超过 30s）
- **手动单次 HTTP 触发**：`GET /web/alarm/num` 21:44:01.917 → `sound playCount=78` 21:44:02.144（200ms 延迟）

### 关键发现
**`sendAlarmSoundWsMessage` 服务端代码 100% 没有节流逻辑。** 每次报警都会立即推送 `playSound` 消息。

## 逆向验证 PSM

```java
// PSM AlarmRecordServiceImpl.java 第 139-140 行
@Value(value="${alarm.interval:60}")
private Integer alarmInterval;   // ← 只读配置，不用于节流
```

```java
// PSM AlarmRecordServiceImpl.java 第 333-339 行
ArrayList configKeys = Lists.newArrayList((Object[])new String[]{type.getSoundConfigKey(), "sound_play_count"});
this.systemConfigService.listByConfigKey(configKeys).forEach(config -> sortConfigByKey.put(config.getConfigKey(), config));
if (CollectionUtils.isNotEmpty(sortConfigByKey)) {
    PlaySoundWsMsgDTO msg = new PlaySoundWsMsgDTO()
        .setPlayCount(parseInt(...))
        .setUri(...);
    wsData.data(msg);
    this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");  // ← 立即广播，无节流
}
```

**PSM 自身也没有服务端节流！**

## 决策
**DataupLoad 沿用 PSM 行为：服务端不节流，由前端 debounce。**

## 理由
1. **1:1 对齐 PSM** 是项目核心策略
2. **前端 debounce 更合理**：单客户端单浏览器窗口，多客户端各自节流
3. **服务端节流的副作用**：多客户端登录时，A 客户端报警，B 客户端听不到 — 不是用户期望
4. **`SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5`** 是发给前端的 WS 消息载荷的一部分，前端按此值 debounce

## 影响
- 服务端代码无需修改
- 前端开发需要按 `playCount` + `interval` 字段做客户端 debounce（不是 server 责任）
- W-ALM-05 当时加的 `AlarmConstants.SOUND_PLAY_DEFAULT_INTERVAL_SECONDS` 常量保留，用于给前端提供默认值

## 教训
- "声音间隔"是 PSM 的**前端约定**，不是 server 责任 — 工单设计时容易误判
- 端到端测试能发现这类"按 brief 字面理解会做错"的决策点
- "服务端节流" vs "前端节流" 的边界判断需要先看 PSM 实际实现

## 历史工单
- W-X28 W-ALM-05: 实现 sendAlarmMessage + AlarmConstants（未做端到端测试）
- W-X30 W-ALM-06: 端到端 WS 测试发现本事实

## 跟进项
- 前端开发文档：明确说明"声音 debounce 由前端实现"
- 未来如确需服务端节流，单独开 W-ALM-07 工单（不应混入 PSM 1:1 路径）
