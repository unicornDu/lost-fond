import { api } from '../api'
import { IS_DESKTOP } from './desktop'

/**
 * 交付文件动作（桌面端 P1）：
 * - 桌面端：/file/open 由服务端按 id 读回绝对路径 → 壳 IPC shell.openPath 原生打开（dsh 的"下载=原生打开"）
 * - Web 端：受控下载 blob → a[download]（三件套响应头由后端保证）
 * 渲染层全程只经手 deliveryId，不拼路径。
 */
export async function openOrDownload(d) {
  if (IS_DESKTOP && window.shiguangDesktop?.openPath) {
    const info = await api.openDelivery(d.id)   // 权限类错误（403/404）照常抛给调用方
    const err = await window.shiguangDesktop.openPath(info.path)   // 成功返回 ''，失败返回错误文案
    if (!err) return 'opened'
    // 原生打开失败（典型：后端在远程机器，路径在本机不存在）→ 降级为受控下载
  }
  const blob = await api.downloadDelivery(d.id)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = d.fileName || 'delivery-file'
  a.click()
  URL.revokeObjectURL(url)
  return 'downloaded'
}

/** 仅桌面端：在文件管理器中显示（dsh present.open 的 reveal 动作）。 */
export async function revealInFolder(d) {
  if (!IS_DESKTOP || !window.shiguangDesktop?.revealPath) throw new Error('仅桌面端支持此操作')
  const info = await api.openDelivery(d.id)
  window.shiguangDesktop.revealPath(info.path)
  return true
}

export const DELIVERY_ICONS = { CLAIM_CERT: '🧾', REPORT_CSV: '📊', MATCH_REPORT: '🖼️' }

export function fmtSize(n) {
  if (n == null) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(2) + ' MB'
}
