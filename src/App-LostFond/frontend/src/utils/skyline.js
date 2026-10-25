/**
 * 活动天际线 · 纯算法（v2.41.1）
 *
 * 来源：`设计prompt/前端/首页信息总览.md` 的 Contribution Skyline（React + Canvas）。
 * 这里只保留与渲染无关的部分：日期网格、等级、统计、月份标签、相机与投影，
 * 方便单测覆盖；画布绘制在 components/ActivitySkyline.vue 里。
 *
 * 坐标约定：x = 周序号，y = 星期（0 = 周一），z = 柱子高度（网格单位）。
 */

export const DAY_MS = 86400000
export const WAVE = 0.42

export const clamp01 = (v) => (v > 0 ? (v < 1 ? v : 1) : 0)
export const lerp = (a, b, t) => a + (b - a) * t
export const easeInOutCubic = (x) => {
  const t = clamp01(x)
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}
export const easeOutCubic = (x) => 1 - Math.pow(1 - clamp01(x), 3)
export const smoothstep = (a, b, x) => {
  const t = clamp01((x - a) / (b - a))
  return t * t * (3 - 2 * t)
}

/** UTC 零点 → "YYYY-MM-DD"。 */
export const toKey = (ms) => new Date(ms).toISOString().slice(0, 10)

/** 任意日期 → 当天 UTC 零点（"YYYY-MM-DD" 按字面读，不发生时区漂移）。 */
export const dayMs = (v) => {
  if (typeof v === 'number') return Math.floor(v / DAY_MS) * DAY_MS
  if (typeof v === 'string') {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(v)
    if (m) return Date.UTC(+m[1], +m[2] - 1, +m[3])
    const d = new Date(v)
    return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())
  }
  return Date.UTC(v.getFullYear(), v.getMonth(), v.getDate())
}

/** 今天（本地日历日）的 UTC 零点。 */
export const todayMs = () => {
  const d = new Date()
  return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())
}

/** 0 = 空，1–4 按"最忙一天"的份额分四档（用 95 分位，避免一柱穿天把所有日子压成 1 级）。 */
export const levelOf = (count, busy) => {
  if (count <= 0) return 0
  if (busy <= 0) return 4
  return 1 + Math.min(3, Math.floor((count / busy) * 4))
}

/**
 * 由 [{date, count}] 铺成日历网格：列 = 周，行 = 星期。
 * @param {Array} data 稀疏数据（可以缺天）
 * @param {number} endMs 结束日（含）
 * @param {number} weekStart 0 = 周日在顶，1 = 周一在顶
 * @param {number} days 覆盖天数（默认半年 182）
 */
export const buildGrid = (data, endMs, weekStart = 1, days = 182) => {
  const counts = new Map()
  for (const d of data || []) {
    if (!d || typeof d.date !== 'string') continue
    const c = Number(d.count)
    if (!Number.isFinite(c) || c <= 0) continue
    const ms = dayMs(d.date)
    if (!Number.isFinite(ms)) continue   // 脏日期直接丢，别让 toISOString 抛异常
    const k = toKey(ms)
    counts.set(k, (counts.get(k) || 0) + c)
  }
  let start = endMs - (Math.max(1, days) - 1) * DAY_MS
  const shift = (new Date(start).getUTCDay() - weekStart + 7) % 7
  start -= shift * DAY_MS
  const cells = []
  for (let ms = start, i = 0; ms <= endMs; ms += DAY_MS, i++) {
    const date = toKey(ms)
    cells.push({ date, count: counts.get(date) || 0, level: 0, week: Math.floor(i / 7), day: i % 7 })
  }
  const nz = cells.map((c) => c.count).filter((c) => c > 0).sort((a, b) => a - b)
  const busy = nz.length ? nz[Math.floor(0.95 * (nz.length - 1))] : 0
  for (const c of cells) c.level = levelOf(c.count, busy)
  return {
    cells,
    weeks: cells.length ? cells[cells.length - 1].week + 1 : 0,
    max: nz.length ? nz[nz.length - 1] : 0,
    activeDays: nz.length,
  }
}

/** 总量 / 最忙一天 / 活跃天数 / 最长与当前连续。 */
export const computeStats = (cells) => {
  let total = 0
  let best = 0
  let bestDate = null
  let activeDays = 0
  let run = 0
  let runStart = null
  let longest = { days: 0, start: null, end: null }
  for (const c of cells) {
    total += c.count
    if (c.count > 0) {
      activeDays++
      if (run === 0) runStart = c.date
      run++
      if (run > longest.days) longest = { days: run, start: runStart, end: c.date }
    } else run = 0
    if (c.count > best) { best = c.count; bestDate = c.date }
  }
  // 当前连续：从最后一天往前数（今天还没结束，最后一天为 0 时从昨天算）
  let j = cells.length - 1
  if (j >= 0 && cells[j].count === 0) j--
  const endAt = j
  while (j >= 0 && cells[j].count > 0) j--
  const cur = endAt - j
  const current = cur > 0 ? { days: cur, start: cells[j + 1].date, end: cells[endAt].date } : { days: 0, start: null, end: null }
  return { total, busiest: { count: best, date: bestDate }, activeDays, longest, current }
}

/** 每列只要跨月就在列首标一次；头两个月挨太近时丢掉前一个。 */
export const monthLabels = (cells, weeks, locale = 'zh-CN') => {
  let fmt
  try { fmt = new Intl.DateTimeFormat(locale, { month: 'short', timeZone: 'UTC' }) }
  catch (e) { fmt = new Intl.DateTimeFormat('en-US', { month: 'short', timeZone: 'UTC' }) }
  const out = []
  let prev = -1
  for (let w = 0; w < weeks; w++) {
    const c = cells[w * 7]
    if (!c) break
    const m = +c.date.slice(5, 7)
    if (m !== prev) out.push({ week: w, label: fmt.format(dayMs(c.date)) })
    prev = m
  }
  if (out.length > 1 && out[1].week - out[0].week < 3) out.shift()
  return out
}

/** 柱子高度（网格单位）：空格是薄片，最忙的一天约 7.6 格高。 */
export const barHeight = (count, max, scale = 1) =>
  count > 0 && max > 0 ? 0.4 + Math.pow(count / max, 0.85) * 7.2 * scale : 0.2

export const YAW_3D = Math.PI / 4
export const ELEV_3D = (34 * Math.PI) / 180

/** e=0 正俯视（纯热力图），e=1 等轴测天际线。 */
export const camera = (e) => ({
  cs: Math.cos(lerp(0, YAW_3D, e)),
  sn: Math.sin(lerp(0, YAW_3D, e)),
  se: Math.sin(lerp(Math.PI / 2, ELEV_3D, e)),
  ce: Math.cos(lerp(Math.PI / 2, ELEV_3D, e)),
})

/** 世界 (x,y,z) → 屏幕（未缩放）。 */
export const projectPoint = (c, x, y, z) => [
  x * c.cs - y * c.sn,
  (x * c.sn + y * c.cs) * c.se - z * c.ce,
]

/** 画家算法的深度：越大越靠近观察者。 */
export const depthOf = (c, x, y) => x * c.sn + y * c.cs

/** 0→1 的升起进度：越靠后的周越晚起，形成从旧到新的波浪。 */
export const riseAt = (t, week, weeks, day) => {
  const d = (weeks > 1 ? week / (weeks - 1) : 0) * 0.36 + (day / 6) * 0.06
  return easeOutCubic((t - d) / (1 - WAVE))
}
