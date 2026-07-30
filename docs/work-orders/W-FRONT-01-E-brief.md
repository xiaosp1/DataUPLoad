# W-FRONT-01-E — 端到端验收

- **父工单**: W-FRONT-01（依赖 W-FRONT-01-D 完成）
- **目标**: 老板侧验收清单全过，留可重复执行的验收脚本

## 验收清单（**全部必须 PASS**）

| # | 项 | 命令 / 操作 | 期望 |
|---|----|-----------|------|
| 1 | GET `/` 主页 | `curl -i http://localhost/` | 200 + `Content-Type: text/html` + body 含 `<div id="app"></div>` |
| 2 | 静态资源 CSS | `curl -i http://localhost/assets/index-xxx.css` | 200 + CSS body |
| 3 | 静态资源 JS | `curl -i http://localhost/js/index-xxx.js` | 200 + JS body |
| 4 | **浏览器零 Whitelabel** | 浏览器访问 `http://localhost/` | 看到登录页（不是 Whitelabel / 不是 404） |
| 5 | 路由守卫 | 输入 `http://localhost/realTime`（无登录态） | 自动跳 `/Login` |
| 6 | 登录闭环 | 填 `super_admin / Abc12345` + 点登录 | 200 + 跳 `/realTime` 占位页 |
| 7 | satoken cookie | DevTools → Application → Cookies | 看到 `satoken` 值 |
| 8 | 刷新不掉登录 | 在 `/realTime` 按 F5 | 仍显示用户名，不跳走 |
| 9 | 退出登录 | 点退出按钮 | 清 sessionStorage + 跳 `/Login` |
| 10 | i18n 切换 | 切 zh-CN → en-US → id-ID | 文案实时变（标题/按钮/占位符） |
| 11 | **GET `/web/auth/login` 浏览器** | 手动访问 | **405**（正确行为，**前端从不直接 GET**，对齐 PSM） |
| 12 | POST `/web/auth/login` 错密码 | curl POST 错密码 | 当前会 500（W-FRONT-01-B fix 范围内，但**不阻塞验收**，标注 known issue） |

## PSM 对齐校验（架构级）

| # | 项 | 现状 |
|---|----|------|
| A1 | 入口是单一 `index.html` | ✅ |
| A2 | 路由在客户端跳转 | ✅ |
| A3 | 后端 `/web/auth/login` POST-only 不动 | ✅（后端零改动） |
| A4 | satoken cookie 自动带 | ✅ |
| A5 | 三语言包 | ✅ zh-CN / en-US / id-ID |
| A6 | Element Plus 替代 Element UI | ✅（新工程） |
| A7 | 单 jar 部署（static/ 内嵌） | ✅ |

## 验收脚本

写一个 `scripts/verify-W-FRONT-01.ps1`，**老板可以一键复跑**：

```powershell
# verify-W-FRONT-01.ps1
$base = "http://localhost"
$pass = 0; $fail = 0

function Check([string]$name, [string]$url, [string]$expect, [string]$method = "GET", $body = $null) {
    try {
        $h = @{}
        if ($method -eq "POST") {
            $r = Invoke-WebRequest -Uri $url -Method POST -ContentType "application/json" -Body $body -Headers @{ Cookie = "satoken=test" } -UseBasicParsing -ErrorAction Stop
            $h = $r.Headers
        } else {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -ErrorAction Stop
        }
        if ($r.StatusCode -eq $expect) {
            Write-Host "[PASS] $name : $($r.StatusCode)" -ForegroundColor Green
            $script:pass++
        } else {
            Write-Host "[FAIL] $name : got $($r.StatusCode), want $expect" -ForegroundColor Red
            $script:fail++
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ("$code" -eq $expect) {
            Write-Host "[PASS] $name : $code (caught)" -ForegroundColor Green
            $script:pass++
        } else {
            Write-Host "[FAIL] $name : error $code, want $expect" -ForegroundColor Red
            $script:fail++
        }
    }
}

Check "GET /"                 "$base/"          "200"
Check "GET /web/auth/login"   "$base/web/auth/login" "405"
$loginBody = '{"username":"super_admin","password":"e10adc3949ba59abbe56e057f20f883e"}'  # sha256("123456") as wrong test
Check "POST /web/auth/login wrong" "$base/web/auth/login" "401" "POST" $loginBody

Write-Host "`n===== RESULT: pass=$pass fail=$fail =====" -ForegroundColor Cyan
```

> 注意：脚本里 password 字段应该是 sha256，不是明文。验收 wrong-pwd 用 sha256("123456") = `e10adc3949ba59abbe56e057f20f883e`

## 报告

`docs/work-orders/W-FRONT-01-report.md`：
- 全部 12 项验收结果表
- PSM 对齐 A1-A7 校验
- `verify-W-FRONT-01.ps1` 跑通输出
- 浏览器截图（登录页 zh-CN + en-US + id-ID 各一张，登录后的 realTime 占位页一张，DevTools cookie 一张）
- 已知问题清单（如果错密码 500 那条没修）

## 不交付

- ❌ 真实业务页（realTime 图表、alarm 列表等）— W-FRONT-02+
- ❌ 性能 / 兼容性测试 — W-FRONT-09

## 耗时上限

30 分钟（脚本 + 截图）
