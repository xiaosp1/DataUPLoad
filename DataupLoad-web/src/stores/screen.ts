// =============================================================================
// W-PERF-B 全局 WS 单例 - 大屏实时数据
//
// 设计要点（仿 PSM GlobalTaskManager.sendScreen() → /ws?type=screen 推送链路）：
//   1) 全局单例：整个 App 只连一个 /ws?type=screen；切路由/进入退出页面不重复 open。
//      - App.vue onMounted 调用 connectScreenSingleton() 启动；onBeforeUnmount 关闭。
//      - 多个 view 同时订阅同一 WS（subscribeScreen 模式），由本模块把最新快照 fan-out。
//   2) 替代 60s polling：RealTime.vue 不再用 setInterval → fetchAll()，改用
//      subscribeScreen(cb) 拿到 WS 推送；首次进入页面若已有快照，直接渲染（~0ms）。
//   3) 服务端零节流（ADR-0011）：服务端每 5s 推全量 ScreenDataDTO 快照，前端按完整快照覆盖消费。
//   4) 不引入 Socket.IO；纯原生 WebSocket + Pinia reactive state，沿用 utils/screenWs.ts。
//
// 注意：本模块不通过 defineStore 暴露 WS controller（避免 Pinia $reset 时被清空导致连接泄漏），
//      只暴露响应式 snapshot 状态（用 reactive() 实现，组件里 watch 即可）。
// =============================================================================

import { reactive, computed, type ComputedRef } from 'vue'
import { connectScreenWs, type ScreenWsMessage } from '../utils/screenWs'
import type { WsController, WsState } from '../utils/ws'
import { useUserStore } from './user'

// -----------------------------------------------------------------------------
// 数据快照类型（与后端 ScreenDataDTO 1:1 对齐；前端只读，不修改）
// -----------------------------------------------------------------------------

/** 单条线实时采集数据（来自 ScreenDataDTO.DetectDataDTO.realTimeDetectData） */
export interface ScreenRealtimeData {
  total: number
  ngCount: number
  removeTotal: number
  removeFail: number
  efficiency: number
  totalNgRate: number
  occupancy: number
  occupancyRate: number
  startTime?: string
  defects?: Array<{ type: string; count: number; showFlag?: number }>
}

/** 单条线（含实时数据） */
export interface ScreenLine {
  lineId: number
  lineNo: string
  faceNo: string
  order: number
  color: string
  realTimeDetectData: ScreenRealtimeData | null
  removeTotal: number
  hourDefectCount?: Array<{ defectName: string; defectCount: number | null }>
}

/** 单条线设备状态（来自 ScreenDataDTO.clientStatusList） */
export interface ScreenClientStatus {
  lineId: number
  lineNo: string
  faceNo: string
  order: number
  cameraStatus: string
  eliminatorStatus: string
  clientStatus: string
}

/** 完整快照（服务端每 5s 推一次） */
export interface ScreenSnapshot {
  /** 推上来的原始 payload（兼容 Screen.vue 老逻辑） */
  raw: any
  /** 拆好的线列表 */
  lines: ScreenLine[]
  /** 设备状态列表 */
  clientStatuses: ScreenClientStatus[]
  /** 当日缺陷累计（来自 defectSum） */
  defectSum: Array<{ defectName: string; defectCount: number }>
  /** 服务端推送时间戳（前端收到时） */
  ts: number
  /** 来源标记 */
  source: 'ws'
}

// -----------------------------------------------------------------------------
// 全局响应式状态（Pinia-free，用 reactive() 实现；多个组件共享同一引用）
// -----------------------------------------------------------------------------

interface ScreenGlobalState {
  snapshot: ScreenSnapshot | null
  lastUpdate: number
  wsState: WsState
  connected: boolean
  uid: string
  subscriberCount: number
}

export const screenState = reactive<ScreenGlobalState>({
  snapshot: null,
  lastUpdate: 0,
  wsState: 'idle',
  connected: false,
  uid: '',
  subscriberCount: 0
})

/** 计算属性：是否有快照 */
export const hasScreenSnapshot: ComputedRef<boolean> = computed(() => screenState.snapshot !== null)

/** 计算属性：是否在线 */
export const isScreenLive: ComputedRef<boolean> = computed(() => screenState.wsState === 'open')

// -----------------------------------------------------------------------------
// 运行时（WS controller + subscribers 持有在模块作用域，避免 Pinia 序列化）
// -----------------------------------------------------------------------------

interface Subscriber {
  id: number
  cb: (snapshot: ScreenSnapshot) => void
}

const runtime = {
  ctrl: null as WsController | null,
  subscribers: [] as Subscriber[],
  subIdCounter: 0,
  initialized: false
}

