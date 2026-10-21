<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { locale, t } from '../i18n'
import {
  DAY_MS, barHeight, buildGrid, camera, computeStats, depthOf, easeInOutCubic, lerp,
  monthLabels, projectPoint, riseAt, smoothstep, todayMs,
} from '../utils/skyline'

/**
 * 活动天际线（v2.41.1）—— 首页「信息总览」，移植自 `设计prompt/前端/首页信息总览.md`。
 *
 * 一个场景而不是两张图：每个"天"是网格里的一个方块，2D 是俯视的热力图，
 * 3D 是同一网格从斜角看过去的城市天际线；切换时相机与柱高一起做一次波浪式过渡，
 * 从最旧的一周扫到最新的一周，所以是"折叠/展开"，不是换图。
 *
 * 与 prompt 的差异（都是为了站点一致性与真数据）：
 * - 配色按用户参考图取「浅蓝 → 中蓝 → 亮蓝」四阶蓝（v2.41.7），不是 GitHub 绿；
 * - 数据来自 `/api/stats/daily`（按发生日统计的真启事量），没有数据时如实显示空态；
 * - 去掉了拖拽环绕，保留悬停/键盘/图例联动与 2D-3D 切换。
 */
const props = defineProps({
  data: { type: Array, default: () => [] },
  days: { type: Number, default: 182 },
})

const stage = ref(null)
const canvasEl = ref(null)
const tipEl = ref(null)
const view = ref('3d')
const active = ref(-1)
const legendLevel = ref(-1)
const width = ref(0)
const tipX = ref(0)
const tipY = ref(0)

const model = computed(() => {
  const grid = buildGrid(props.data, todayMs(), 1, props.days)
  const months = monthLabels(grid.cells, grid.weeks, locale.value)
  return { ...grid, stats: computeStats(grid.cells), months }
})

const empty = computed(() => model.value.stats.total === 0)
const stats = computed(() => model.value.stats)

/* ---------- 主题色（画布拿不到 CSS 变量，用探针读一次） ---------- */
let COL = []
let EDGE = []
let MUTED = [120, 120, 112]
let INK = [20, 20, 18]
const HEAT_RAMP = [
  [235, 238, 240], [220, 232, 247], [196, 215, 237], [155, 195, 240], [43, 126, 223],
]
function readColor(varName, fallback) {
  try {
    const el = document.createElement('span')
    el.style.cssText = `position:absolute;left:-9999px;color:var(${varName})`
    document.body.appendChild(el)
    const c = getComputedStyle(el).color
    el.remove()
    const m = /(\d+\.?\d*)[,\s]+(\d+\.?\d*)[,\s]+(\d+\.?\d*)/.exec(c)
    return m ? [+m[1], +m[2], +m[3]] : fallback
  } catch (e) { return fallback }
}
const mix = (a, b, k) => [a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k]
const rgb = (c) => `rgb(${Math.round(c[0])},${Math.round(c[1])},${Math.round(c[2])})`
function readTheme() {
  INK = readColor('--ink', [20, 20, 18])
  const bg = readColor('--cream', [250, 250, 248])
  COL = HEAT_RAMP.map((c, i) => readColor(`--heat-${i}`, c))
  EDGE = COL.map((c) => c.map((v) => v * 0.9))
  MUTED = mix(bg, INK, 0.52)
  eng.colDirty = true
}

/* ---------- 引擎（非响应式，逐帧读） ---------- */
const eng = {
  t: 0, target: 1, raf: 0, last: 0,
  W: 0, H2: 0, H3: 0, Hc: 170, dpr: 1,
  n: 0, weeks: 0, wx: [], dy: [], lv: [], hgt: [], zs: [],
  polys: null, hover: [], dim: [],
  gutter: 34, colDirty: true, order: [],
}

function layout() {
  const el = stage.value
  if (!el) return
  const W = Math.round(el.clientWidth)
  if (!W) return
  const weeks = Math.max(1, model.value.weeks)
  eng.W = W
  eng.weeks = weeks
  width.value = W
  eng.gutter = W < 560 ? 0 : 34
  eng.H2 = Math.round(26 + (7 / weeks) * (W - eng.gutter) + 16)
  eng.H3 = Math.round(Math.min(Math.max(W * 0.44, 200), 330))
  eng.dpr = Math.min(2, window.devicePixelRatio || 1)
  const canvas = canvasEl.value
  canvas.width = Math.round(W * eng.dpr)
  canvas.height = Math.round(Math.max(eng.H2, eng.H3) * eng.dpr)
  canvas.style.width = W + 'px'
  canvas.style.height = Math.max(eng.H2, eng.H3) + 'px'
  load()
  draw()
}

