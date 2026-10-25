import { resolveAsset } from './server'

/**
 * 静态资源地址。
 *
 * Web / Electron：前后端同源，直接返回相对路径；
 * Android：后端地址由用户在启动时填写，需要拼成 http(s)://host:port/uploads/...
 */
export function assetUrl(p) {
  return resolveAsset(p)
}
