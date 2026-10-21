import { ref, watch } from 'vue'

/**
 * 列表 / 网格视图偏好（v2.41）
 *
 * 之前 `view` 是页面里的一个普通 ref：进详情页时首页被卸载，返回时重新挂载，
 * 值回到初始的 list —— 于是「点网格 → 进详情 → 返回」会莫名变成列表。
 * 这里把选择落到 localStorage（与 shiguang_rail_closed / shiguang_theme 同一套做法）：
 * 同浏览器下次进来仍是上次的视图，刷新也不丢。
 *
 * @param {string} key localStorage 键名
 * @param {'list'|'grid'} fallback 没有记录时的默认视图
 */
const MODES = ['list', 'grid']

export function useViewMode(key, fallback = 'list') {
  let init = MODES.includes(fallback) ? fallback : 'list'
  try {
    const saved = localStorage.getItem(key)
    if (MODES.includes(saved)) init = saved
  } catch (e) { /* 隐私模式：退回默认值 */ }

  const view = ref(init)
  watch(view, (v) => {
    if (!MODES.includes(v)) return
    try { localStorage.setItem(key, v) } catch (e) { /* 写入失败不影响当前会话 */ }
  })
  return view
}
