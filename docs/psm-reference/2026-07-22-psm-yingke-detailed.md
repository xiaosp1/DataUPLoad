# PSM yingke (鹰科) 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: 🟡 P1（外部系统集成，独立模块）

---

## 1. 业务定位

### 1.1 解决什么问题

yingke 模块是 PSM 与**鹰科（外部视觉检测平台）**的集成层：

- **认证管理**：定时登录鹰科获取 ticket（基于 `loginInterval` 自动续期）
- **数据查询**：提供产线/缺陷字典查询（`/client/yk/line-defect`）+ 缺陷记录分时查询（`/client/yk/defect-record`）
- **报警推送**：监听 `PushAlarmEvent`，将缺陷报警异步推送给鹰科
- **配置化开关**：`yk.enable=false` 时整个模块不工作

### 1.2 与其他模块的依赖关系

```
yingke ──→ detect (IDefectRecordService / ILineDayRecordService)  # 数据查询
yingke ──→ line   (ILineService)                                  # 产线字典
yingke ──→ alarm  (AlarmRecordDAO / IDefectTypeService)            # 报警推送 + 缺陷字典
yingke ──→ framework (HikWebClient)                               # HTTP 客户端
yingke ──→ alarm  (PushAlarmEvent)                                # 报警事件订阅
```

---

## 2. 类清单（15 个 java）

### 2.1 config/ (1)
| 类 | 配置前缀 | 字段 |
|---|---|---|
| `YKConfig` | `@ConfigurationProperties("yk")` | enable / url / username / password / loginInterval / workshop / searchRemove |

### 2.2 dto/ (10)
| 类别 | DTO |
|---|---|
| 通用请求 | `YKRequestDTO<Params>` / `ListParamsDTO<T>` / `StringParamDTO` |
| 响应 | `YKResponseDTO<Context>` / `ContextDTO` |
| 业务 | `AlarmDTO` / `DetectDataDTO` (+DefectDataDTO/RemoveCountDTO inner) / `LineAndDefectDTO` |
| 查询 | `SearchDefectRecordDTO` |
| 登录 | `LoginResultDTO` |

### 2.3 event/ (1)
| 事件 | 触发方 | 监听方 |
|---|---|---|
| `PushAlarmEvent` | `AlarmRecordServiceImpl.sendAlarmMessage` | `YKServiceImpl.pushAlarm2YK` (@Async) |

### 2.4 service/ (1) + service/impl/ (1)
| 接口 | 实现 | 责任 |
|---|---|---|
| `IYKService` | `YKServiceImpl` (198 行) | **🟡 P1** 全部鹰科业务 |

### 2.5 web/ (1)
| Controller | 端点 |
|---|---|
| `YKController` | `/client/yk/line-defect` (GET) + `/client/yk/defect-record` (POST) |

---

## 3. 核心流程

### 3.1 Ticket 自动续期流程（启动时 + 定时）

```
Spring Boot 启动
  │
  └─→ YKServiceImpl.@PostConstruct init()
        │
        └─→ updateTicket()
              │
              ├─→ if yk.enable == false → 跳过
              │
              ├─→ POST AuthenticationController.Login
              │     parameters = [username, password]
              │     → HikWebClient 同步阻塞（blockPost）
              │
              ├─→ if resp.success → ticket = resp.context.ticket
              │                   log "success to get ticket"
              │                 else
              │                   log "get ticket failed"
              │
              └─→ 调度下次续期：
                    threadPoolTaskScheduler.schedule(updateTicket, now + loginInterval 分钟)
                    (单线程 ThreadPoolTaskScheduler, name: "Update-Ticket-Thread-")
```

**关键点**：
- `ThreadPoolTaskScheduler` 在构造函数中创建并初始化（poolSize=1）
- 每次续期成功后调度下一次（**链式调度**，无固定 cron）
- 如果 `yk.enable=false`，**不会初始化 ticket**，pushAlarm2YK 会一直报错

