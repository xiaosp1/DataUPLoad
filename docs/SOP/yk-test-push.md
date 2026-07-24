# YK 测试推送 SOP（老板 2026-07-22 18:35 新规）

## 铁则
1. PM 任何 yk 推送测试，**必须先在群里说**
2. 等老板说"同意推"
3. 推之前必须跑 `scripts/test-yk-push.ps1`
4. 脚本自动 30 秒后回滚 yk.enable = false
5. 即使老板说"推 1 条"，也只能 1 条，30 秒后自动熔断

## 推送步骤

```powershell
# 步骤 1：在群里说"PM 请求推测试报警，XXX 条，YYY 秒"
# 步骤 2：等老板 OK
# 步骤 3：跑脚本
.\scripts\test-yk-push.ps1 -Reason "PM验证W-F03聚合字段"
```

## 禁止
- 禁止用 curl/Invoke-WebRequest 直接 POST 报警绕过 SOP
- 禁止 yk.enable 改为 true 后不熔断
- 禁止推超过 10 条

## 失败回滚

如果脚本崩了、PM 跑了脚本忘了熔断、或者老板要立刻停推，**手工回滚**：

```powershell
# 把 yk.enable 改回 false
(Get-Content E:\DEMO\数据采集\DataupLoad\config\application-prod.yml) `
    -replace 'enable: true', 'enable: false' | Set-Content E:\DEMO\数据采集\DataupLoad\config\application-prod.yml
```

## 关联文件
- 脚本：`scripts/test-yk-push.ps1`
- Git hook：`.git/hooks/pre-commit-yk-check.sh`
- 配置文件：`DataupLoad/config/application-prod.yml`
