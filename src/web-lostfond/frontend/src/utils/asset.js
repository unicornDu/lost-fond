/**
 * 静态资源地址（Web 单端）。
 *
 * 前后端同源：后端把 `/uploads/**` 与 `frontend/dist` 一起托管，所以直接用相对路径即可。
 * 保留这一层是为了以后接 CDN / 对象存储时只改这里（`assetUrl(p) => CDN + p`），
 * 不用去改几十处模板。
 */
export function assetUrl(p) {
  return p || ''
}
