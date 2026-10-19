import { ref } from 'vue'

/**
 * 主题（亮 / 暗）。
 * - 令牌在 styles/main.css：html[data-theme="dark"] 覆盖应用色板；
 *   同时给 <html> 加 .dark 类，启用 Element Plus 官方暗色变量（弹窗/表单/表格）。
 * - 首次无记录时跟随系统 prefers-color-scheme；用户手动切换后写入 localStorage。
 */
const KEY = 'shiguang_theme'
export const theme = ref('light')

function systemDark() {
  return typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: dark)').matches
}

export function applyTheme(next, persist = true) {
  const t = next === 'dark' ? 'dark' : 'light'
  theme.value = t
  const root = document.documentElement
  root.dataset.theme = t
  root.classList.toggle('dark', t === 'dark')
  root.style.colorScheme = t
  if (persist) {
    try { localStorage.setItem(KEY, t) } catch (e) { /* 隐私模式忽略 */ }
  }
  window.dispatchEvent(new CustomEvent('shiguang-theme-changed', { detail: t }))
}

export function initTheme() {
  let saved = null
  try { saved = localStorage.getItem(KEY) } catch (e) { /* 隐私模式忽略 */ }
  applyTheme(saved || (systemDark() ? 'dark' : 'light'), false)
}

export function toggleTheme() {
  applyTheme(theme.value === 'dark' ? 'light' : 'dark')
}
