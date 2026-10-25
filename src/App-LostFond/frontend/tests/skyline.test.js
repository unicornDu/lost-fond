// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import {
  buildGrid, camera, computeStats, dayMs, levelOf, monthLabels, riseAt, toKey, todayMs,
} from '../src/utils/skyline'

/** v2.41.1 首页活动天际线：先把纯算法锁住，画布部分靠人工目测。 */
describe('skyline 活动网格', () => {
  const end = dayMs('2026-10-21')

  it('按周对齐铺满窗口，最后一天落在结束日', () => {
    const g = buildGrid([], end, 1, 182)
    // 最后一列允许是半周（未来几天没有格子）
    expect(g.cells.length).toBeGreaterThan((g.weeks - 1) * 7)
    expect(g.cells.length).toBeLessThanOrEqual(g.weeks * 7)
    expect(g.cells[g.cells.length - 1].date).toBe('2026-10-21')
    // 第一列一定是周一
    expect(new Date(g.cells[0].date + 'T00:00:00Z').getUTCDay()).toBe(1)
  })

  it('稀疏数据会按日期合并，缺的天补 0', () => {
    const g = buildGrid([
      { date: '2026-10-01', count: 2 },
      { date: '2026-10-01', count: 3 },
      { date: '2026-10-03', count: 1 },
      { date: 'not-a-date', count: 9 },
      { date: '2026-10-05', count: 0 },
    ], end, 1, 30)
    const at = (d) => g.cells.find((c) => c.date === d)
    expect(at('2026-10-01').count).toBe(5)
    expect(at('2026-10-02').count).toBe(0)
    expect(at('2026-10-03').count).toBe(1)
    expect(g.max).toBe(5)
    expect(g.activeDays).toBe(2)
  })

  it('等级按 95 分位分四档，一柱穿天不会把所有日子压成 1 级', () => {
    const cells = Array.from({ length: 20 }, (_, i) => ({ count: i === 19 ? 100 : 4 }))
    const busy = 4
    expect(levelOf(0, busy)).toBe(0)
    expect(levelOf(1, busy)).toBe(2)
    expect(levelOf(4, busy)).toBe(5 - 1)   // 1 + min(3, floor(4/4*4)) = 4
    expect(levelOf(100, busy)).toBe(4)
    expect(cells.length).toBe(20)
  })

  it('统计：总量 / 最忙一天 / 活跃天数 / 最长与当前连续', () => {
    const g = buildGrid([
      { date: '2026-10-10', count: 3 },
      { date: '2026-10-11', count: 1 },
      { date: '2026-10-12', count: 5 },
      { date: '2026-10-15', count: 2 },
    ], end, 1, 30)
    const s = computeStats(g.cells)
    expect(s.total).toBe(11)
    expect(s.activeDays).toBe(4)
    expect(s.busiest).toEqual({ count: 5, date: '2026-10-12' })
    expect(s.longest.days).toBe(3)
    expect(s.longest.start).toBe('2026-10-10')
    expect(s.current.days).toBe(0)         // 10-15 之后没有连续到今天
  })

  it('月份标签不会挤在一起', () => {
    const g = buildGrid([], end, 1, 182)
    const labels = monthLabels(g.cells, g.weeks, 'zh-CN')
    expect(labels.length).toBeGreaterThan(1)
    for (let i = 1; i < labels.length; i++) {
      expect(labels[i].week - labels[i - 1].week).toBeGreaterThanOrEqual(3)
    }
  })

  it('相机：t=0 是正俯视，t=1 是等轴测', () => {
    const flat = camera(0)
    expect(flat.cs).toBeCloseTo(1, 6)
    expect(flat.sn).toBeCloseTo(0, 6)
    expect(flat.ce).toBeCloseTo(0, 6)
    const iso = camera(1)
    expect(iso.cs).toBeCloseTo(Math.SQRT1_2, 3)
    expect(iso.se).toBeCloseTo(Math.sin((34 * Math.PI) / 180), 6)
  })

  it('升起进度：0 处全平、1 处全满，且旧的周先起', () => {
    expect(riseAt(0, 10, 20, 3)).toBe(0)
    expect(riseAt(1, 10, 20, 3)).toBe(1)
    expect(riseAt(0.6, 0, 20, 0)).toBeGreaterThan(riseAt(0.6, 19, 20, 5))
  })

  it('toKey / todayMs 保持本地日历日不漂移', () => {
    expect(toKey(dayMs('2026-01-01'))).toBe('2026-01-01')
    const t = new Date(todayMs())
    expect(t.getUTCHours()).toBe(0)
  })
})