function load() {
  const m = model.value
  eng.n = m.cells.length
  eng.wx = m.cells.map((c) => c.week)
  eng.dy = m.cells.map((c) => c.day)
  eng.lv = m.cells.map((c) => c.level)
  eng.hgt = m.cells.map((c) => barHeight(c.count, m.max))
  eng.zs = new Array(eng.n).fill(0)
  eng.hover = new Array(eng.n).fill(0)
  eng.dim = new Array(eng.n).fill(0)
  eng.polys = new Float32Array(eng.n * 8)
  eng.order = Array.from({ length: eng.n }, (_, i) => i)
}

/** 投影包围盒（含柱顶与底面），用于把整幕缩放到画布内。 */
function extent(cam, zs, fill) {
  let minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity
  const add = (x, y, z) => {
    const p = projectPoint(cam, x, y, z)
    if (p[0] < minx) minx = p[0]
    if (p[0] > maxx) maxx = p[0]
    if (p[1] < miny) miny = p[1]
    if (p[1] > maxy) maxy = p[1]
  }
  const w = 0.82
  for (let i = 0; i < eng.n; i++) {
    const x = eng.wx[i] + 0.09
    const y = eng.dy[i] + 0.09
    const z = fill ? zs[i] : 0
    add(x, y, z); add(x + w, y, z); add(x, y + w, z); add(x + w, y + w, z)
    add(x, y + w, 0); add(x + w, y, 0)
  }
  add(0, 7.6, 0); add(eng.weeks, 7.6, 0)
  return { minx, maxx, miny, maxy }
}

function quadPath(ctx, p, o, r) {
  if (r < 0.4) {
    ctx.moveTo(p[o], p[o + 1])
    ctx.lineTo(p[o + 2], p[o + 3])
    ctx.lineTo(p[o + 4], p[o + 5])
    ctx.lineTo(p[o + 6], p[o + 7])
    ctx.closePath()
    return
  }
  ctx.moveTo((p[o + 6] + p[o]) / 2, (p[o + 7] + p[o + 1]) / 2)
  for (let k = 0; k < 4; k++) {
    const b = (k + 1) % 4
    ctx.arcTo(p[o + k * 2], p[o + k * 2 + 1], p[o + b * 2], p[o + b * 2 + 1], r)
  }
  ctx.closePath()
}

