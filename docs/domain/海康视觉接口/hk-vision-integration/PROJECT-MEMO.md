# PROJECT-MEMO — 海康视觉 ↔ 英科对接

> 项目根目录：`海康视觉接口/hk-vision-integration/`
> 最近更新：2026-07-06

## 项目结构
- 中间服务：FastAPI（海康调本服务登录+报警；本服务主动调海康/英科）
- 源文档：父目录 `01-接口需求整理.md`、`02-对接Checklist.md`、`03-openapi-draft.yaml`

## 技术栈
- Python 3.11+ / FastAPI / httpx / APScheduler / Pydantic v2 / loguru / uvicorn
- 配置：pydantic-settings + `.env`
- 测试：pytest + fastapi TestClient
- 依赖管理：poetry（pyproject.toml 已写，待 `poetry install`）

## 公共约定
- 字段命名：内部统一 snake_case，对外（英科 dataportal PascalCase、海康）通过 Pydantic alias 适配
- 时间格式：内部统一 ISO 8601（`2024-08-30T14:30:00`），adapter 层处理海康空格分隔格式
- 响应封装：对外响应保持英科风格 `{Success, Message, Result, Context}`，HTTP 状态恒 200，业务结果看 `Result.code`（200 成功/4xx 失败）
- 幂等键：`WorkShop + Line + Face + AlarmTime + AlarmType` SHA1
- 日志：loguru，控制台+文件（logs/app.log，10MB 轮转，保留 14 天）
- Ticket：进程内 TTL 缓存（默认 1800s），M2 加自动续期
- 车间代码枚举：WorkShopEnum（淮北 HBN1-6/HBP1-6、江西 JXN1-4、青州 QZM1/QZN1-3/QZP1-3）

## 接口清单（4+1）
1. 海康→本服务（代英科入口）：POST /api/dataportal/invoke — AuthenticationController.Login
2. 海康→本服务：POST /api/dataportal/invoke — VisualInspectionController.HandleVisualInspectionAlarm
3. 本服务→海康：GET 配置查询（URL 待海康）
4. 本服务→海康：POST 检测数据查询（URL 待海康，按整点）
5. 本服务→英科：POST /api/dataportal/invoke（M2 实现真实转发）
- 内部：GET /healthz

## 历史决策
- 2026-07-06 立项：完成 M1 骨架、STATUS.md、PROJECT-PLAN.md、README.md
- 需求文档第 13 项待确认问题已全部登记在 STATUS.md "阻塞项"
- 2026-07-06 PM 模式切换：初始化 STATUS.md/PROJECT-MEMO.md/TODO.md，上下文压缩归档

## 已交付文件（M1 骨架，共 28 个源码/文档文件）
- 根目录：README.md / PROJECT-PLAN.md / pyproject.toml / .env.example / .gitignore
- src/hk_integration：main.py / config.py / logging.py / scheduler.py / __init__.py
- api/routes：health.py（/healthz 可跑）/ alarm.py（dataportal 分发+Ticket 校验+幂等）
- api/schemas：dataportal.py（PascalCase 信封模型）/ alarm.py（AlarmPayload）
- clients：hk_client.py（M3 待实现）/ inkey_client.py（M2 待实现）
- services：auth.py（TicketCache TTL）/ alarm_service.py（幂等键+落日志）/ sync_service.py（M3 骨架）
- models：enums.py（WorkShopEnum 22 个车间）/ domain.py（ConfigSnapshot/DefectRecord 等）
- tests：test_health.py / test_alarm_api.py（无 Ticket→401，合法 Ticket→200）
- docs/openapi-draft.yaml（复制自父目录）

## 本会话归档（2026-07-06 上午至下午）
- 启动动作：根据 `01-接口需求整理.md` 做项目规划+建立目录
- 执行方式：首次直接派 Codex，遭遇 PowerShell here-string 转义 bug（2 次 `字符串缺少终止符: '@` 报错），改用 write 工具手动补齐所有空文件，稳定可控
- 代码质量门：`python -m compileall src tests` 全量 SYNTAX OK；依赖未安装（fastapi 等不在系统 Python），待 `poetry install` 后跑 pytest
- M1 验收状态：骨架代码与文档全部就位，可启动性在依赖安装后验证（预计一次通过）
