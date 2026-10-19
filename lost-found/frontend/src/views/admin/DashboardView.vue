<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
// 修复 F-06：echarts 按需引入（仅折线+环形图及所用组件），全量引入曾打出 1.1MB 单 chunk
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, PieChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { theme } from '../../composables/useTheme'

/** 管理后台数据看板（审查 2.9）：ECharts 近30天发布趋势 + 分类占比 + 统计卡。 */
const stats = ref({ total: 0, reunited: 0, successRate: 0, userCount: 0 })
const loading = ref(false)
let trendChart = null
let pieChart = null
const trendEl = ref(null)
const pieEl = ref(null)
let lastDaily = []
let lastRatio = []

/** 图表配色跟随主题令牌，避免暗色下坐标轴文字看不清 */
function tokens() {
  const cs = getComputedStyle(document.documentElement)
  const get = (k) => cs.getPropertyValue(k).trim()
  return { ink: get('--ink'), soft: get('--ink-soft'), line: get('--line'), paper: get('--paper') }
}

function renderTrend(daily) {
  if (!trendEl.value) return
  const c = tokens()
  trendChart = trendChart || echarts.init(trendEl.value)
  trendChart.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: c.soft, fontFamily: 'inherit' },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: {
      type: 'category', data: daily.map((d) => String(d.date)),
      axisLine: { lineStyle: { color: c.line } },
      axisTick: { show: false },
      axisLabel: { color: c.soft },
    },
    yAxis: {
      type: 'value', minInterval: 1,
      splitLine: { lineStyle: { color: c.line } },
      axisLabel: { color: c.soft },
    },
    series: [{
      name: '发布量', type: 'line', smooth: true,
      data: daily.map((d) => Number(d.value)),
      areaStyle: { opacity: 0.14, color: c.ink },
      lineStyle: { color: c.ink, width: 2 },
      itemStyle: { color: c.ink },
    }],
  }, true)
}

function renderPie(ratio) {
  if (!pieEl.value) return
  const c = tokens()
  pieChart = pieChart || echarts.init(pieEl.value)
  pieChart.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: c.soft, fontFamily: 'inherit' },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: c.soft } },
    series: [{
      type: 'pie', radius: ['40%', '68%'],
      data: ratio.map((r) => ({ name: r.name, value: Number(r.value) })),
      label: { formatter: '{b}: {c}', color: c.soft },
      itemStyle: { borderColor: c.paper, borderWidth: 2 },
      color: ['#4A6CF7', '#2FA46A', '#E8823A', '#8B5CF6', '#E5484D', '#9A9A92'],
    }],
  }, true)
}

async function load() {
  loading.value = true
  try {
    const d = await api.adminStats()
    stats.value = d
    lastDaily = d.dailyTrend || []
    lastRatio = d.categoryRatio || []
    await nextTick()
    renderTrend(lastDaily)
    renderPie(lastRatio)
  } catch (e) {
    ElMessage.error(e.message || '看板加载失败')
  } finally {
    loading.value = false
  }
}

function onResize() { trendChart?.resize(); pieChart?.resize() }

/* 切主题后重建图表（ECharts 的颜色是 canvas 内的像素，不会跟 CSS 变量走） */
watch(theme, async () => {
  await nextTick()
  trendChart?.dispose(); trendChart = null
  pieChart?.dispose(); pieChart = null
  renderTrend(lastDaily)
  renderPie(lastRatio)
})
onMounted(() => { load(); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose(); pieChart?.dispose()
})
</script>

<template>
  <div v-loading="loading">
    <div class="cards">
      <div class="card paper-card"><b>{{ stats.total }}</b><span>启事总数</span></div>
      <div class="card paper-card"><b>{{ stats.reunited }}</b><span>已找回/已认领</span></div>
      <div class="card paper-card"><b>{{ stats.successRate }}%</b><span>认领成功率</span></div>
      <div class="card paper-card"><b>{{ stats.userCount }}</b><span>注册用户</span></div>
    </div>
    <div class="charts">
      <div class="chart paper-card">
        <h3>近 30 天发布趋势</h3>
        <div ref="trendEl" class="chart-body"></div>
      </div>
      <div class="chart paper-card">
        <h3>分类占比</h3>
        <div ref="pieEl" class="chart-body"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.card { padding: 18px; display: flex; flex-direction: column; gap: 4px; align-items: center; }
.card b { font-family: var(--font-display); font-size: 26px; color: var(--orange-deep); }
.card span { color: var(--ink-faint); font-size: 12.5px; }
.charts { display: grid; grid-template-columns: 1.4fr 1fr; gap: 14px; }
.chart { padding: 16px; }
.chart h3 { font-size: 15px; margin-bottom: 8px; }
.chart-body { width: 100%; height: 300px; }
@media (max-width: 900px) { .cards { grid-template-columns: repeat(2,1fr); } .charts { grid-template-columns: 1fr; } }
</style>