function draw() {
  const canvas = canvasEl.value
  if (!canvas || !eng.W || !eng.n) return
  const ctx = canvas.getContext('2d')
  const e = easeInOutCubic(eng.t)
  const cam = camera(e)
  const Hc = lerp(eng.H2, eng.H3, e)
  eng.Hc = Hc
  if (stage.value) stage.value.style.height = Hc.toFixed(1) + 'px'

  for (let i = 0; i < eng.n; i++) eng.zs[i] = riseAt(eng.t, eng.wx[i], eng.weeks, eng.dy[i]) * eng.hgt[i]

  const b = extent(cam, eng.zs, false)
  const pad = lerp(6, 18, e)
  const left = pad + eng.gutter * (1 - e)
  const top = pad + 18 * (1 - e)
  const aw = eng.W - left - pad
  const ah = Hc - top - pad
  const bw = Math.max(1e-6, b.maxx - b.minx)
  const bh = Math.max(1e-6, b.maxy - b.miny)
  const s = Math.min(aw / bw, ah / bh)
  const ox = left + (aw - bw * s) / 2 - b.minx * s
  const oy = top + (ah - bh * s) / 2 - b.miny * s
  const { cs, sn, se, ce } = cam
  const px = (x, y) => ox + (x * cs - y * sn) * s
  const py = (x, y, z) => oy + ((x * sn + y * cs) * se - z * ce) * s

  ctx.setTransform(eng.dpr, 0, 0, eng.dpr, 0, 0)
  ctx.clearRect(0, 0, eng.W, Hc)

  const w = 0.82
  const radius = lerp(0.2, 0.03, e) * s
  const border = 1 - e
  const leg = legendLevel.value

  eng.order.sort((a, c) => depthOf(cam, eng.wx[a] + 0.5, eng.dy[a] + 0.5) - depthOf(cam, eng.wx[c] + 0.5, eng.dy[c] + 0.5))

  for (let k = 0; k < eng.n; k++) {
    const i = eng.order[k]
    const x0 = eng.wx[i] + 0.09
    const y0 = eng.dy[i] + 0.09
    const z = eng.zs[i]
    const o = i * 8
    eng.polys[o] = px(x0, y0); eng.polys[o + 1] = py(x0, y0, z)
    eng.polys[o + 2] = px(x0 + w, y0); eng.polys[o + 3] = py(x0 + w, y0, z)
    eng.polys[o + 4] = px(x0 + w, y0 + w); eng.polys[o + 5] = py(x0 + w, y0 + w, z)
    eng.polys[o + 6] = px(x0, y0 + w); eng.polys[o + 7] = py(x0, y0 + w, z)

    const tall = z * ce * s
    const base = COL[eng.lv[i]]
    let col = base
    if (leg >= 0 && eng.lv[i] !== leg) col = mix(base, COL[0], 0.72)
    if (i === active.value) col = mix(base, INK, 0.16)

    // 侧面（3D 才有）
    if (tall > 0.3) {
      const sx1 = px(x0, y0 + w), sy1 = py(x0, y0 + w, z)
      const sx2 = px(x0 + w, y0 + w), sy2 = py(x0 + w, y0 + w, z)
      ctx.beginPath()
      ctx.moveTo(sx1, sy1); ctx.lineTo(sx2, sy2)
      ctx.lineTo(px(x0 + w, y0 + w), py(x0 + w, y0 + w, 0)); ctx.lineTo(px(x0, y0 + w), py(x0, y0 + w, 0))
      ctx.closePath()
      ctx.fillStyle = rgb([col[0] * 0.86, col[1] * 0.86, col[2] * 0.86])
      ctx.fill()
    }
    if (tall > 0.3 && w * sn * s > 0.3) {
      ctx.beginPath()
      ctx.moveTo(px(x0 + w, y0), py(x0 + w, y0, 0))
      ctx.lineTo(px(x0 + w, y0 + w), py(x0 + w, y0 + w, 0))
      ctx.lineTo(px(x0 + w, y0 + w), py(x0 + w, y0 + w, z))
      ctx.lineTo(px(x0 + w, y0), py(x0 + w, y0, z))
      ctx.closePath()
      ctx.fillStyle = rgb([col[0] * 0.7, col[1] * 0.7, col[2] * 0.7])
      ctx.fill()
    }

    ctx.beginPath()
    quadPath(ctx, eng.polys, o, radius)
    ctx.fillStyle = rgb(col)
    ctx.fill()
    if (border > 0.02 && eng.lv[i] > 0) {
      const ec = EDGE[eng.lv[i]]
      ctx.strokeStyle = `rgba(${Math.round(ec[0])},${Math.round(ec[1])},${Math.round(ec[2])},${border.toFixed(3)})`
      ctx.lineWidth = 1
      ctx.stroke()
    }
    if (i === active.value) {
      ctx.strokeStyle = `rgba(${INK[0]},${INK[1]},${INK[2]},.85)`
      ctx.lineWidth = 1.4
      ctx.stroke()
    }
  }

  // 标签：2D 在顶部标月份、左侧标星期；3D 只在最前一排标月份
  ctx.font = `400 10px ${getComputedStyle(document.body).fontFamily || 'sans-serif'}`
  const a2 = 1 - smoothstep(0, 0.42, e)
  const a3 = smoothstep(0.66, 1, e)
  const muted = `rgba(${Math.round(MUTED[0])},${Math.round(MUTED[1])},${Math.round(MUTED[2])},`
  if (a2 > 0.01) {
    ctx.fillStyle = muted + a2.toFixed(3) + ')'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'bottom'
    let edge = -Infinity
    for (const m of model.value.months) {
      const x = px(m.week + 0.09, -0.3)
      const tw = ctx.measureText(m.label).width
      if (x < edge || x + tw > eng.W) continue
      ctx.fillText(m.label, x, py(m.week + 0.09, -0.3, 0) - 3)
      edge = x + tw + 8
    }
    if (eng.gutter > 0) {
      ctx.textAlign = 'right'
      ctx.textBaseline = 'middle'
      const rows = [[1, t('activity.wd.mon')], [3, t('activity.wd.wed')], [5, t('activity.wd.fri')]]
      for (const [day, label] of rows) {
        ctx.fillText(label, px(0, day + 0.5) - 6, py(0, day + 0.5, 0))
      }
    }
  }
  if (a3 > 0.01) {
    ctx.fillStyle = muted + a3.toFixed(3) + ')'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'top'
    let edge = -Infinity
    for (const m of model.value.months) {
      const x = px(m.week + 0.5, 7.34)
      const tw = ctx.measureText(m.label).width
      if (x < edge || x + tw > eng.W) continue
      ctx.fillText(m.label, x, py(m.week + 0.5, 7.34, 0) + 2)
      edge = x + tw + 12
    }
  }

  // 提示气泡跟随当前格子
  const idx = active.value
  if (idx >= 0 && idx < eng.n && tipEl.value) {
    const o = idx * 8
    const cx = (eng.polys[o] + eng.polys[o + 2] + eng.polys[o + 4] + eng.polys[o + 6]) / 4
    const cy = Math.min(eng.polys[o + 1], eng.polys[o + 3], eng.polys[o + 5], eng.polys[o + 7])
    const tw = tipEl.value.offsetWidth || 120
    tipX.value = Math.min(eng.W - tw / 2 - 4, Math.max(tw / 2 + 4, cx))
    tipY.value = Math.max(cy - 8, 12)
  }
}

