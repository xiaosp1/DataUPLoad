// =============================================================================
// W-RT-8 全局未处理报警徽章 store（玻璃风 AlarmHint 数据源）
//
// 设计要点（参考 stores/screen.ts 的 singleton 思路）：
//   1) 全局单例：整个 App 只连一个 /ws?type=alarm；切路由不重连。
//      - App.vue onMounted 调用 connectAlarmSingleton()；卸载 disconnectAlarmSingleton(true)
//   2) 数据源：
//      - 基线：组件首次订阅时若 recent 还为空，主动拉一次
//             GET /web/alarm/list?pageSize=5&solve=2&sortType=1 拿最近 5 条未处理报警
//      - 增量：WS /ws?type=alarm 推送（与 Alarm.vue 同款 fan-in 解析逻辑）
//   3) 与 Alarm.vue 共存：
//      - Alarm.vue 自己 connectWs 后持有独立 controller；本 store 再连一份也不会
//        互相挤掉（同 type 不同 controller 是 framework-starter 允许的——
//        WebSocketHandler.sendByType(type, msg) 是 broadcast 给所有该 type 的连接）。
//      - 唯一约束：本 store 只服务于"未处理报警徽章"，wsState 不影响 Alarm 页。
//   4) 不引入新依赖；走 utils/ws.ts + api/alarm.ts + stores/user.ts。
// =============================================================================

import { reactive, computed, type ComputedRef } from 'vue'
import { createWs, type WsController, type WsState, type WsMessage } from '../utils/ws'
import { listAlarm, type AlarmRecord } from '../api/alarm'
import { useUserStore } from './user'

// -----------------------------------------------------------------------------
// 类型
// -----------------------------------------------------------------------------

const RECENT_LIMIT = 5

/** 待消费的最新报警（带未处理标记） */
export interface AlarmHintItem {
  id: number | string
  uuid: string
  time: string
  lineNo: string
  faceNo: string
  level: number
  type: number
  message: string
}

interface AlarmGlobalState {
  /** 最近未处理报警（最多 RECENT_LIMIT 条；最新在前） */
  recent: AlarmHintItem[]
  /** 未处理报警总数（用于徽章角标） */
  pending: number
  /** WS 状态 */
  wsState: WsState
  /** 是否已建立 WS 连接 */
  connected: boolean
  /** 当前 uid */
  uid: string
  /** 订阅者数量（Badge 组件 mounted → +1，unmounted → -1） */
  subscriberCount: number
  /** 首次基线加载是否完成（避免 Badge 一直转圈） */
  baselineLoaded: boolean
  /** 上次基线加载时间戳 */
  lastBaselineTs: number
}

export const alarmState = reactive<AlarmGlobalState>({
  recent: [],
  pending: 0,
  wsState: 'idle',
  connected: false,
  uid: '',
  subscriberCount: 0,
  baselineLoaded: false,
  lastBaselineTs: 0
})

// -----------------------------------------------------------------------------
// 计算属性
// -----------------------------------------------------------------------------

export const hasAlarmHint: ComputedRef<boolean> = computed(() => alarmState.recent.length > 0)
export const isAlarmWsOpen: ComputedRef<boolean> = computed(() => alarmState.wsState === 'open')

// -----------------------------------------------------------------------------
// 内部工具
// -----------------------------------------------------------------------------

function normalize(rec: any): AlarmHintItem | null {
  if (!rec || typeof rec !== 'object') return null
  const id = rec.id ?? rec.uuid ?? ''
  if (!id) return null
  return {
    id,
    uuid: String(rec.uuid ?? ''),
    time: String(rec.time ?? ''),
    lineNo: String(rec.lineNo ?? ''),
    faceNo: String(rec.faceNo ?? ''),
    level: Number(rec.level ?? 1),
    type: Number(rec.type ?? 1),
    message: String(rec.message ?? '')
  }
}

/** 把一条新报警插到最近列表头（去重 + 截断） */
function pushRecent(rec: AlarmHintItem) {
  if (!rec) return
  // 去重：同 id 或同 uuid 视为同一报警
  const dupIdx = alarmState.recent.findIndex((r) =>
    (rec.uuid && r.uuid === rec.uuid) || String(r.id) === String(rec.id)
  )
  if (dupIdx === 0) {
    // 已在最前，仅替换内容（time/message 可能后端补齐）
    alarmState.recent[0] = rec
    return
  }
  if (dupIdx > 0) alarmState.recent.splice(dupIdx, 1)
  alarmState.recent.unshift(rec)
  if (alarmState.recent.length > RECENT_LIMIT) {
    alarmState.recent.length = RECENT_LIMIT
  }
}

// -----------------------------------------------------------------------------
// 运行时（WS controller + 订阅者，模块作用域，避免 Pinia 序列化丢失）
// -----------------------------------------------------------------------------

interface Subscriber {
  id: number
  cb: (item: AlarmHintItem) => void
}

const runtime = {
  ctrl: null as WsController | null,
  subscribers: [] as Subscriber[],
  subIdCounter: 0,
  initialized: false
}

/**
 * 加载基线：从 REST 拿最近 5 条未处理报警。
 *
 * 幂等：失败不抛错，最多打 console.warn；Badge 组件会显示 0。
 */
