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
// W-RT-7：拖拽排序支持
//   - reorder(fromIdx, toIdx) 临时调整顺序 → 立即更新本地状态（乐观 UI）
//     → 调 PUT /web/line/order 持久化 → 失败回滚 + 重新 load
//   - 排序依据：lineOrder 字段（lineOrder = idx + 1，从 1 开始）
//   - 选中的行（selectedLineKey）会随拖动保持指向同一 line（lineKey 不变）
//
// 注：本 store 只为 W-RT-2 的左栏做最小集，后续 RT-3（缺陷类型子表）/RT-4（设备状态）
//     再决定要不要扩展。
// =============================================================================

import { defineStore } from 'pinia'
import {
  listLine,
  parseRealtimeData,
  updateLineOrder,
  type LineItem,
  type LineOrderItem,
  type RealtimeDetectData
} from '../api/realtime'

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
  /**
   * W-RT-7：拖拽排序字段（1-based）。
   * 注：后端 line_order 表与 line 表是分离的（详见 W-LIN-05）；
   *     前端按列表 idx+1 维护一个临时 lineOrder 用于 PUT /web/line/order 持久化。
   */
  lineOrder: number
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
  /**
   * W-RT-7：拖拽中的源索引（-1 = 无）。
   * 用于列表渲染占位线（玻璃风半透明）。
   */
  dragFromIdx: number
  /**
   * W-RT-7：拖拽中的目标索引（-1 = 无）。
   * 用于列表渲染插入位置（占位线位置）。
   */
  dragOverIdx: number
  /**
   * W-RT-7：最近一次排序是否成功（用于 UI 提示）。
   */
  lastReorderSuccess: boolean
}

export const useLineStore = defineStore('line', {
  state: (): LineState => ({
    lines: [],
    loaded: false,
    loading: false,
    selectedLineKey: '',
    errorMsg: '',
    dragFromIdx: -1,
    dragOverIdx: -1,
    lastReorderSuccess: true
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
          this.lines = resp.data.map((raw, idx) => {
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
              // W-RT-7：初始 lineOrder 按列表 idx+1 占位
              // （首次 load 后端 line_order 表可能为空或部分填充；
              //  这里给一个本地连续序列，触发 reorder 时整体覆盖后端即可）
              lineOrder: idx + 1,
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
    },

    /**
     * W-RT-7：拖拽中 — 标记源 / 目标索引（不修改数据）。
     * - fromIdx: 拖起的行 idx（dragstart 时设）
     * - overIdx: 当前悬停的目标行 idx（dragover 时设）
     */
    setDragState(fromIdx: number, overIdx: number): void {
      this.dragFromIdx = fromIdx
      this.dragOverIdx = overIdx
    },

    /** W-RT-7：拖拽结束 / 取消 — 清空 drag 标记 */
    clearDragState(): void {
      this.dragFromIdx = -1
      this.dragOverIdx = -1
    },

    /**
     * W-RT-7：拖拽排序。
     *
     * 流程：
     *  1. 备份当前 lines（用于失败回滚）
     *  2. 临时调整 lines（splice + 重置 lineOrder = idx+1）
     *  3. PUT /web/line/order 持久化全部线别（后端要求 size == 总数）
     *  4. 成功 → 更新 lastReorderSuccess
     *  5. 失败 → 还原 backup + 重新 load（force=true 拿后端真值）+ 抛出错误
     *
     * 注意：
     *  - selectedLineKey 是按 lineKey 查找的，splice 后仍能正确指向同一行；
     *    所以**不需要**额外修 selectedLineKey。
     *  - 后端 lineOrderService.modLineOrder 是「全删 + 全插」，所以必须传全部行；
     *    不能只传 from/to 两个受影响行。
     */
    async reorder(fromIdx: number, toIdx: number): Promise<void> {
      if (fromIdx === toIdx) {
        this.clearDragState()
        return
      }
      if (
        fromIdx < 0 ||
        toIdx < 0 ||
        fromIdx >= this.lines.length ||
        toIdx >= this.lines.length
      ) {
        this.clearDragState()
        return
      }

      // 1) 备份
      const backup = this.lines.map((l) => ({ ...l }))

      // 2) 乐观更新本地顺序
      const moved = this.lines.splice(fromIdx, 1)[0]
      this.lines.splice(toIdx, 0, moved)
      // 重置 lineOrder = idx + 1（保持本地序列与新顺序一致）
      this.lines = this.lines.map((l, i) => ({ ...l, lineOrder: i + 1 }))

      // 3) 持久化到后端
      const payload: LineOrderItem[] = this.lines.map((l, i) => ({
        lineId: l.id,
        order: i + 1
      }))
      try {
        const resp = await updateLineOrder(payload)
        if (!resp || !resp.success) {
          throw new Error(resp?.message || '排序保存失败')
        }
        this.lastReorderSuccess = true
        this.clearDragState()
      } catch (err: any) {
        // 5) 失败回滚
        this.lines = backup
        this.lastReorderSuccess = false
        this.clearDragState()
        // 强制 reload 后端真值（避免本地 backup 也不可信）
        try {
          await this.load(true)
        } catch {
          /* load 已经自己 try/catch 了；这里静默 */
        }
        throw err
      }
    }
  }
})
