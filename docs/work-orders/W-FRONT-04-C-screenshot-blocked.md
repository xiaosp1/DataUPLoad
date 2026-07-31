# W-FRONT-04-C 截图受阻 — login 10500

> **日期**: 2026-07-31 13:30 GMT+8
> **状态**: 老板让 PM 操作截图，PM 发现 login 自 7-31 09:26 起就一直 10500，新老服务都坏
> **结论**: 截图只能截到登录页，无法验证 reload 修复

## 真因

`POST /web/auth/login` 返回 `{"success":false,"code":10500,"message":"操作异常"}`

error.log 多次出现 `Invalid bound statement (not found): com.hikrobotics.solution.framework.component.account.mapper.AccountDAO.get`

**关键发现**：
1. 7-31 09:26 老服务 PID 6000 (7-23 编译) 就报这个错（不是新编译导致）
2. 新服务 PID 18092 (7-31 编译) 同样报
3. framework-starter-2.2.3-SNAPSHOT.jar 里 `AccountMapper.xml` 路径是 `framework/mapper/`，interface 是 `framework/component/account/mapper/AccountDAO`
4. namespace 正确匹配，但 MyBatis 启动时没加载这个 XML

**怀疑**：7-25 W-AUTH-01 P1 修复后某次 application-prod.yml 改动丢了 mybatis-plus.mapper-locations 配置，导致 XML 全部不扫。

## 业务影响

- ✅ `GET /` (登录页 HTML) 200
- ✅ `GET /assets/index-*.js` (Vue bundle) 200
- ✅ `GET /web/account/list` 200（无需登录）
- ✅ WS screen/alarm 实时数据正常推送（info.log 持续打）
- ✅ 报警处理 / 细粒度推送 全部正常
- ❌ `POST /web/auth/login` 10500（账号体系坏）
- ❌ `/web/account/current` 等需 satoken 接口都坏

## 截图限制

老板让 PM 截图，但 login 自身坏了，**无法截到 reload 路由保留的修复页面**。

能截的：
- 登录页加载（Vue SPA 200 + 玻璃风）
- 登录失败的报错弹窗（10500 操作异常）

不能截的：
- 登录后的 /#/realtime / /#/alarm
- F5 reload 路由保留场景

## 下一步

老板拍板：
1. **优先修 10500**：调查 mybatis-plus mapper 加载（PM 推测 application-prod.yml 缺 mapper-locations 或 framework-starter jar 嵌套 XML 加载问题）
2. **回滚 services 到老 jar**（没有老 jar，只有老进程但已死，无 7-23 编译产物备份）
3. **PM 直接 curl 关键接口 + 贴日志**：放弃截图，文字报告 + 日志给老板看

## 不许做的事

- ❌ 不许用 placeholder / mock 数据假装截图
- ❌ 不许绕过 10500 强截图（loader 不靠谱）
- ❌ 不许改 framework-starter jar