function parseScreenPayload(raw: any): ScreenSnapshot {
  // 兼容：(payload.data ?? payload) 两种后端形态（Alarm.vue / Screen.vue 既有代码风格）
  const inner = raw?.data ?? raw
  const detectData = Array.isArray(inner?.detectData) ? inner.detectData : []
  const clientStatusList = Array.isArray(inner?.clientStatusList) ? inner.clientStatusList : []
  const defectSum = Array.isArray(inner?.defectSum) ? inner.defectSum : []

  const lines: ScreenLine[] = detectData.map((d: any) => ({
    lineId: Number(d.lineId ?? 0),
    lineNo: String(d.lineNo ?? ''),
    faceNo: String(d.faceNo ?? ''),
    order: Number(d.order ?? 0),
    color: String(d.color ?? ''),
    realTimeDetectData: d.realTimeDetectData ?? null,
    removeTotal: Number(d.removeTotal ?? 0),
    hourDefectCount: Array.isArray(d.hourDefectCount) ? d.hourDefectCount : []
  }))

  const clientStatuses: ScreenClientStatus[] = clientStatusList.map((c: any) => ({
    lineId: Number(c.lineId ?? 0),
    lineNo: String(c.lineNo ?? ''),
    faceNo: String(c.faceNo ?? ''),
    order: Number(c.order ?? 0),
    cameraStatus: String(c.cameraStatus ?? ''),
    eliminatorStatus: String(c.eliminatorStatus ?? ''),
    clientStatus: String(c.clientStatus ?? '')
  }))

  const defectSumParsed = defectSum.map((d: any) => ({
    defectName: String(d.defectName ?? ''),
    defectCount: Number(d.defectCount ?? 0)
  }))

  return {
    raw: inner,
    lines,
    clientStatuses,
    defectSum: defectSumParsed,
    ts: Date.now(),
    source: 'ws'
  }
}

// -----------------------------------------------------------------------------
// 公开 API
// -----------------------------------------------------------------------------

/**
 * 建立全局 WS 连接（App.vue onMounted 调用一次）。
 *
 * 幂等：多次调用仅保留一个 controller。
 */
export function connectScreenSingleton(): void {
  if (runtime.initialized && runtime.ctrl) return

  const userStore = useUserStore()
  const uid = userStore.id ? String(userStore.id) : 'web'
  screenState.uid = uid

  runtime.ctrl = connectScreenWs(
    uid,
    (msg: ScreenWsMessage) => {
      if (!msg || typeof msg !== 'object') return
      const type = String(msg.type || '')
      // 服务端 ScreenServiceImpl.sendScreenDataInfo() 推的就是 type="screen"
      if (type !== 'screen') return
      const payload = (msg as any).payload ?? (msg as any).data ?? null
      if (!payload || typeof payload !== 'object') return

      const snap = parseScreenPayload(payload)
      screenState.snapshot = snap
      screenState.lastUpdate = Date.now()

      // fan-out 给所有订阅者
      for (const sub of runtime.subscribers) {
        try {
          sub.cb(snap)
        } catch (err) {
          // eslint-disable-next-line no-console
          console.warn('[screenStore] subscriber callback threw:', err)
        }
      }
    },
    (s) => {
      screenState.wsState = s
    }
  )
  runtime.ctrl.open()
  runtime.initialized = true
  screenState.connected = true
}

/**
 * 关闭全局 WS 连接。
 *
 * @param force=true 时强制断开；否则仅在订阅者全释放时关闭（keep-alive 风格）。
 */
export function disconnectScreenSingleton(force = false): void {
  if (!runtime.ctrl) return
  if (!force && runtime.subscribers.length > 0) return
  try {
    runtime.ctrl.close()
  } catch {
    /* ignore */
  }
  runtime.ctrl = null
  runtime.initialized = false
  screenState.connected = false
  screenState.wsState = 'closed'
}

/**
 * 订阅快照更新（被 RealTime.vue / Screen.vue 等调用）。
 *
 * - 立即用最新快照（若有）触发一次回调，确保首屏 0ms 渲染。
 * - 返回 unsubscribe 函数。
 */
export function subscribeScreen(cb: (snapshot: ScreenSnapshot) => void): () => void {
  const id = ++runtime.subIdCounter
  runtime.subscribers.push({ id, cb })
  screenState.subscriberCount = runtime.subscribers.length

  // 首屏快照直推（已有数据则立刻触发一次，避免组件再发一次 REST 拉取）
  if (screenState.snapshot) {
    try {
      cb(screenState.snapshot)
    } catch {
      /* ignore */
    }
  }
  return () => {
    runtime.subscribers = runtime.subscribers.filter((s) => s.id !== id)
    screenState.subscriberCount = runtime.subscribers.length
    // 订阅归零时不强 disconnect；路由切换不会触发重连。
    // 想强制断开请在 App.vue onBeforeUnmount 调 disconnectScreenSingleton(true)。
  }
}
