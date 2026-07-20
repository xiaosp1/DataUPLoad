# IntcoEdge WebUI

英科边缘数据中控大屏前端 —— Vue 3 + Element Plus + ECharts。

> Task B1 交付：搭建工程骨架 + 主大屏 / 产线详情 / 缺陷查询三个核心页面 + mock 数据。
> 数据接口约定见后端 [src/IntcoEdge.EdgeHost](../../IntcoEdge.EdgeHost/)（W-A3 同步建设中）。

---

## 目录结构

```
src/IntcoEdge.WebUI/
├── package.json          # 依赖清单（vue 3.4 / vite 5 / element-plus 2.4 / echarts 5.4 / axios 1.6）
├── vite.config.js        # 端口 5289，代理 /api -> EdgeHost 5288
├── index.html            # 入口 HTML（中英科中控大屏）
├── README.md             # 本文件
└── src/
    ├── main.js           # Vue + Element Plus + ECharts 注册
    ├── App.vue           # 顶部 Header + 左侧菜单 + 主区域
    ├── router/index.js   # vue-router 4
    ├── api/
    │   ├── index.js      # axios 封装，baseURL = '/api'
    │   └── mock.js       # mock 数据（10 产线 / 120 摄像头 / 30 缺陷 / 5 报警）
    ├── stores/index.js   # pinia 全局状态
    ├── components/
    │   ├── LineOverview.vue       # 产线概览卡片
    │   ├── DefectTrendChart.vue   # 缺陷趋势折线图
    │   ├── LineDefectRanking.vue  # 产线缺陷排行柱状图
    │   └── AlarmList.vue          # 报警列表
    ├── views/
    │   ├── Dashboard.vue          # 主大屏
    │   ├── LineDetail.vue         # 产线详情
    │   └── DefectQuery.vue        # 缺陷查询
    └── assets/styles.css          # 深色主题
```

## 开发

```bash
cd src/IntcoEdge.WebUI
npm install      # 装依赖（首次，PM 统一执行）
npm run dev      # 启动 vite dev server，http://localhost:5289
```

打开浏览器访问 **http://localhost:5289/** 即可看到主大屏。
前端通过 vite proxy 把 `/api/*` 转发到 EdgeHost `http://localhost:5288`。

> 备注：PM 验收时统一执行 `npm install`；B1 期间不联网、不 build。

## 部署（生产）

```bash
npm run build    # 输出到 dist/
```

把 `dist/` 目录内容拷贝到 EdgeHost 的静态托管目录：
- 默认配置（`appsettings.json` -> `IntcoEdge:WebUi:Path`）指向 `src/IntcoEdge.WebUI/dist`
- EdgeHost 启动时检测该路径，若存在则 `UseStaticFiles` + `MapFallbackToFile("index.html")`
- 浏览器访问 **http://localhost:5288/** 即可（API 走相对路径 `/api/*` 同源访问，无跨域问题）

## 与 EdgeHost 5288 的对接

| 场景 | 前端 | 后端 |
|------|------|------|
| 开发 | `npm run dev` → http://localhost:5289/ | `dotnet run` → http://localhost:5288/ |
| 开发联调 | 浏览器开 5289，Vite proxy 把 `/api` 转发到 5288 | 5288 需在 Program.cs 注册 CORS（v0.4 加入） |
| 生产集成 | EdgeHost serve `dist/` | 同源，零 CORS |

### 接口约定（v0.3 mock → v0.4 真接口）

```
GET /api/lines            # 产线列表
GET /api/lines/{id}       # 单产线详情
GET /api/cameras          # 摄像头列表（支持 ?lineId=）
GET /api/defects          # 缺陷列表（支持 ?lineId=&from=&to=&type=）
GET /api/defects/trend    # 缺陷趋势（按小时聚合）
GET /api/defects/ranking  # 产线缺陷排行
GET /api/alarms           # 报警列表（支持 ?since=）
GET /health               # 健康检查
```

W-A3 完成 Controller 后，前端把 `api/mock.js` 切回 `api/index.js` 的 axios 调用即可。

## 主题色

- 背景：`#0a1929`
- 卡片：`#0f2942`
- 文字：`#e0e6ed`
- 强调：`#1976d2`
- 告警：`#f56c6c`
- 成功：`#67c23a`

字体：`Microsoft YaHei` + `DIN` / `Roboto` 数字。

## 已知 TODO

- W-A3 真接口未完成前走 mock（`api/mock.js`）
- v0.4 加入 WebSocket 实时推送（当前 Dashboard 用 setInterval 轮询 mock）
- v0.4 加入用户登录 / 权限控制（Element Plus 菜单权限）
- v0.5 加入摄像头 RTSP 实时画面（video.js / flv.js）
