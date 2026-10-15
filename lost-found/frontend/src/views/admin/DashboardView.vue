<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

/** 管理后台数据看板（审查 2.9）：ECharts 近30天发布趋势 + 分类占比 + 统计卡。 */
const stats = ref({ total: 0, reunited: 0, successRate: 0, userCount: 0 })
const loading = ref(false)
let trendChart = null
let pieChart = null
const trendEl = ref(null)
const pieEl = ref(null)

function renderTrend(daily) {
  if (!trendEl.value) return
  trendChart = trendChart || echarts.init(trendEl.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: daily.map((d) => String(d.date)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '发布量', type: 'line', smooth: true,
      data: daily.map((d) => Number(d.value)),
      areaStyle: { opacity: 0.18 },
      itemStyle: { color: '#FF7D2E' },
    }],
  })
}

function renderPie(ratio) {
  if (!pieEl.value) return
  pieChart = pieChart || echarts.init(pieEl.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '68%'],
      data: ratio.map((r) => ({ name: r.name, value: Number(r.value) })),
      label: { formatter: '{b}: {c}' },
    }],
  })
}

async function load() {
  loading.value = true
  try {
    const d = await api.adminStats()
    stats.value = d
    await nextTick()
    renderTrend(d.dailyTrend || [])
    renderPie(d.categoryRatio || [])
  } catch (e) {
    ElMessage.error(e.message || '看板加载失败')
  } finally {
    loading.value = false
  }
}

function onResize() { trendChart?.resize(); pieChart?.resize() }
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
