import { describe, it, expect } from 'vitest'
import { findPlace, CAMPUS_PLACES } from '../src/constants/campus.js'

/**
 * v2.41 位置显示：地点文本 → 校园平面图楼块。
 * 发布页的地点下拉 allow-create，所以文本也可能是「3号宿舍楼东门」这类自建串，
 * 这里锁住"最长别名优先"的匹配规则，避免以后加别名时互相抢命中。
 */
describe('findPlace 地点匹配', () => {
  it('8 个预设地点都能精确命中', () => {
    for (const p of CAMPUS_PLACES) {
      expect(findPlace(p.name)?.key).toBe(p.key)
    }
  })

  it('带前缀/后缀的自建文本按包含关系命中', () => {
    expect(findPlace('3号宿舍楼')?.key).toBe('dorm')
    expect(findPlace('第二食堂二楼')?.key).toBe('canteen')
    expect(findPlace('图书馆东门')?.key).toBe('library')
    expect(findPlace('操场看台下')?.key).toBe('track')
    expect(findPlace('老实验楼 302')?.key).toBe('lab')
  })

  it('取命中别名里最长的一条，不被短别名抢走', () => {
    // 「教学楼」比「教室」长，应落到第一教学楼；「食堂」不应盖过「第二食堂」
    expect(findPlace('第一教学楼A座')?.key).toBe('teach')
    expect(findPlace('第二食堂')?.key).toBe('canteen')
  })

  it('完全对不上的文本返回 null（由调用方落兜底点）', () => {
    expect(findPlace('')).toBeNull()
    expect(findPlace(null)).toBeNull()
    expect(findPlace('南门快递驿站')).toBeNull()
  })
})