### 3.2 数据查询流程

**A. 产线/缺陷字典查询（GET /client/yk/line-defect）**
```
YKController.searchLineAndDefect()
  │
  └─→ YKServiceImpl.handleLineAndDefectSearch()
        │
        ├─→ lineService.list() → 收集 lineNo 列表 + faceNo 列表
        └─→ defectTypeService.list() → 收集 defectName 列表
              │
              └─→ LineAndDefectDTO { lineGroup: [L1, L2, ...], faceGroup: [F1, F2, ...], defectGroup: [...] }
```

**B. 缺陷记录分时查询（POST /client/yk/defect-record）**
```
YKController.searchDefectRecord(@RequestBody SearchDefectRecordDTO)
  │
  └─→ YKServiceImpl.searchDefectRecord(form)
        │
        ├─→ defectRecordService.searchDefectRecord(form)
        │     └─→ 复用 defect 模块的查询（按时间分组 + lineNo+faceNo+defectType 过滤）
        │           → List<DefectDayRecordPO>
        │           → 转换为 List<DetectDataDTO.DefectDataDTO>
        │
        └─→ if ykConfig.searchRemove == true:
              └─→ lineDayRecordService.searchLineDayRecord(form)
                    └─→ List<LineDayRecordPO>
                          → 转换为 List<DetectDataDTO.RemoveCountDTO>
                    │
                    └─→ DetectDataDTO { defects: [...], removeCounts: [...] }
```

### 3.3 报警推送流程（事件驱动）

```
AlarmRecordServiceImpl.sendAlarmMessage() 检测到 sendYkEnable=YES
  │
  └─→ publish(PushAlarmEvent{source=this, alarmRecord})
        │
        └─→ YKServiceImpl.pushAlarm2YK(@Async @EventListener)
              │
              ├─→ if ticket == null OR yk.enable == false → log error "ticket is null"
              │
              ├─→ AlarmDTO = AlarmDTO.convertFromPO(alarmRecord)
              ├─→ alarm.setWorkshop(ykConfig.workshop)
              │
              ├─→ if alarmRecord.defectName != null:
              │     ├─→ SELECT COUNT(*) FROM alarm_record
              │     │     WHERE defect_name=? AND line_no=? AND face_no=? AND type=? AND solve=UNSOLVED
              │     ├─→ alarm.alarmCount = count
              │     └─→ alarm.alarmDetails = "{oldDetails}({count})"  // 拼接括号
              │
              ├─→ POST VisualInspectionController.HandleVisualInspectionAlarm
              │     parameters = [AlarmDTO]
              │     context = { ticket, lang=1 }
              │     → HikWebClient.blockPost
              │
              └─→ if resp.success=false OR resp.result==null OR parseCode(resp.result) != 200:
                    log error "push alarm info to yk failed"
```

**核心方法签名**:
```java
@Async @EventListener(PushAlarmEvent.class)
public void pushAlarm2YK(PushAlarmEvent event)
    // ticket 空检查 → 转 DTO → 拼 count → 推 POST → 验返回码

public Integer parseCode(Object data)  // BaseResult.toBean(result) → result.getCode()
```

---

## 4. 关键类逐个解析

### 4.1 🟡 P1: `YKServiceImpl` (198 行)

**核心字段**:
```java
private volatile String ticket;  // 鹰科登录票据（volatile 保证多线程可见）
private final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
// 单线程调度器，专门用于 ticket 续期
```

**构造函数**:
```java
public YKServiceImpl() {
    this.threadPoolTaskScheduler.setPoolSize(1);
    this.threadPoolTaskScheduler.setThreadNamePrefix("Update-Ticket-Thread-");
    this.threadPoolTaskScheduler.initialize();
}
```
⚠️ **注意**：构造函数中直接调用 `initialize()`，但 `@Autowired` 还未注入（即 `ykConfig` 还没值）。所以构造函数本身不依赖 ykConfig，但 `@PostConstruct init()` 会调用 `updateTicket()`，此时 ykConfig 已注入。

