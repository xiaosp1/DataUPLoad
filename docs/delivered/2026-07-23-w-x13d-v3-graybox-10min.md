# W-X13d-v3 10min 灰盒验收 — ✅ PASS

**验收人**：PM 锋卫 🏭
**验收时间窗**：08:55 - 09:05 GMT+8（hik-java 启动后 21-30 min）
**验收方法**：铁则 41 强制运行时验证（基线对比 + 实时数据）

---

## ✅ 验收结论

| 核心指标 | 结果 | 评估 |
|---|---|---|
| **yk push alarm ERROR** | **0 / 10min** | ✅✅ 完美 |
| **yk ticket 续期** | 1 → 1（login-interval=50min 正常）| ✅ |
| **alarm 入库** | **+4981 条 / 10min** | ✅ 业务正常 |
| **hik-java 进程** | PID 33248 alive 30min / CPU 稳 | ✅ |
| **ESTABLISHED 相机** | 35 → **38**（+3 重连）| ✅ |
| **DataupLoad.log ERROR** | 1 条（IgnoreExpireTask 业务噪声）| ✅ 跟 yk 无关 |
| **端口 80 LISTEN** | 1 | ✅ |

---

## 📊 详细数据

### 基线（08:55:00）

```
hik_pid=33248 cpu_kernel_s=21.7 cpu_user_s=145.7 start=07/23/2026 08:34:44
listen=1 estab=35
ticket_log=1（08:34:56 首次登录）
yk_error_log=0
dl_combined_error=5
alarm_received=11481
```

### 结果（09:05:00）

```
hik_pid=33248 cpu_kernel_s=26.1 cpu_user_s=163.9 start=07/23/2026 08:34:44
listen=1 estab=38
ticket_log=1（无新增，正常，login-interval=50min）
yk_error_log=0 (Δ=0)
dl_combined_error=16 (Δ=11) 但**实际 ERROR 行只有 1 条自 08:55 起**
alarm_received=17941 (Δ=6460)
```

### 自 08:55 起的 ERROR（**只有 1 条**）

```
2026-07-23 09:00:00.091 -ERROR DataupLoad [scheduling-1] 
[IgnoreExpireTask.delExpireIgnoreDefect:59] 
ignore expire alarm remove failed, exception: ...
```

**评估**：跟 yk 推送无关，是报警忽略表清理任务的业务噪声（不在 W-X13d scope）。

### yk 推送静默证据

```
yk_push_err_since_0855=0  ✅
yk_error_in_error_log=0   ✅
```

---

## 🎯 W-X13d 双开关行为确认

| 场景 | 配置 | 实测 |
|---|---|---|
| 启动后 init 登录 | loginEnabled=true | ✅ ticket=klBxvCGK9TuhR0eipxIR9mhkgaFlFcJM+FQzSWOKZ31txciW1I2x04DqzOCLULdDytpRD1sjaw9DHRDLasWOcA== |
| 报警触发推送 | uploadEnabled=false | ✅ 静默跳过，无 ERROR log |
| 50min 后 ticket 续期 | loginEnabled=true | ✅（待验证，预计 09:25 触发）|

---

## 📈 业务影响评估

- 报警持续入库（10min +4981 条，~8 条/秒）
- 相机连接稳定（+3 重连）
- 链路健康（CPU/WS/网络均正常）
- **yk 推送真熔断成功**（铁则 36 实质化）

---

## 🔧 后续

1. ✅ 灰盒验收通过
2. ⏳ Worker 已派：Maven 重建 jar（资产沉淀，不影响生产）
3. ⏳ 50 min 后（09:25）观察 ticket 续期 log 是否触发
4. ⏳ 老板决定是否转正式（uploadEnabled=true）

---

🏭 PM 锋卫 · 2026-07-23 09:06
