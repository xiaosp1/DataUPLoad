# W-X15b — 还原 ignore_alarm 测试数据 + 修 entity 字段类型翻车（🟡 P1）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 15:35
**优先级**：🟡 P1（W-X15a 跑后清理导致 ignore_alarm 现有数据全清；entity 字段类型被 W-X15a Worker 擅自改）
**基于工单**：W-X15a 完工 + PM 翻车承认

---

## 🎓 PM 翻车承认（铁则 49 立）

**W-X15a 工单 §3 "IgnoreAlarm entity 加 ignoreAll/faceId"** 没明示 endTime/startTime 字段类型是否可改。

W-X15a Worker 主动把 `endTime / startTime` 从 `LocalDateTime` 改成 `String`：
- ✅ 后续代码（DTO/Service）一致改 String
- ✅ grep 验证下游无破坏（AlarmRecordServiceImpl / yk / WS 不直接读 IgnoreAlarm.endTime）
- ❌ **PM 工单没说允许改 entity 字段类型**
- ❌ **PM 翻车：工单设计时没说"不允许改 entity 字段类型"**

**铁则 49（新立）**：Worker 改 entity 字段类型必须经 PM 单独授权。

---

## 📋 任务清单（2 项）

### 1. 评估 entity 字段类型是否回滚到 LocalDateTime

**当前现状**：
- `IgnoreAlarm.endTime` = String（与 DB varchar(19) 直接对齐）
- `IgnoreAlarm.startTime` = String（同上）
- `IgnoreAlarmDTO.endTime` = String
- `IgnoreAlarmServiceImpl.handleAlarmIgnore()` 用 `form.getEndTime()` 字符串直传

**两种方案**：

**方案 A（保持 W-X15a）**：
- 优点：与 DB 100% 对齐，MP 查询无需任何转换
- 缺点：与 PSM entity 字段类型不一致（PSM 是 LocalDateTime）
- 缺点：未来若 DB 改 timestamp 类型，所有 ignore 业务代码要回滚

**方案 B（回滚 LocalDateTime）**：
- 优点：与 PSM 1:1 对齐，未来扩展兼容
- 优点：保留 PM 工单原意"只加字段，不改类型"
- 缺点：handleAlarmIgnore / isIgnore 需要 LocalDateTime ↔ String 转换

**PM 建议：方案 B（回滚 LocalDateTime）**——理由：
- W-X15a Worker 是越权改的（铁则 49）
- PSM 端是 LocalDateTime，DB schema 升级到 timestamp 是早晚的事
- 转换逻辑只在 3 处加，不复杂

### 2. 执行回滚（如果选 B）

#### 2.1 改 IgnoreAlarm.java
```java
// endTime / startTime 改回 LocalDateTime
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@TableField("start_time")
private LocalDateTime startTime;  // 改回 LocalDateTime

@TableField("end_time")
private LocalDateTime endTime;    // 改回 LocalDateTime

// 加一个辅助方法（PM 审批后由 Worker 加）：
public IgnoreAlarm setStartTimeByString(String s) {
   if (s == null || s.isEmpty()) return this;
   this.startTime = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
   return this;
}
```

#### 2.2 改 IgnoreAlarmServiceImpl.handleAlarmIgnore()
- 用 setStartTimeByString / setEndTimeByString 转换
- 非空校验保留

#### 2.3 改 IgnoreAlarmServiceImpl.isIgnore() 和 getIgnoreDefect()
- 保留字符串比较（varchar < timestamp 必报错）—— 这部分**不需要回滚**

#### 2.4 编译验证（0 错 0 警告）

#### 2.5 字节码验证：
- IgnoreAlarm.endTime 类型 = LocalDateTime（javap 看字段签名）
- IgnoreAlarmServiceImpl.handleAlarmIgnore() 看到 setStartTimeByString + setEndTimeByString 调用

#### 2.6 单元测试：复用 W-X15a 的 W_X15a_Test.java，INSERT → isIgnore(true) → DELETE

### 3. 还原 ignore_alarm 数据（PM 工单要的 3 条 W-X15 测试数据）

**W-X15 测试时插了 3 条数据**（PM 截图：W-X15-L4/FA4 + W-X15-L5/FA5 + xx/LL/FF）被 W-X15a Worker 单元测试清理掉了。

**还原方案**：W-X15b 在单元测试完成后 INSERT 1 条 W-X15 风格测试数据（保留痕迹），但 INSERT 时标记 `create_time = 2026-07-23` 注释。

```sql
INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, end_time, create_time, update_time)
VALUES ('W-X15-restore', 1, 'L-restore', 'F-restore', '2099-12-31 23:59:59', NOW(), NOW())
RETURNING id;
```

报告里写明：W-X15a 单元测试清理了 ignore_alarm 表（PM 翻车承认），W-X15b 恢复 1 条做痕迹。

### 4. 写报告
`docs/delivered/2026-07-23-W-X15b-restore-entity.md`，必须包含：
- 改动前后 diff（entity 字段类型 + service 转换）
- 编译结果（命令 + 输出）
- 字节码 `javap -p` 截图（确认 endTime 类型是 LocalDateTime）
- 单元测试 psql 模拟证据
- ignore_alarm 现状（应 1 条 W-X15-restore 痕迹）
- **不重启 hik-java**（PM 决策）

---

## 🚫 严禁

- ❌ 重启 hik-java PID 33248
- ❌ 改 yml
- ❌ 改 uploadEnabled / loginEnabled / global-enabled
- ❌ 改其它业务代码
- ❌ 改 AlarmRecordServiceImpl（不在本工单范围）

---

## 🎯 PM 验收标准（铁则 40/41/49）

1. ✅ `IgnoreAlarm.endTime` 类型 = `LocalDateTime`（PM 翻车纠正）
2. ✅ `IgnoreAlarmServiceImpl.isIgnore/getIgnoreDefect` 字符串比较保留（W-X15a 修复）
3. ✅ `handleAlarmIgnore` 用转换方法（setEndTimeByString）写库
4. ✅ javac 编译 0 错 0 警告
5. ✅ 字节码 `javap -p` 显示 `endTime` 类型是 `Ljava/time/LocalDateTime;`
6. ✅ 单元测试 PASS（INSERT→isIgnore(true)→DELETE）
7. ✅ ignore_alarm 还原 1 条 W-X15-restore 痕迹
8. ✅ 报告完整（含 PM 翻车承认 + diff + 编译 + 字节码 + 单元测试）

完成在群内回复。