**关键方法**:
```java
@PostConstruct private void init()                      // 启动时调用 updateTicket
private void updateTicket()                              // 登录 + 调度下次续期
@Override handleLineAndDefectSearch()                    // 字典查询
@Override searchDefectRecord(SearchDefectRecordDTO)      // 缺陷记录查询
@Async @EventListener pushAlarm2YK(PushAlarmEvent)       // 报警推送
public Integer parseCode(Object data)                    // 响应码解析（BaseResult.toBean）
```

**注意**：构造函数中的 `threadPoolTaskScheduler.initialize()` 会在 `@PostConstruct` 前执行，所以调度器已就绪。

### 4.2 🟡 P1: `YKConfig`

```java
@ConfigurationProperties("yk")
public class YKConfig {
    private boolean enable;
    private String url;
    private String username;
    private String password;
    private Integer loginInterval;
    private String workshop;
    private boolean searchRemove = true;
    
    public String getBaseUrl() {
        return this.url.replace(this.getUri(), "");  // 去掉 path 部分
    }
    public String getUri() {
        return URLUtil.toURI(this.url).getPath();     // 提取 path
    }
}
```

**application.yml** 预期格式:
```yaml
yk:
  enable: true
  url: https://yk.example.com/api/path
  username: psm_user
  password: xxxxx
  loginInterval: 30  # 分钟
  workshop: 车间A
  searchRemove: true
```

---

## 5. HTTP 协议

### 5.1 请求结构

```json
POST {yk.baseUrl}{yk.uri}  // 例如 https://yk.example.com/api/path

{
    "apiType": "AuthenticationController",
    "method": "Login",
    "parameters": [
        "psm_user",
        "xxxxx"
    ],
    "context": null  // 登录时为 null，调用时为 {"ticket": "...", "lang": 1}
}
```

### 5.2 响应结构

```json
{
    "success": true,
    "context": {
        "ticket": "eyJhbGc..."
    },
    "result": null  // 或 BaseResult { code: 200, message: "ok", data: ... }
}
```

### 5.3 报警推送请求

```json
POST https://yk.example.com/api/path
{
    "apiType": "VisualInspectionController",
    "method": "HandleVisualInspectionAlarm",
    "parameters": [
        {
            "value": [
                {
                    "alarmCount": 3,
                    "alarmDetails": "缺陷详情(3)",
                    "workshop": "车间A",
                    ...
                }
            ]
        }
    ],
    "context": { "ticket": "eyJhbGc...", "lang": 1 }
}
```

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| 无对应模块 | ❌ 没做 | - |

yingke 模块是**外部系统集成层**，EdgeHost 部署在产线侧不需要直接调用鹰科（一般是上层调度系统调用）。

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| YKServiceImpl（鹰科 HTTP 集成）| ❌ 不需要移植 | ⚪ N/A |
| Ticket 自动续期 | ❌ | ⚪ N/A |
| 报警推送（PushAlarmEvent → YK）| ❌ | ⚪ N/A |

### 6.3 移植建议

**yingke 模块不需要移植到 EdgeHost**：
1. EdgeHost 是产线侧网关，负责本地数据汇聚
2. yingke 是上层 PSM 与鹰科（管理层视觉平台）的集成
3. EdgeHost 只需要：
   - 本地 retention（detect.DetectDataTaskManager）
   - 报警本地落库 + WebSocket 推送（alarm.AlarmRecordServiceImpl）
   - **不需要**直接 HTTP 调用鹰科

**如果未来需要 EdgeHost 直连鹰科**：
- 抄 `YKConfig` + `YKServiceImpl.pushAlarm2YK`
- Ticket 续期改 `IHostedService` + `PeriodicTimer`
- `HikWebClient` 改 `HttpClient` + `IHttpClientFactory`