/* ---------- 交互 ---------- */
function hitTest(x, y) {
  const p = eng.polys
  for (let i = 0; i < eng.n; i++) {
    const o = i * 8
    let inside = true
    let sign = 0
    for (let k = 0; k < 4; k++) {
      const ax = p[o + k * 2], ay = p[o + k * 2 + 1]
      const bx = p[o + ((k + 1) % 4) * 2], by = p[o + ((k + 1) % 4) * 2 + 1]
      const cross = (bx - ax) * (y - ay) - (by - ay) * (x - ax)
      if (Math.abs(cross) < 1e-9) continue
      const s = cross > 0 ? 1 : -1
      if (sign === 0) sign = s
      else if (s !== sign) { inside = false; break }
    }
    if (inside) return i
  }
  return -1
}

function onMove(e) {
  const r = canvasEl.value.getBoundingClientRect()
  const i = hitTest(e.clientX - r.left, e.clientY - r.top)
  if (i !== active.value) active.value = i
}

function onLeave() { active.value = -1 }

function onKey(e) {
  const keys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Escape']
  if (!keys.includes(e.key) || !eng.n) return
  e.preventDefault()
  if (e.key === 'Escape') { active.value = -1; return }
  let i = active.value >= 0 ? active.value : eng.n - 1
  if (e.key === 'ArrowLeft') i -= 7
  if (e.key === 'ArrowRight') i += 7
  if (e.key === 'ArrowUp') i -= 1
  if (e.key === 'ArrowDown') i += 1
  active.value = Math.max(0, Math.min(eng.n - 1, i))
}

/* ---------- 循环 ---------- */
function tick(now) {
  eng.raf = 0
  const dt = Math.min(0.05, Math.max(0, (now - eng.last) / 1000))
  eng.last = now
  if (eng.t !== eng.target) {
    const step = dt / 1.3
    eng.t = eng.target > eng.t ? Math.min(eng.target, eng.t + step) : Math.max(eng.target, eng.t - step)
    draw()
    if (eng.t !== eng.target) eng.raf = requestAnimationFrame(tick)
  }
}
function kick() {
  if (eng.raf) return
  eng.last = performance.now()
  eng.raf = requestAnimationFrame(tick)
}

watch(view, (v) => {
  eng.target = v === '3d' ? 1 : 0
  kick()
})
watch([active, legendLevel], () => draw())
watch(model, () => { layout() })

/* 首次进入视野时把 2D 展开成 3D */
let io = null
let ro = null
let mo = null
let onTheme = null

onMounted(() => {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  readTheme()
  eng.t = reduce ? 1 : 0
  eng.target = 1
  layout()
  if (reduce) draw()

  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(() => layout())
    ro.observe(stage.value)
  }
  io = new IntersectionObserver((entries) => {
    if (entries.some((en) => en.isIntersecting)) {
      if (!reduce) kick()
      io?.disconnect()
    }
  }, { threshold: 0.3 })
  io.observe(stage.value)

  onTheme = () => { readTheme(); draw() }
  window.addEventListener('shiguang-theme-changed', onTheme)
  mo = new MutationObserver(onTheme)
  mo.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'class'] })
})

