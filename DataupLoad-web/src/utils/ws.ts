// =============================================================================
// W-FRONT-02-E2 共享 WebSocket 工具
//
// 设计要点：
//   1) 端点 1:1 对齐 framework-starter 的 WebSocketConfig：
//        - 路径：/ws（framework-starter 单端点 + ?type= 过滤）
//        - query：?uid=<用户标识>&type=<alarm|screen|sound>
//        - 鉴权：satoken cookie 由浏览器自动带上（与 axios 同源策略一致）
//   2) 通用连接器：createWs({ uid, type, onMessage }) 返回带 reconnect/heartbeat
//      的 controller（close / open / state），调用方决定何时 subscribe/unsubscribe。
//   3) PSM 设计：服务端零节流（ADR-0011），前端不做去重/合并；新消息到达后
//      立刻把 message 推给 onMessage，由调用方决定插入位置 + 触发声音。
//   4) 不引入 Socket.IO；纯原生 WebSocket，符合 brief 禁止条款。
//
// E2 用法（alarm 页）：
//     const ctrl = createWs({ uid: String(user.id), type: 'alarm', onMessage: push })
//     ctrl.open()
//     onUnmounted(() => ctrl.close())
//
// E8 会复用本文件的 createWs 思路，但 type='screen'（按 brief 表格所列）。
// =============================================================================

export type WsClientType = 'alarm' | 'screen' | 'sound' | 'data'

/**
 * 后端 WebSocketHandler 推送的统一消息格式（framework-starter WsMessage）。
 * payload 形态由各 type 的后端处理器自由定义；前端做防御性判空即可。
 */
export interface WsMessage<T = unknown> {
  type: WsClientType | string
  payload: T
  [key: string]: any
}

export type WsState = 'idle' | 'connecting' | 'open' | 'closing' | 'closed'

export interface CreateWsOptions {
  /** 当前用户唯一标识（PSM 端用 user.id）；空字符串 = 不带 uid（仍允许连接，由后端拦截） */
  uid: string
  /** PSM ?type= 过滤值 */
  type: WsClientType
  /** 收到消息回调（已 try/catch + JSON.parse） */
  onMessage: (msg: WsMessage) => void
  /** 连接状态变化回调（指示器用） */
  onState?: (state: WsState, info?: { code?: number; reason?: string }) => void
  /** 自定义端点路径（默认 /ws）；framework-starter 默认注册路径 */
  path?: string
  /** 自定义 host（默认 location.host，dev 环境跨代理时用） */
  host?: string
  /** 断线重连延迟（ms），默认 3000；传 0 = 不重连 */
  reconnectDelay?: number
  /** 心跳间隔（ms），默认 25000；framework-starter 自身有 WsKeepAliveTask，前端 ping 仅做兜底 */
  heartbeatInterval?: number
}

export interface WsController {
  open: () => void
  close: () => void
  send: (data: string | object) => boolean
  readonly state: WsState
}

/**
 * 创建一个自带重连 + 状态机 + 心跳的 WebSocket controller。
 *
 * 注意：本函数只创建并返回 controller，不会自动 open。
 *      调用方需要在登录态确认后手动 ctrl.open()，登出/卸载时 ctrl.close()。
 */
export function createWs(opts: CreateWsOptions): WsController {
  const {
    uid,
    type,
    onMessage,
    onState,
    path = '/ws',
    host,
    reconnectDelay = 3000,
    heartbeatInterval = 25000
  } = opts

  let ws: WebSocket | null = null
  let state: WsState = 'idle'
  let reconnectTimer: number | null = null
  let heartbeatTimer: number | null = null
  let manuallyClosed = false

  function setState(next: WsState, info?: { code?: number; reason?: string }) {
    state = next
    if (onState) {
      try {
        onState(next, info)
      } catch {
        /* swallow */
      }
    }
  }

  function buildUrl(): string {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    // 默认走 location.host（dev = vite:5173/5175/prod = 同一域名）
    // 但当前 vite.config.js 只代理了 /web，没代理 /ws，
    // 所以前端代码如果直连当前 host 会拿到 vite HMR 的 WS upgrade 失败。
    // 解决办法：当 location.port >= 5173（vite dev），改连后端 8080 直连。
    let h = host
    if (!h) {
      const isViteDev = /^5\d{3}$/.test(location.port || '')
      h = isViteDev ? 'localhost:8080' : location.host
    }
    const qs = new URLSearchParams()
    if (uid) qs.set('uid', uid)
    qs.set('type', type)
    return `${proto}://${h}${path}?${qs.toString()}`
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = window.setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        try {
          ws.send(JSON.stringify({ type: 'ping', payload: null, ts: Date.now() }))
        } catch {
          /* ignore */
        }
      }
    }, heartbeatInterval)
  }

  function stopHeartbeat() {
    if (heartbeatTimer != null) {
      window.clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function scheduleReconnect() {
    if (manuallyClosed || reconnectDelay <= 0) return
    if (reconnectTimer != null) return
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      // 只在 closed 状态下尝试重连
      if (!manuallyClosed && (!ws || ws.readyState === WebSocket.CLOSED)) {
        try {
          doConnect()
        } catch {
          scheduleReconnect()
        }
      }
    }, reconnectDelay)
  }

  function doConnect() {
    manuallyClosed = false
    setState('connecting')
    const url = buildUrl()
    try {
      ws = new WebSocket(url)
    } catch (err) {
      setState('closed', { reason: (err as Error)?.message || 'create-failed' })
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      setState('open')
      startHeartbeat()
    }

    ws.onmessage = (ev: MessageEvent) => {
      let msg: WsMessage
      try {
        msg = typeof ev.data === 'string' ? (JSON.parse(ev.data) as WsMessage) : { type: 'data', payload: ev.data }
      } catch {
        // 非 JSON 帧，包装成 raw data 消息
        msg = { type: 'data', payload: ev.data }
      }
      try {
        onMessage(msg)
      } catch {
        /* swallow handler error */
      }
    }

    ws.onerror = () => {
      // onerror 之后通常会触发 onclose，不要在这里直接 setState(closed)
    }

    ws.onclose = (ev: CloseEvent) => {
      stopHeartbeat()
      ws = null
      setState('closed', { code: ev.code, reason: ev.reason })
      if (!manuallyClosed) scheduleReconnect()
    }
  }

  return {
    open() {
      manuallyClosed = false
      if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        return
      }
      doConnect()
    },
    close() {
      manuallyClosed = true
      if (reconnectTimer != null) {
        window.clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      stopHeartbeat()
      if (ws) {
        try {
          setState('closing')
          ws.close(1000, 'client-close')
        } catch {
          /* ignore */
        }
      } else {
        setState('closed')
      }
    },
    send(data: string | object) {
      if (!ws || ws.readyState !== WebSocket.OPEN) return false
      try {
        ws.send(typeof data === 'string' ? data : JSON.stringify(data))
        return true
      } catch {
        return false
      }
    },
    get state() {
      return state
    }
  }
}

// ---------------------------------------------------------------------------
// 便捷封装：E2 alarm 页专用（保留给 E8 复用同类思路）
// ---------------------------------------------------------------------------

/**
 * 连接 alarm 实时通道（/ws?uid=&type=alarm）。
 *
 * 语义等价于 createWs({ uid, type: 'alarm', ... })，但参数语义更聚焦。
 */
export function connectAlarmWs(
  uid: string,
  onMessage: (msg: WsMessage) => void,
  onState?: (state: WsState, info?: { code?: number; reason?: string }) => void
): WsController {
  return createWs({ uid, type: 'alarm', onMessage, onState })
}