export async function loadAlarmBaseline(): Promise<void> {
  try {
    const resp = await listAlarm({
      pageNum: 1,
      pageSize: RECENT_LIMIT,
      solve: 2, // 2 = 未处理
      sortType: 1 // 1 = 降序
    })
    if (resp && resp.success !== false && resp.data) {
      const records = (resp.data as any).records ?? []
      alarmState.recent = []
      for (const r of records) {
        const item = normalize(r)
        if (item) alarmState.recent.push(item)
      }
      // 头部 → 最新在前的语义
      alarmState.recent = alarmState.recent.slice(0, RECENT_LIMIT)
      alarmState.pending = Number((resp.data as any).total ?? alarmState.recent.length)
    } else {
      // 后端返回 code != 0：保留旧值（避免 UI 跳变），只记日志
      // eslint-disable-next-line no-console
      console.warn('[alarmStore] baseline returned non-ok envelope', resp)
    }
  } catch (err) {
    // eslint-disable-next-line no-console
    console.warn('[alarmStore] loadAlarmBaseline failed', err)
  } finally {
    alarmState.baselineLoaded = true
    alarmState.lastBaselineTs = Date.now()
  }
}

/**
 * 建立全局 WS 连接（App.vue onMounted 调用一次，幂等）。
 */
export function connectAlarmSingleton(): void {
  if (runtime.initialized && runtime.ctrl) return

  const userStore = useUserStore()
  const uid = userStore.id ? String(userStore.id) : 'web'
  alarmState.uid = uid

  runtime.ctrl = createWs({
    uid,
    type: 'alarm',
    onMessage(msg: WsMessage) {
      // 兼容多种后端推送形态（与 Alarm.vue pushIncomingAlarm 一致）：
      //   { type: 'alarm', data: <AlarmRecord | AlarmRecord[] | { data: AlarmRecord }> }
      //   { type: 'alarm', payload: <...> }
      //   { type: 'push-alarm' | 'new-alarm', payload: any }
      const tp = String(msg?.type || '')
      // 后端 WsMessage.build().data(alarms) 序列化后是 data 字段（不是 payload）
      const raw = (msg as any)?.data ?? (msg as any)?.payload
      if (!raw || typeof raw !== 'object') return

      // 后端 sendAlarmTextMessage() 推的是未处理报警**数组**（data: [...]），
      // 前端必须逐条 normalize 才能进 recent；只认单条会全部丢弃。
      const list = Array.isArray(raw) ? raw : ((raw as any)?.data ?? raw)
      const arr = Array.isArray(list) ? list : [list]
      for (const item of arr) {
        let alarm: AlarmHintItem | null = null
        if (tp === 'alarm' || tp === 'push-alarm' || tp === 'new-alarm') {
          if (item && typeof item === 'object' && (item.id || item.uuid || item.message || item.time)) {
            alarm = normalize(item)
          }
        }
        if (!alarm) continue
        // 增量 → recent 头插 + pending++
        pushRecent(alarm)
        alarmState.pending += 1
        // 通知所有订阅者（Badge 弹窗动画/声音用）
        for (const sub of runtime.subscribers) {
          try {
            sub.cb(alarm)
          } catch (err) {
            // eslint-disable-next-line no-console
            console.warn('[alarmStore] subscriber callback threw:', err)
          }
        }
      }
    },
    onState(s: WsState) {
      alarmState.wsState = s
    }
  })
  runtime.ctrl.open()
  runtime.initialized = true
  alarmState.connected = true

  // 同时拉基线（不阻塞）
  void loadAlarmBaseline()
}

/**
 * 关闭全局 WS 连接。
 *
 * @param force=true 时强制断开；否则仅在订阅者全释放时关闭（keep-alive 风格）。
 */
export function disconnectAlarmSingleton(force = false): void {
  if (!runtime.ctrl) return
  if (!force && runtime.subscribers.length > 0) return
  try {
    runtime.ctrl.close()
  } catch {
    /* ignore */
  }
  runtime.ctrl = null
  runtime.initialized = false
  alarmState.connected = false
  alarmState.wsState = 'closed'
}

/**
 * 订阅新增未处理报警（Badge 组件 mounted/unmounted 时调用）。
 *
 * - 立即用现有列表头一条触发一次回调（保证 UI 第一帧就是最新的）。
 * - 返回 unsubscribe 函数。
 */
export function subscribeAlarmHint(cb: (item: AlarmHintItem) => void): () => void {
  const id = ++runtime.subIdCounter
  runtime.subscribers.push({ id, cb })
  alarmState.subscriberCount = runtime.subscribers.length
  // 首屏数据直推：Badge 拿 latest 渲染（如果有）
  if (alarmState.recent.length > 0) {
    try {
      cb(alarmState.recent[0])
    } catch {
      /* ignore */
    }
  }
  return () => {
    runtime.subscribers = runtime.subscribers.filter((s) => s.id !== id)
    alarmState.subscriberCount = runtime.subscribers.length
    // 订阅归零时不强 disconnect；路由切换不会触发重连。
    // 想强制断开请在 App.vue onBeforeUnmount 调 disconnectAlarmSingleton(true)。
  }
}

/**
 * 把一条标记为"已忽略"的报警从 recent 中移除（并扣减 pending）。
 * Alarm 页 ignore 操作后调用本方法，避免 Badge 与 Alarm 页状态不一致。
 */
export function markIgnored(alarmIdOrUuid: string | number): void {
  if (alarmIdOrUuid === undefined || alarmIdOrUuid === null) return
  const before = alarmState.recent.length
  alarmState.recent = alarmState.recent.filter(
    (r) => String(r.id) !== String(alarmIdOrUuid) && r.uuid !== String(alarmIdOrUuid)
  )
  if (alarmState.recent.length < before) {
    alarmState.pending = Math.max(0, alarmState.pending - 1)
  }
}
