// =============================================================================
// W-RT-2 左栏线别列表 store
//
// 数据源：GET /web/line/list → 返回 LineItem[]
//   - 单条字段：id / name / lineNo / faceNo / clientNo / color / realtimeData(JSON 字符串)
//   - LineDTO 真实字段没有 hourDefectCount / hourRemoveCount（PSM 老 SPA 自定义聚合）
//     简化版：当前小时缺陷 = realtimeData.ngCount；当前小时剔除 = realtimeData.removeTotal
//
// 关键设计：lineNo 在我们系统里**不唯一**（同一 lineNo 下可能有 A1/A2 多个面）。
//  所以用 lineKey = `${lineNo}:${faceNo}`（后端返回的 key 字段）作唯一标识。
//  selectedLineKey 是真正的"选中行指针"，lineStore.selectedLine getter 返回那一行。
//
// 注：本 store 只为 W-RT-2 的左栏做最小集，后续 RT-3（缺陷类型子表）/RT-4（设备状态）
//     再决定要不要扩展。
// =============================================================================

import { defineStore } from 'pinia'
import { listLine, parseRealtimeData, type LineItem, type RealtimeDetectData } from '../api/realtime'

/** 左栏卡片消费的最小视图模型 */
export interface LineListItem {
  /** 唯一键：lineNo:faceNo */
  lineKey: string
  id: number
  name: string
  lineNo: string
  faceNo: string
  /** PSM 风格：序号彩色块；没有 color 字段时退化为 accent */
  color: string
  /** 当小时缺陷总数（= realtimeData.ngCount，没聚合就是当前实时值） */
  hourDefectCount: number
  /** 当小时剔除总数（= realtimeData.removeTotal） */
  hourRemoveCount: number
  realtime: RealtimeDetectData | null
  raw: LineItem
}

interface LineState {
  lines: LineListItem[]
  loaded: boolean
  loading: boolean
  /** 当前选中的 lineKey（卡片里被点亮的项；空 = 还没选） */
  selectedLineKey: string
  /** 最近一次错误（UI 兜底用） */
  errorMsg: string
}

export const useLineStore = defineStore('line', {
  state: (): LineState => ({
    lines: [],
    loaded: false,
    loading: false,
    selectedLineKey: '',
    errorMsg: ''
  }),

  getters: {
    /** 选中那行（没有 = null） */
    selectedLine(state): LineListItem | null {
      if (!state.selectedLineKey) return null
      return state.lines.find((l) => l.lineKey === state.selectedLineKey) || null
    },
    /** 选中行的 realtime 详情（中栏喂数据用） */
    selectedRealtime(): RealtimeDetectData | null {
      // getter 里访问 selectedLine 会自动收集依赖（reactive）
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const sl = (this as any).selectedLine as LineListItem | null
      return sl?.realtime ?? null
    },
    /** 列表条数（KPI 共用） */
    total(): number {
      return this.lines.length
    }
  },

  actions: {
    /**
     * 拉取线别列表（PSM 用相同端点）。
     * 已加载过的话默认不再重复拉；传 force=true 强制刷新。
     */
    async load(force = false): Promise<void> {
      if (this.loading) return
      if (this.loaded && !force) return
      this.loading = true
      this.errorMsg = ''
      try {
        const resp = await listLine()
        if (resp.success && Array.isArray(resp.data)) {
          this.lines = resp.data.map((raw) => {
            const rt = parseRealtimeData(raw.realtimeData)
            // PSM 老 SPA 的 hourDefectCount / hourRemoveCount 是后端聚合字段，
            // 我们 LineDTO 没这俩字段；用实时值兜底，保证左栏一定有数字。
            // 优先用 line.color（后端 LineDTO 字段），没有则用默认 accent。
            const lineColor = (raw as LineItem & { color?: string }).color || '#5ce1ff'
            // 唯一键：优先用后端 key 字段，否则拼 lineNo:faceNo
            const lineKey = (raw as LineItem & { key?: string }).key || `${raw.lineNo}:${raw.faceNo}`
            return {
              lineKey,
              id: raw.id,
              name: raw.name,
              lineNo: raw.lineNo,
              faceNo: raw.faceNo,
              color: lineColor,
              hourDefectCount: rt?.ngCount ?? 0,
              hourRemoveCount: rt?.removeTotal ?? 0,
              realtime: rt,
              raw
            }
          })
          this.loaded = true
          // 默认选第一条
          if (!this.selectedLineKey && this.lines.length > 0) {
            this.selectedLineKey = this.lines[0].lineKey
          }
        } else {
          this.lines = []
          this.errorMsg = resp.message || '线别数据加载失败'
        }
      } catch (err: any) {
        this.lines = []
        this.errorMsg = err?.message || String(err)
      } finally {
        this.loading = false
      }
    },

    /** 单击行：选中（lineKey 唯一） */
    select(lineKey: string): void {
      this.selectedLineKey = lineKey
    }
  }
})
