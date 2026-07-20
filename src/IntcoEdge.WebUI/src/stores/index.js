import { defineStore } from 'pinia'
import { fetchLines, fetchCameras, fetchDefects, fetchAlarms } from '@/api'

/**
 * 全局状态 store
 * 当前主要存"产线 / 摄像头"基础数据，多个视图共享。
 * 视图级局部状态仍用 ref/computed，不污染全局。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    lines: [],
    cameras: [],
    defects: [],
    alarms: [],
    loading: {
      lines: false,
      cameras: false,
      defects: false,
      alarms: false
    },
    lastUpdated: {
      lines: null,
      cameras: null,
      defects: null,
      alarms: null
    }
  }),

  getters: {
    /** 在线产线数 */
    runningLineCount: (state) => state.lines.filter((l) => l.status === 'running').length,
    /** 24h 总缺陷数 */
    totalDefects24h: (state) => state.lines.reduce((sum, l) => sum + (l.defectCount24h || 0), 0),
    /** 未确认报警数 */
    unacknowledgedAlarmCount: (state) => state.alarms.filter((a) => !a.acknowledged).length,
    /** 在线摄像头数 */
    onlineCameraCount: (state) => state.cameras.filter((c) => c.status === 'online').length
  },

  actions: {
    async loadLines() {
      this.loading.lines = true
      try {
        this.lines = await fetchLines()
        this.lastUpdated.lines = new Date().toISOString()
      } finally {
        this.loading.lines = false
      }
    },

    async loadCameras(lineId) {
      this.loading.cameras = true
      try {
        this.cameras = await fetchCameras(lineId)
        this.lastUpdated.cameras = new Date().toISOString()
      } finally {
        this.loading.cameras = false
      }
    },

    async loadDefects(params) {
      this.loading.defects = true
      try {
        this.defects = await fetchDefects(params)
        this.lastUpdated.defects = new Date().toISOString()
      } finally {
        this.loading.defects = false
      }
    },

    async loadAlarms() {
      this.loading.alarms = true
      try {
        this.alarms = await fetchAlarms()
        this.lastUpdated.alarms = new Date().toISOString()
      } finally {
        this.loading.alarms = false
      }
    },

    /** 全量刷新（在 Dashboard 顶部"刷新"按钮触发） */
    async refreshAll() {
      await Promise.all([
        this.loadLines(),
        this.loadCameras(),
        this.loadAlarms()
      ])
    }
  }
})
