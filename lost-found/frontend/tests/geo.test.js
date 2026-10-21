// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import {
  formatLatLng, lngLatToWorld, offsetToLngLat, worldToLngLat, wgs84ToGcj02,
} from '../src/utils/geo'

/** v2.41.1 地图工具：投影可逆、国测局偏移、坐标文案。 */
describe('geo 地图工具', () => {
  it('墨卡托投影可逆（6 位小数内）', () => {
    for (const [lng, lat] of [[114.361, 30.5359], [116.397, 39.908], [-122.4194, 37.7749]]) {
      const p = lngLatToWorld(lng, lat, 16)
      const back = worldToLngLat(p.x, p.y, 16)
      expect(back.lng).toBeCloseTo(lng, 6)
      expect(back.lat).toBeCloseTo(lat, 6)
    }
  })

  it('点选偏移：中心点等于原坐标，往右为东、往下为南', () => {
    const el = { clientWidth: 512, clientHeight: 512 }
    const c = offsetToLngLat(256, 256, el, 30.5359, 114.361, 17)
    expect(c.lat).toBeCloseTo(30.5359, 6)
    expect(c.lng).toBeCloseTo(114.361, 6)
    const right = offsetToLngLat(356, 256, el, 30.5359, 114.361, 17)
    expect(right.lng).toBeGreaterThan(114.361)
    expect(right.lat).toBeCloseTo(30.5359, 6)
    const down = offsetToLngLat(256, 356, el, 30.5359, 114.361, 17)
    expect(down.lat).toBeLessThan(30.5359)
  })

  it('WGS-84 → GCJ-02：国内偏移在几百米量级，境外原样返回', () => {
    const g = wgs84ToGcj02(30.5359, 114.361)
    const dLat = (g.lat - 30.5359) * 111000
    const dLng = (g.lng - 114.361) * 111000 * Math.cos((30.5359 * Math.PI) / 180)
    const dist = Math.hypot(dLat, dLng)
    expect(dist).toBeGreaterThan(100)
    expect(dist).toBeLessThan(1200)
    const paris = wgs84ToGcj02(48.8566, 2.3522)
    expect(paris.lat).toBe(48.8566)
    expect(paris.lng).toBe(2.3522)
  })

  it('坐标文案带方向字母', () => {
    expect(formatLatLng(30.5359, 114.361, 4)).toBe('30.5359° N, 114.3610° E')
    expect(formatLatLng(-33.8688, -70.6693, 4)).toBe('33.8688° S, 70.6693° W')
    expect(formatLatLng(null, null)).toBe('')
  })
})