onBeforeUnmount(() => {
  if (eng.raf) cancelAnimationFrame(eng.raf)
  io?.disconnect()
  ro?.disconnect()
  mo?.disconnect()
  if (onTheme) window.removeEventListener('shiguang-theme-changed', onTheme)
})

const tipText = computed(() => {
  const c = model.value.cells[active.value]
  if (!c) return ''
  return c.count ? t('activity.tip', { n: c.count, date: c.date }) : t('activity.tipZero', { date: c.date })
})
</script>

<template>
  <section class="sky" :aria-label="$t('activity.aria')">
    <header class="sky-head">
      <div class="sky-title">
        <p class="eyebrow">{{ $t('activity.eyebrow') }}</p>
        <h2>
          <b>{{ stats.total }}</b>
          <span>{{ $t('activity.title', { n: stats.total }) }}</span>
        </h2>
        <p class="sky-sub">{{ $t('activity.sub') }}</p>
      </div>
      <div class="sky-switch" role="group" :aria-label="$t('activity.view')">
        <span class="pill" :class="{ on3d: view === '3d' }" aria-hidden="true"></span>
        <button type="button" :class="{ on: view === '2d' }" :aria-pressed="view === '2d'" @click="view = '2d'">
          <svg viewBox="0 0 16 16" aria-hidden="true"><rect x="1.5" y="1.5" width="5.5" height="5.5" rx="1" /><rect x="9" y="1.5" width="5.5" height="5.5" rx="1" /><rect x="1.5" y="9" width="5.5" height="5.5" rx="1" /><rect x="9" y="9" width="5.5" height="5.5" rx="1" /></svg>
          <span>{{ $t('activity.flat') }}</span>
        </button>
        <button type="button" :class="{ on: view === '3d' }" :aria-pressed="view === '3d'" @click="view = '3d'">
          <svg viewBox="0 0 16 16" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"><path d="M8 1.2 14.2 4.6v6.8L8 14.8 1.8 11.4V4.6Z" /><path d="M1.8 4.6 8 8l6.2-3.4M8 8v6.8" /></svg>
          <span>{{ $t('activity.skyline') }}</span>
        </button>
      </div>
    </header>

    <div ref="stage" class="sky-stage">
      <canvas
        ref="canvasEl" tabindex="0" role="img"
        :aria-label="$t('activity.canvas', { n: stats.total })"
        @pointermove="onMove" @pointerleave="onLeave" @blur="active = -1" @keydown="onKey"
      ></canvas>
      <div
        v-if="active >= 0" ref="tipEl" class="sky-tip" role="tooltip"
        :style="{ left: tipX + 'px', top: tipY + 'px' }"
      >{{ tipText }}</div>
      <p v-if="empty" class="sky-empty">{{ $t('activity.empty') }}</p>
    </div>

    <div class="sky-foot">
      <div class="sky-stats">
        <div><span>{{ $t('activity.stat.active') }}</span><p class="val"><b>{{ stats.activeDays }}</b><em>{{ $t('activity.unit.day') }}</em></p></div>
        <div><span>{{ $t('activity.stat.busiest') }}</span><p class="val"><b>{{ stats.busiest.count }}</b><em>{{ $t('activity.unit.post') }}</em></p><small>{{ stats.busiest.date || '—' }}</small></div>
        <div><span>{{ $t('activity.stat.longest') }}</span><p class="val"><b>{{ stats.longest.days }}</b><em>{{ $t('activity.unit.day') }}</em></p></div>
        <div><span>{{ $t('activity.stat.current') }}</span><p class="val"><b>{{ stats.current.days }}</b><em>{{ $t('activity.unit.day') }}</em></p></div>
      </div>
      <div class="sky-legend" @mouseleave="legendLevel = -1">
        <span>{{ $t('activity.less') }}</span>
        <button
          v-for="(c, i) in 5" :key="i" type="button" class="sw"
          :class="['l' + i, { on: legendLevel === i }]"
          :aria-label="$t('activity.level', { n: i })"
          @mouseenter="legendLevel = i" @mouseleave="legendLevel = -1" @click="legendLevel = legendLevel === i ? -1 : i"
        ></button>
        <span>{{ $t('activity.more') }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.sky {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 18px 20px 14px;
  margin-bottom: 26px;
}
.sky-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.eyebrow { font-size: 10.5px; font-weight: 700; letter-spacing: .18em; color: var(--ink-faint); margin: 0 0 8px; }
.sky-title h2 { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: 0; display: flex; align-items: baseline; gap: 8px; }
.sky-title h2 b { font-family: var(--font-display); font-size: 26px; font-weight: 800; letter-spacing: -.02em; }
.sky-title h2 span { color: var(--ink-soft); font-size: 13px; font-weight: 600; }
.sky-sub { margin: 5px 0 0; font-size: 11.5px; color: var(--ink-faint); }

.sky-switch {
  position: relative; display: inline-flex; padding: 3px;
  border: 1px solid var(--line-2); border-radius: 999px; flex: none;
}
.sky-switch .pill {
  position: absolute; top: 3px; bottom: 3px; left: 3px;
  width: calc(50% - 3px); border-radius: 999px;
  background: var(--ink);
  transition: transform .45s cubic-bezier(.65, 0, .35, 1);
}
.sky-switch .pill.on3d { transform: translateX(100%); }
.sky-switch button {
  position: relative; z-index: 1;
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 0; border-radius: 999px; background: transparent;
  color: var(--ink-faint); font-family: inherit; font-size: 11.5px; font-weight: 700;
  cursor: pointer; transition: color .35s ease;
}
.sky-switch button.on { color: var(--paper); }
.sky-switch button svg { width: 13px; height: 13px; }
.sky-switch button svg rect { fill: currentColor; }

.sky-stage { position: relative; margin-top: 10px; }
.sky-stage canvas { display: block; width: 100%; height: 170px; outline: none; touch-action: pan-y; }
.sky-stage canvas:focus-visible { outline: 2px solid var(--ink); outline-offset: 3px; border-radius: 6px; }
.sky-tip {
  position: absolute; z-index: 3; transform: translate(-50%, -100%);
  padding: 6px 10px; border-radius: 8px;
  background: var(--ink); color: var(--paper);
  font-size: 11.5px; font-weight: 600; white-space: nowrap;
  box-shadow: 0 8px 18px rgba(20, 20, 18, .18);
  pointer-events: none;
}
.sky-empty {
  position: absolute; inset: 0; display: grid; place-items: center;
  margin: 0; font-size: 12.5px; color: var(--ink-faint);
}

.sky-foot {
  display: flex; align-items: center; justify-content: space-between;
  gap: 14px; flex-wrap: wrap; margin-top: 10px;
  padding-top: 12px; border-top: 1px solid var(--line);
}
.sky-stats { display: flex; gap: 26px; flex-wrap: wrap; }
.sky-stats div { display: flex; flex-direction: column; gap: 2px; }
.sky-stats span { font-size: 10.5px; color: var(--ink-faint); font-weight: 600; }
.sky-stats .val { display: flex; align-items: baseline; gap: 3px; margin: 0; }
.sky-stats b { font-size: 15px; font-weight: 800; font-variant-numeric: tabular-nums; }
.sky-stats em { font-style: normal; font-size: 10px; color: var(--ink-faint); }
.sky-stats small { font-size: 9.5px; color: var(--ink-faint); letter-spacing: .02em; }

.sky-legend { display: flex; align-items: center; gap: 5px; font-size: 10px; color: var(--ink-faint); }
.sky-legend .sw {
  width: 11px; height: 11px; border-radius: 3px; border: 0; padding: 0; cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(127, 127, 127, .12);
  transition: transform .18s ease;
}
.sky-legend .sw:hover, .sky-legend .sw.on { transform: scale(1.22); }
.sky-legend .sw.l0 { background: var(--heat-0); }
.sky-legend .sw.l1 { --h: var(--heat-1); }
.sky-legend .sw.l2 { --h: var(--heat-2); }
.sky-legend .sw.l3 { --h: var(--heat-3); }
.sky-legend .sw.l4 { --h: var(--heat-4); }
.sky-legend .sw.l1, .sky-legend .sw.l2, .sky-legend .sw.l3, .sky-legend .sw.l4 {
  background: var(--h);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--h) 90%, #000);
}

@media (max-width: 640px) {
  .sky { padding: 16px 14px 12px; }
  .sky-stats { gap: 16px; }
  .sky-legend { display: none; }
}
</style>
