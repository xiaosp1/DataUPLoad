<template>
  <div class="demo-shell">
    <GlassPage title="W-FRONT-02-B 设计 Token + 玻璃组件库演示" subtitle="苹果系玻璃风格 · Element Plus 主题重写 · 5 个核心组件（Card / Button / MenuItem / Table / Page）">
      <template #actions>
        <div class="demo-actions">
          <GlassButton variant="default" @click="onReset">重置</GlassButton>
          <GlassButton variant="primary" @click="onSubmit">登录演示</GlassButton>
          <GlassButton variant="danger" @click="onDanger">退出</GlassButton>
        </div>
      </template>

      <div class="demo-grid">
        <!-- 左侧：玻璃药丸菜单 -->
        <GlassCard :padding="16" class="demo-grid__sidebar">
          <div class="demo-grid__menu-label">实时监控</div>
          <nav class="demo-grid__menu">
            <GlassMenuItem
              v-for="item in menuItems"
              :key="item.key"
              :icon="item.icon"
              :active="activeMenu === item.key"
              @click="activeMenu = item.key"
            >
              {{ item.label }}
            </GlassMenuItem>
          </nav>

          <div class="demo-grid__menu-label" style="margin-top: 20px">系统管理</div>
          <nav class="demo-grid__menu">
            <GlassMenuItem
              v-for="item in adminItems"
              :key="item.key"
              :icon="item.icon"
              :active="activeMenu === item.key"
              @click="activeMenu = item.key"
            >
              {{ item.label }}
            </GlassMenuItem>
          </nav>
        </GlassCard>

        <!-- 右侧：内容区 -->
        <div class="demo-grid__main">
          <!-- KPI 行 -->
          <div class="demo-grid__kpis">
            <GlassCard v-for="kpi in kpis" :key="kpi.label" :padding="20" :hover="true">
              <div class="kpi">
                <div class="kpi__label">{{ kpi.label }}</div>
                <div class="kpi__value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
                <div class="kpi__trend" :class="kpi.trend > 0 ? 'kpi__trend--up' : 'kpi__trend--down'">
                  {{ kpi.trend > 0 ? '↑' : '↓' }} {{ Math.abs(kpi.trend) }}% vs 昨日
                </div>
              </div>
            </GlassCard>
          </div>

          <!-- 表格行 -->
          <GlassCard :padding="20" class="demo-grid__table-card">
            <template #header>
              <div class="demo-grid__table-head">
                <h3 class="demo-grid__table-title">实时报警</h3>
                <GlassButton variant="default" @click="onViewMore">查看全部</GlassButton>
              </div>
            </template>
            <GlassTable :data="alarmRows" stripe>
              <el-table-column prop="time" label="时间" width="120" />
              <el-table-column prop="line" label="产线" width="120" />
              <el-table-column prop="device" label="设备" width="120" />
              <el-table-column prop="type" label="告警类型" />
              <el-table-column prop="level" label="等级" width="100">
                <template #default="{ row }">
                  <span class="badge" :class="`badge--${row.level}`">{{ row.levelLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default>
                  <GlassButton variant="primary" size="small">处理</GlassButton>
                </template>
              </el-table-column>
            </GlassTable>
          </GlassCard>
        </div>
      </div>
    </GlassPage>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 菜单数据
const menuItems = [
  { key: 'realtime', icon: '▣', label: '实时数据' },
  { key: 'alarm', icon: '⚠', label: '报警管理' },
  { key: 'defect', icon: '⚙', label: '缺陷处理' }
]
const adminItems = [
  { key: 'account', icon: '☷', label: '账号管理' },
  { key: 'config', icon: '⚒', label: '系统配置' },
  { key: 'log', icon: '☰', label: '操作日志' }
]

const activeMenu = ref<string>('realtime')

// KPI
const kpis = [
  { label: '总产量 (今日)', value: '42,318', color: 'var(--text-primary)', trend: 12.3 },
  { label: '合格率', value: '98.4%', color: 'var(--accent)', trend: 0.6 },
  { label: '活跃报警', value: '7', color: 'var(--warning)', trend: -3 },
  { label: '缺陷率', value: '1.6%', color: 'var(--danger)', trend: -0.4 }
]

// 报警行
const alarmRows = ref([
  { time: '14:22', line: '产线 #2', device: 'A12', type: '串口断开', level: 'danger', levelLabel: '严重' },
  { time: '14:18', line: '产线 #5', device: 'B08', type: '检测超时', level: 'warning', levelLabel: '警告' },
  { time: '14:05', line: '产线 #8', device: 'C03', type: '漏检告警', level: 'danger', levelLabel: '严重' },
  { time: '13:51', line: '产线 #3', device: 'A07', type: '速度异常', level: 'warning', levelLabel: '警告' },
  { time: '13:40', line: '产线 #1', device: 'A01', type: '网络抖动', level: 'info', levelLabel: '提示' }
])

function onReset() {
  activeMenu.value = 'realtime'
}
function onSubmit() {
  // eslint-disable-next-line no-console
  console.log('submit clicked')
}
function onDanger() {
  // eslint-disable-next-line no-console
  console.log('logout clicked')
}
function onViewMore() {
  // eslint-disable-next-line no-console
  console.log('view more clicked')
}
</script>

<style lang="scss" scoped>
.demo-shell {
  min-height: 100vh;
  padding: var(--space-5);
}

.demo-actions {
  display: flex;
  gap: var(--space-3);
}

.demo-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: var(--space-4);

  &__sidebar {
    align-self: start;
    position: sticky;
    top: var(--space-5);
  }

  &__menu-label {
    padding: 0 12px 8px;
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-semibold);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  &__menu {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__main {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
    min-width: 0;
  }

  &__kpis {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-4);
  }

  &__table-card {
    display: flex;
    flex-direction: column;
  }

  &__table-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-4);
  }

  &__table-title {
    margin: 0;
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
  }
}

.kpi {
  &__label {
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    margin-bottom: var(--space-2);
  }
  &__value {
    font-size: var(--font-size-2xl);
    font-weight: var(--font-weight-bold);
    line-height: 1.2;
  }
  &__trend {
    font-size: var(--font-size-xs);
    margin-top: var(--space-2);
    &--up { color: var(--success); }
    &--down { color: var(--danger); }
  }
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-pill);
  border: 1px solid transparent;

  &--danger {
    background: rgba(255, 90, 95, 0.15);
    border-color: rgba(255, 90, 95, 0.35);
    color: var(--danger);
  }
  &--warning {
    background: rgba(255, 183, 77, 0.15);
    border-color: rgba(255, 183, 77, 0.35);
    color: var(--warning);
  }
  &--info {
    background: rgba(92, 225, 255, 0.15);
    border-color: rgba(92, 225, 255, 0.35);
    color: var(--accent);
  }
}

@media (max-width: 960px) {
  .demo-grid {
    grid-template-columns: 1fr;
    &__kpis { grid-template-columns: repeat(2, 1fr); }
  }
}
</style>
