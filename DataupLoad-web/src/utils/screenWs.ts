// =============================================================================
// W-FRONT-02-E8 大屏 WebSocket 工具
//
// 与 utils/ws.ts (E2 alarm) 同思路，但 type='screen'：
//   - 端点 1:1 对齐 framework-starter WebSocketConfig：/ws?uid=&type=screen
//   - satoken cookie 由浏览器自动带上（同源策略与 axios 一致）
//   - 30 秒心跳：断线自动重连，setInterval 心跳包（ping / {"type":"ping"}）
//
// E8 复用 utils/ws.ts 的 createWs() 即可，类型别名保持窄语义（screen-only）
// =============================================================================

import { createWs, type WsController, type WsMessage, type WsState } from './ws'

/**
 * 大屏 WS 消息（后端 ScreenMessageHandler 推送）
 *
 * 服务端 PSM 设计：服务端对单次推送零节流（ADR-0011），由前端按 type 字段路由
 * 派发：alarm / defect / device / line / heartbeat。
 */
export interface ScreenWsMessage<T = unknown> {
  type: 'alarm' | 'defect' | 'device' | 'line' | 'heartbeat' | string
  payload: T
  [key: string]: any
}

/**
 * 大屏实时连接器
 *
 * @param uid  当前用户唯一标识（PSM 端取 user.id；空字符串 = 不带 uid）
 * @param onMessage  收到消息回调（type=alarm/defect/... 时由调用方处理）
 * @param onState    连接状态变化回调（顶栏指示器用）
 */
export function connectScreenWs(
  uid: string,
  onMessage: (msg: ScreenWsMessage) => void,
  onState?: (state: WsState, info?: { code?: number; reason?: string }) => void
): WsController {
  return createWs({
    uid,
    type: 'screen',
    onMessage(msg: WsMessage) {
      // 防御性判空 + 透传给调用方
      if (!msg || typeof msg !== 'object') return
      onMessage(msg as ScreenWsMessage)
    },
    onState,
    // 30s 心跳（断线自动重连由 createWs 内部处理；heartbeat 仅做 ping 兜底）
    heartbeatInterval: 30_000,
    reconnectDelay: 3_000
  })
}
