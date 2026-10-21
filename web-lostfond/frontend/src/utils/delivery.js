import { api } from '../api'

/**
 * 交付文件下载（P1，Web 单端）：
 * 受控下载 blob → a[download]；三件套响应头（no-store / nosniff / CSP）由后端保证，
 * 渲染层只经手 deliveryId，不拼磁盘路径。
 */
export async function downloadDelivery(d) {
  const blob = await api.downloadDelivery(d.id)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = d.fileName || 'delivery-file'
  a.click()
  URL.revokeObjectURL(url)
  return 'downloaded'
}

export const DELIVERY_ICONS = { CLAIM_CERT: '🧾', REPORT_CSV: '📊', MATCH_REPORT: '🖼️' }

export function fmtSize(n) {
  if (n == null) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(2) + ' MB'
}
