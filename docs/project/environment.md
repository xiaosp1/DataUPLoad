# 现场环境（★ 老板 16:00 提供）

> 老板 16:00 提供的现场网络信息 + PM 16:00-16:05 现场验证结果。

---

## 🖥️ 本机

| 项 | 值 | 备注 |
|---|---|---|
| **本机外部网口 IP** | **`192.168.135.150`** | 老板确认 |
| **主机名** | QZZBZFJIAHONGH | OpenClaw 报 |
| **操作系统** | Windows 10 (x64) | OpenClaw 报 |
| **角色** | **新 EdgeHost 开发机 + 测试机 + 部署机（最终）** | 老板拍 |

## 🌐 现场网络

| 网段 | 用途 |
|---|---|
| `192.168.135.0/24` | **车间网络**（视觉软件 + 老 PSM + 本机都在这网段） |
| `192.168.80.0/24` | **英科/MES 网络**（`192.168.80.33` 是英科统一网关） |

## 🎯 关键机器

| IP | 角色 | 状态 | 备注 |
|---|---|---|---|
| **`192.168.135.150`** | **本机（新 EdgeHost 部署目标）** | ✅ 在线 | ping < 1ms |
| **`192.168.135.15`** | **现场老 PSM** | ✅ 在线 | 443 端口开（HTTPS） |
| **`192.168.80.33`** | **英科系统统一网关** | ✅ 在线 | 10031 端口，ping 25ms |
| `192.168.135.1` | 车间网关 | ✅ 在线 | ARP 表里 |
| `192.168.135.70-89` | 车间工控机群（24+ 台）| ✅ 在线 | 视觉软件/相机控制盒大概率在这里 |

## ✅ 老板 16:00 确认的连通性

| 测试 | 结果 |
|---|---|
| 车间视觉软件 → 本机 | ✅ **能拼通**（老板说） |
| 本机 → MES | ✅ **能拼通**（老板说） |
| 本机 → 英科网关 `192.168.80.33:10031` | ✅ **能拼通**（PM 验证 25ms） |
| 本机 → 现场老 PSM `https://192.168.135.15:443` | ✅ **能拼通**（PM 验证端口 443 OPEN） |
| 老 PSM 上 `/client/yk/line-defect` | ✅ 返回 404（路径需 HTTPS + PSM 没启动？）或 PSM 端有重定向 |

## 🔧 端口分配预案（避免冲突）

| 端口 | 服务 | 状态 |
|---|---|---|
| `5188` | **老 EdgeHost（已掉线）** | 释放 |
| **`5288`** | **新 EdgeHost REST API（PM 提议）** | 待派 |
| **`80/443`** | 老 PSM HTTPS（不动）| 已占用 |
| **`10031`** | 英科统一网关（不动）| 已占用 |

## 🧪 测试链路预案（★ 给 Worker 的"测通不通"清单）

新 EdgeHost 起来后，按以下顺序验证：

### 链路 1：视觉软件 → 新 EdgeHost

```bash
# 模拟视觉软件推数据到新 EdgeHost
curl -X POST http://192.168.135.150:5288/client/data/detect \
  -H "Content-Type: application/json" \
  -d '{"faceNo":"A1","lineNo":"L01","todayData":{},"realTimeData":{}}'

# 期望：200 OK + defect_record 入库
```

### 链路 2：MES → 新 EdgeHost → 数据返回

```bash
# MES 拉字典
curl -X GET http://192.168.135.150:5288/client/yk/line-defect

# MES 拉缺陷
curl -X POST http://192.168.135.150:5288/client/yk/defect-record \
  -H "Content-Type: application/json" \
  -d '{"startTime":"2026-07-20 00:00:00"}'

# 期望：响应格式严格按 PSM 协议 1+2（见 08-hikvision-yk-protocol.md）
```

### 链路 3：新 EdgeHost → 英科网关（报警推送）

```bash
# EdgeHost 推报警到英科
curl -X POST http://192.168.80.33:10031/api/dataportal/invoke \
  -H "Content-Type: application/json" \
  -d '{
    "ApiType":"VisualInspectionController",
    "Parameters":[{"Value":[{
      "WorkShop":"QZN2",
      "Line":"L01",
      "Face":"A",
      "AlarmTime":"2026-07-20T16:00:00",
      "AlarmType":"Temperature Alert",
      "AlarmLevel":"High",
      "AlarmDetails":"test",
      "AlarmResult":"已处理",
      "AlarmCount":1
    }]}],
    "Method":"HandleVisualInspectionAlarm",
    "Context":{"InvOrgId":1}
  }'

# 期望：英科网关返回 Success=true（前提：Ticket 已登录）
```

### 链路 4：现场老 PSM 还能继续用（并行观察）

```bash
# 老 PSM 状态检查
curl -k -X GET https://192.168.135.15:443/client/yk/line-defect

# 期望：返回产线+缺陷字典
```

---

## ⚠️ PM 验证时的发现

1. **`192.168.135.15:443` 老 PSM 在 HTTPS 上**——确认 PSM 用 jks 自签证书（hikrobot.jks），curl/HttpClient 调用需要 `-k` / `SkipCertificateCheck`
2. **`192.168.135.15:8000` 也 OPEN**——可能是 PSM 的某个备用端口（WebSocket？管理界面？）需要再探
3. **车间工控机群 IP 70-89**——PM 推测是**视觉软件主机**（每台装一个视觉软件）。这意味着我们测试时可以模拟任一工控机推数据
4. **`192.168.80.33:10031` HTTPS 慢响应**（超时 5s）—— 可能需要更长 timeout，或者英科网关有反向代理

## 📂 相关文档

- `docs/project/work-breakdown.md` — 任务分解
- `docs/project/new-edgehost-scope.md` — 项目总纲
- `docs/domain/英科医疗手套车间/02-data-flow.md` — 数据流（待补充 IP 信息）
- `docs/domain/海康大屏逆向/PSM/reverse-engineering/08-hikvision-yk-protocol.md` — 协议（待补车间代码 QZN2）

---

## 🎯 新增 Task

老板环境信息确认后，新增 **Task E1：现场环境验证**：

| Task E1 | 现场环境验证 |
|---|---|
| **依赖** | 无（首批 Worker 启动时必做） |
| **输入** | 本文档 + `192.168.135.15` 老 PSM + `192.168.80.33:10031` 英科网关 |
| **输出** | 验证报告：`能调通老 PSM / 能登录英科 / 能模拟视觉软件推数据` |
| **DoD** | 3 条链路 curl 测试都返回预期 |
| **Worker** | W-E1（PM 自己 + Worker 一起） |
| **估时** | 半天 |
| **优先级** | **🟢 必须 W1 完成**，否则后续 Task 没法测 |

**新 EdgeHost REST API 端口：5288**（不跟老 5188 冲突）。
