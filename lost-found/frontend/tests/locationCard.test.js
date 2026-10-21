// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LocationCard from '../src/components/LocationCard.vue'
import { t } from '../src/i18n'

/**
 * v2.41.1 位置卡（方案 A）：折叠 → 展开、有坐标/无坐标两种态。
 * jsdom 没有布局，瓦片铺不进去（paintTiles 返回 0），这里只验结构与文案。
 */
function mountCard(props = {}) {
  return mount(LocationCard, {
    props: { location: '图书馆', lat: 30.5359, lng: 114.361, category: '衣物饰品', ...props },
    global: { config: { globalProperties: { $t: t } } },
  })
}

describe('LocationCard 位置卡', () => {
  it('折叠态显示地点与坐标，点击后展开', async () => {
    const w = mountCard()
    expect(w.find('.lc').classes()).not.toContain('open')
    expect(w.text()).toContain('图书馆')
    expect(w.text()).toContain('30.53590° N, 114.36100° E')
    expect(w.find('.lc-pin').exists()).toBe(true)

    await w.find('.lc').trigger('click')
    await nextTick()
    expect(w.find('.lc').classes()).toContain('open')
    // 展开后不再显示"点击展开"提示
    expect(w.find('.lc-hint').exists()).toBe(false)
    w.unmount()
  })

  it('没有坐标时走兜底：不挂图钉样式，写明未标注精确位置', async () => {
    const w = mountCard({ lat: null, lng: null })
    expect(w.find('.lc').classes()).not.toContain('has-coord')
    expect(w.find('.lc-fallback').exists()).toBe(true)
    expect(w.text()).toContain(t('loc.noCoord'))
    await w.find('.lc').trigger('click')
    expect(w.find('.lc').classes()).toContain('open')
    w.unmount()
  })

  it('暴露 expand() 供详情页文字链调用', async () => {
    const w = mountCard()
    w.vm.expand()
    await nextTick()
    expect(w.find('.lc').classes()).toContain('open')
    w.unmount()
  })
})
