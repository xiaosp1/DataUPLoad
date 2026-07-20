/**
 * Mock 数据
 *
 * W-A3 还没完工前，前端展示用。
 * 数据规模按 PSM 车间真实情况模拟：10 条产线 / 120 个摄像头 / 30 条缺陷 / 5 条报警。
 *
 * 字段命名尽量与 W-A3 后续真接口对齐，减少切换成本。
 */

// ---------- 产线（10 条） ----------
export const lines = [
  { id: 'L01', name: '1# 挤出线', workshop: 'A 区', status: 'running', cameraCount: 12, defectCount24h: 38, oee: 0.86 },
  { id: 'L02', name: '2# 挤出线', workshop: 'A 区', status: 'running', cameraCount: 12, defectCount24h: 24, oee: 0.91 },
  { id: 'L03', name: '3# 挤出线', workshop: 'A 区', status: 'warning', cameraCount: 12, defectCount24h: 56, oee: 0.72 },
  { id: 'L04', name: '4# 挤出线', workshop: 'A 区', status: 'running', cameraCount: 12, defectCount24h: 31, oee: 0.83 },
  { id: 'L05', name: '5# 挤出线', workshop: 'B 区', status: 'running', cameraCount: 12, defectCount24h: 19, oee: 0.89 },
  { id: 'L06', name: '6# 挤出线', workshop: 'B 区', status: 'running', cameraCount: 12, defectCount24h: 27, oee: 0.85 },
  { id: 'L07', name: '7# 挤出线', workshop: 'B 区', status: 'stopped', cameraCount: 12, defectCount24h: 0, oee: 0.00 },
  { id: 'L08', name: '8# 挤出线', workshop: 'B 区', status: 'running', cameraCount: 12, defectCount24h: 22, oee: 0.88 },
  { id: 'L09', name: '9# 挤出线', workshop: 'C 区', status: 'warning', cameraCount: 12, defectCount24h: 47, oee: 0.68 },
  { id: 'L10', name: '10# 挤出线', workshop: 'C 区', status: 'running', cameraCount: 12, defectCount24h: 33, oee: 0.81 }
]

// ---------- 摄像头（10 线 × 12 = 120） ----------
const cameraTypes = ['挤出机头', '真空定型', '牵引', '切割', '堆垛']
const cameraStatuses = ['online', 'online', 'online', 'online', 'online', 'online', 'online', 'offline', 'warning']

export const cameras = []
for (const line of lines) {
  for (let i = 1; i <= 12; i++) {
    const idx = (lines.indexOf(line) * 12 + i)
    cameras.push({
      id: `${line.id}-C${String(i).padStart(2, '0')}`,
      lineId: line.id,
      lineName: line.name,
      name: `${cameraTypes[i % cameraTypes.length]} 相机 #${i}`,
      type: cameraTypes[i % cameraTypes.length],
      ip: `192.168.${10 + lines.indexOf(line)}.${20 + i}`,
      status: cameraStatuses[idx % cameraStatuses.length],
      fps: cameraStatuses[idx % cameraStatuses.length] === 'online' ? 24 + (idx % 5) : 0,
      lastHeartbeat: '2026-07-20 16:45:00'
    })
  }
}

// ---------- 缺陷（30 条，覆盖 24h） ----------
const defectTypes = ['气泡', '杂质', '划痕', '变形', '色差', '尺寸偏差']
const severities = ['low', 'medium', 'high', 'critical']

export const defects = []
const now = new Date()
for (let i = 0; i < 30; i++) {
  const line = lines[i % lines.length]
  const t = new Date(now.getTime() - i * 47 * 60 * 1000)
  defects.push({
    id: `D${String(24072000 + i)}`,
    lineId: line.id,
    lineName: line.name,
    cameraId: `${line.id}-C${String((i % 12) + 1).padStart(2, '0')}`,
    type: defectTypes[i % defectTypes.length],
    severity: severities[i % severities.length],
    confidence: 0.75 + ((i * 7) % 25) / 100,
    occurredAt: t.toISOString().replace('T', ' ').slice(0, 19),
    imageUrl: null, // TODO: 接入海康图片服务后填真 URL
    resolved: i > 20
  })
}

// ---------- 缺陷趋势（24h 按小时聚合） ----------
export const defectTrend = {
  hours: Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`),
  // 三条线：A 区、B 区、C 区
  series: [
    {
      name: 'A 区（1#-4#）',
      data: [2, 1, 0, 0, 0, 1, 3, 8, 12, 15, 18, 22, 20, 24, 26, 23, 19, 15, 12, 8, 5, 3, 2, 1]
    },
    {
      name: 'B 区（5#-8#）',
      data: [1, 0, 0, 0, 1, 2, 5, 9, 11, 14, 17, 20, 19, 21, 23, 22, 18, 14, 11, 7, 4, 2, 1, 0]
    },
    {
      name: 'C 区（9#-10#）',
      data: [0, 0, 0, 0, 0, 1, 2, 4, 6, 8, 10, 12, 13, 14, 15, 13, 11, 9, 7, 5, 3, 2, 1, 0]
    }
  ]
}

// ---------- 产线缺陷排行 ----------
export const defectRanking = lines
  .map((l) => ({ id: l.id, name: l.name, value: l.defectCount24h }))
  .sort((a, b) => b.value - a.value)

// ---------- 报警（5 条最新） ----------
export const alarms = [
  {
    id: 'A001',
    level: 'critical',
    lineId: 'L09',
    lineName: '9# 挤出线',
    type: '气泡超标',
    message: '9# 挤出线气泡缺陷连续 30 秒超过阈值（10/min），请现场排查',
    occurredAt: '2026-07-20 16:42:18',
    acknowledged: false
  },
  {
    id: 'A002',
    level: 'warning',
    lineId: 'L03',
    lineName: '3# 挤出线',
    type: '相机离线',
    message: '3# 挤出线 L03-C07 相机心跳超时，请检查网络',
    occurredAt: '2026-07-20 16:35:02',
    acknowledged: false
  },
  {
    id: 'A003',
    level: 'warning',
    lineId: 'L07',
    lineName: '7# 挤出线',
    type: '全线停机',
    message: '7# 挤出线全线停机，原因待查',
    occurredAt: '2026-07-20 16:21:44',
    acknowledged: true
  },
  {
    id: 'A004',
    level: 'info',
    lineId: 'L01',
    lineName: '1# 挤出线',
    type: 'OEE 达标',
    message: '1# 挤出线 OEE 连续 1 小时 ≥ 85%',
    occurredAt: '2026-07-20 15:50:11',
    acknowledged: true
  },
  {
    id: 'A005',
    level: 'warning',
    lineId: 'L09',
    lineName: '9# 挤出线',
    type: '杂质超标',
    message: '9# 挤出线杂质缺陷 5 分钟内 8 次',
    occurredAt: '2026-07-20 15:38:09',
    acknowledged: false
  }
]
