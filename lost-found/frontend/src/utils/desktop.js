/**
 * API 来源解析（一套代码三端运行，M-P0 环境变量化）：
 * - Web（http/https，无 VITE_API_BASE）：同源——Vite 代理 / Nginx 兜 /api 与 /uploads；
 * - 桌面（Electron file://）：壳经 query 注入 apiBase（见 desktop/main.cjs 发行配置链）；
 * - 移动（Capacitor）：构建时经 `.env.mobile` 的 VITE_API_BASE 注入服务器地址
 *   （vite build --mode mobile；Android WebView 源是 https://localhost，相对路径不可用）。
 */
const IS_FILE = typeof window !== 'undefined' && window.location.protocol === 'file:'

function readApiBase() {
  try {
    if (IS_FILE) {
      const fromQuery = new URLSearchParams(window.location.search).get('apiBase')
      return (fromQuery || 'http://localhost:8080').replace(/\/+$/, '')
    }
    const fromEnv = import.meta.env?.VITE_API_BASE
    if (fromEnv) return String(fromEnv).replace(/\/+$/, '')
  } catch { /* 走默认 */ }
  return IS_FILE ? 'http://localhost:8080' : ''
}

export const IS_DESKTOP = IS_FILE
/** Capacitor 原生容器（Android WebView / iOS WKWebView） */
export const IS_NATIVE = typeof window !== 'undefined' && !!window.Capacitor?.isNativePlatform?.()
export const API_ORIGIN = readApiBase()

/**
 * 后端相对资源路径（如 /uploads/x.jpg）→ 转绝对地址：
 * - 桌面（file://）：无同源，必须拼 apiBase；
 * - 移动原生（Capacitor，页面源是 https://localhost）：相对路径会打到 APK 内的本地
 *   服务器（404），同样要拼 API_ORIGIN（F-13，真机图片才显示）；
 * - Web / PWA：同源，原样返回。
 */
export function assetUrl(p) {
  if (!p) return p
  return (IS_DESKTOP || IS_NATIVE) && p.startsWith('/') ? API_ORIGIN + p : p
}
