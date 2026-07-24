# W-X21 — alarm.global-enabled 全局开关 — 完工报告

- **任务编号**: W-X21 (2026-07-23)
- **派工人**: PM 锋卫 14:25
- **执行人**: Worker W-X21（subagent, depth 1/1）
- **开始**: 14:25
- **完工**: 14:31（约 6 min）

---

## 1. 改动文件清单

### 1.1 `DataupLoad\config\application-prod.yml`

在 `alarm:` 段顶部新增 `global-enabled: true` 配置项，并加注释说明 W-X21 语义：

```yaml
# 2026-07-23 PM 锋卫派 W-X21：alarm.global-enabled 全局开关（紧急可一键关停）
# - global-enabled=true（默认）：报警正常落 PG + 走 sendAlarmMessage 全链路
# - global-enabled=false：AlarmRecordServiceImpl.add() 入口即 return BaseResult.build().ok()
#   报警既不落 PG 也不推 yk / WebSocket；用于老板紧急关停（老板指令时 PM 派工单改 false）
alarm:
  global-enabled: true
  config:
    - type: defect
      template: (?<=\[)[^]]+(?=\])
    - type: system
      template: ^([^。]*)
    - type: device
      template: ^([^。]*)
```

### 1.2 `DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\config\DefectAlarmConfig.java`

新增字段 `globalEnabled`（默认 `true`）+ getter/setter：

```java
@ConfigurationProperties("alarm")
public class DefectAlarmConfig {
   /** W-X21 全局开关（默认 true）。false 时报警入口直接 return */
   private boolean globalEnabled = true;

   public boolean isGlobalEnabled() {
      return this.globalEnabled;
   }

   public void setGlobalEnabled(boolean globalEnabled) {
      this.globalEnabled = globalEnabled;
   }

   // ... existing config + DefectTypeConfig unchanged
}
```

由于该类已标注 `@ConfigurationProperties("alarm")`，Spring Boot 自动将配置项 `alarm.global-enabled` 注入到 `globalEnabled` 字段（Kebab-case → camelCase 自动映射）。

### 1.3 `DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.java`

`add(AlarmDTO)` 方法入口处加全局开关判断：

```java
@Override
public BaseResult add(AlarmDTO form) {
   if (!this.alarmConfig.isGlobalEnabled()) {
      log.warn("alarm global disabled, skip.[form={}]", form);
      return BaseResult.build().ok();
   }
   // ... 原有逻辑不变
}
```

该判断在 `alarmType` 判 null 之前执行，当 `global-enabled=false` 时直接 return，**报警既不落 PG 也不推 yk / WebSocket**。

---

## 2. 编译结果

### 2.1 命令

```
E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe -encoding UTF-8
  -cp "E:\DEMO\数据采集\DataupLoad\lib\*;E:\DEMO\数据采集\DataupLoad\target\classes"
  -d E:\DEMO\数据采集\DataupLoad\target\classes
  E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.java
  E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\config\DefectAlarmConfig.java
```

### 2.2 输出

| 项目 | 结果 |
|------|------|
| ExitCode | **0** |
| stdout | (空) |
| stderr | (空) |
| 编译错误 | **0** |
| 编译警告 | **0** |

### 2.3 字节码验证

```java
// DefectAlarmConfig.class
private boolean globalEnabled;
public boolean isGlobalEnabled();
public void setGlobalEnabled(boolean);

// AlarmRecordServiceImpl.class add() 字节码开头:
//  aload_0 → getfield #26 (alarmConfig)
//  invokevirtual #32 (DefectAlarmConfig.isGlobalEnabled():Z)
//  ifne 28          ← if true, skip to AlarmTypeEnum.getByCode
//  否则：ldc "alarm global disabled, skip.[form={}]"
//        invokeinterface Logger.warn
//        invokestatic BaseResult.build()
//        invokevirtual BaseResult.ok()
//        areturn     ← early return
```

### 2.4 hik-java 运行状态

编译过程中 `hik-java.exe` (PID 33248) 正常运行，**未触发任何重启**。

---

## 3. 使用方式

### 3.1 正常情况（默认）

```yaml
alarm:
  global-enabled: true   # 报警全链路正常
```

### 3.2 紧急关停（老板指令时 PM 改）

```yaml
alarm:
  global-enabled: false  # 报警入口即 return，不落库不推送
```

### 3.3 恢复

改回 `global-enabled: true` 并重启 hik-java（PM 重启）。

---

## 4. 约束遵守

| 约束 | 状态 |
|------|------|
| ❌ 重启 hik-java | ✅ 未触发 |
| ❌ 改 yk 配置 | ✅ 未碰 yk 段 |
| ❌ 改其它业务代码 | ✅ 仅改 alarm 模块 3 个文件 |
| ❌ 验证运行时 | ✅ 未启动/重启任何进程 |

---

## 5. 验收命令

```powershell
# 1. yml 中有 global-enabled: true
Select-String -Path E:\DEMO\数据采集\DataupLoad\config\application-prod.yml -Pattern 'global-enabled'
# → 期望：至少 1 hit

# 2. Java 源码中有 isGlobalEnabled
Select-String -Path E:\DEMO\数据采集\DataupLoad\src -Pattern 'isGlobalEnabled' -SimpleMatch
# → 期望：DefectAlarmConfig.java (定义) + AlarmRecordServiceImpl.java (调用)

# 3. .class 中有 isGlobalEnabled
E:\DEMO\数据采集\DataupLoad\jdk\bin\javap -p E:\DEMO\数据采集\DataupLoad\target\classes\com\hikrobotics\solution\module\alarm\config\DefectAlarmConfig.class
# → 期望：输出含 isGlobalEnabled() / setGlobalEnabled(boolean)
```

---

## 6. 产出物

1. `DataupLoad\config\application-prod.yml` — 配置项（+ 注释）
2. `DataupLoad\src\main\java\com\...\config\DefectAlarmConfig.java` — `globalEnabled` 字段 + getter/setter
3. `DataupLoad\src\main\java\com\...\service\impl\AlarmRecordServiceImpl.java` — `add()` 入口守卫
4. `DataupLoad\target\classes\com\...\config\DefectAlarmConfig.class` — 新编译字节码
5. `DataupLoad\target\classes\com\...\service\impl\AlarmRecordServiceImpl.class` — 新编译字节码
6. `docs\delivered\2026-07-23-W-X21-global-switch-result.md` — 本报告
