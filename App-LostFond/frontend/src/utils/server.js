/**
 * App 端服务器地址。
 *
 * Web / Electron：后端与页面同源，server 为空，API 走相对路径。
 * Android：页面打包在 WebView 内，必须由用户填写 Windows 版（或公网隧道）地址，
 *          例如 http://192.168.1.10:18080，地址保存在 localStorage。
 */
const KEY = 'shiguang_server'
const DEFAULT_SERVER = String(import.meta.env.VITE_DEFAULT_SERVER || '').trim()

export function normalizeServer(raw) {
  let v = String(raw || '').trim()
  if (!v) return ''
  if (!/^https?:\/\//i.test(v)) v = 'http://' + v
  v = v.replace(/\/+$/, '')
  v = v.replace(/\/api$/i, '')
  return v
}

export function getServerBase() {
  return normalizeServer(localStorage.getItem(KEY) || DEFAULT_SERVER)
}

export function setServerBase(url) {
  const v = normalizeServer(url)
  if (v) localStorage.setItem(KEY, v)
  return v
}

export function clearServerBase() {
  localStorage.removeItem(KEY)
}

export function hasServer() {
  return !!getServerBase()
}

export function isAndroidTarget() {
  return import.meta.env.VITE_APP_TARGET === 'android'
}

export function isAppTarget() {
  const target = import.meta.env.VITE_APP_TARGET
  return target === 'android' || target === 'electron'
}

/** axios baseURL：App 端指向用户填写的服务器，Web/Electron 走同源相对路径。 */
export function apiRoot() {
  const base = getServerBase()
  return base ? base + '/api' : '/api'
}

/** 把后端返回的 /uploads/... 转成绝对地址（Android WebView 的页面源不是后端）。 */
export function resolveAsset(p) {
  if (!p) return ''
  if (/^(https?:|data:|blob:)/i.test(p)) return p
  const base = getServerBase()
  if (!base) return p
  return base + (p.startsWith('/') ? p : '/' + p)
}