---

## 7. 风险 / 注意点

### 7.1 ⚠️ Ticket 续期失败不重试

```java
this.threadPoolTaskScheduler.schedule(this::updateTicket, overTimeInstant);
```

只在 `success == true` 时调度下次。如果某次失败，**没有 schedule 下次调用**，导致 ticket 一直为空。需要外层重试或 `try-catch` 后无论如何 schedule。

### 7.2 ⚠️ ThreadPoolTaskScheduler 关闭

`threadPoolTaskScheduler` 没有显式 `@PreDestroy destroy()`，应用关闭时线程池可能被强制中断，调度任务丢失。建议增加：

```java
@PreDestroy
public void shutdown() {
    if (threadPoolTaskScheduler != null) threadPoolTaskScheduler.shutdown();
}
```

### 7.3 ⚠️ HikWebClient 同步阻塞

`client.uri(...).body(request).blockPost(YKResponseDTO.class, YKResponseDTO.DEFAULT)` 是**同步阻塞**，在 `@Async` 线程中执行。如果鹰科响应慢，会占用 async 线程池线程。

### 7.4 ⚠️ 报警推送错误处理弱

```java
if (!resp.getSuccess() || resp.result == null || parseCode(resp.result) != 200) {
    log.error("push alarm info to yk failed.[resp={}]", resp);
}
```

只 log.error，**没有重试、没有本地持久化**。如果鹰科宕机，报警会丢失。建议：
- 重试 3 次（指数退避）
- 失败时写入 `yk_push_failed` 表，定时任务补推

### 7.5 ticket 线程可见性

`private volatile String ticket;` 用了 volatile，保证多线程可见。但 `updateTicket` 中**先 set ticket 再 schedule**，有可能 schedule 还没执行就被新线程读到新 ticket（这是好事）。但 pushAlarm2YK 在另一个线程读 ticket，`@Async` 默认用 `SimpleAsyncTaskExecutor`（无池），可能存在 `ticket = null` 的瞬间窗口。

### 7.6 AlarmDTO.convertFromPO 字段映射

需要查看 `AlarmDTO.convertFromPO` 方法实现。从推断看：
- alarmCount / alarmDetails 字段由 PSM 端构造（带括号拼接）
- alarmCount 来自 `SELECT COUNT(*)` 当前 UNSOLVED 同类报警

### 7.7 enable=false 模式下行为

`yk.enable=false` 时：
- `@PostConstruct init()` 跳过 `updateTicket()` —— ✅
- `pushAlarm2YK` 检测 `enable==false` 直接 log error（**没有跳过**）—— ⚠️ 应该 early return

### 7.8 searchRemove 默认值

`searchRemove = true` 默认开启，意味着 `/client/yk/defect-record` 默认会查 `removeTotal`。如果鹰科侧不需要这个数据，可以关闭减少 SQL。

### 7.9 DetectDataDTO 内嵌结构

`DetectDataDTO` 包含两个 inner 类 `DefectDataDTO` 和 `RemoveCountDTO`，分别转换 `DefectDayRecordPO` 和 `LineDayRecordPO`。鹰科需要这种结构化数据。

---

## 8. 总结

yingke 模块是 PSM 与鹰科的集成层，P1 关注点：
1. **`YKServiceImpl.pushAlarm2YK`**：异步事件推送报警
2. **`updateTicket`**：自动续期 ticket（链式调度）
3. **`YKConfig`**：7 个配置项

关键风险：
- Ticket 续期失败不重试
- ThreadPoolTaskScheduler 未关闭
- HikWebClient 同步阻塞
- 推送失败无重试/持久化
- enable=false 模式下 pushAlarm2YK 仍然执行（只是 log error）

**EdgeHost 移植优先级：⚪ N/A**（yingke 不需要移植到产线侧）
