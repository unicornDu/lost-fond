import { api } from '../api'
import { saveBlob } from './saveFile'

/**
 * 交付文件下载（P1）：
 * Web 走受控 blob 下载；Android WebView 写缓存后交给系统分享面板保存。
 * 渲染层只经手 deliveryId，不拼磁盘路径。
 */
export async function downloadDelivery(d) {
  const blob = await api.downloadDelivery(d.id)
  return saveBlob(blob, d.fileName || 'delivery-file', { dialogTitle: d.title || '保存文件' })
}

export const DELIVERY_ICONS = { CLAIM_CERT: '🧾', REPORT_CSV: '📊', MATCH_REPORT: '🖼️' }

export function fmtSize(n) {
  if (n == null) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(2) + ' MB'
}
