import { t } from '../i18n'

/**
 * v3 设计系统（Digg 信息流）通用展示逻辑。
 * 只做「数据 → 文案/颜色」的纯函数映射，不引入任何组件状态，
 * 供 FeedCard / ItemCard / FeedRail / AppSidebar 共用，避免各写一套。
 */

/** 业务状态 → 词条 key 与配色类（已认领/已找回统一归入「已找回」） */
export function statusOf(item) {
  const s = item?.status || ''
  if (s === '已找回' || s === '已认领') return { key: 'status.done', cls: 'st-done' }
  return item?.type === 'found'
    ? { key: 'status.found', cls: 'st-found' }
    : { key: 'status.lost', cls: 'st-lost' }
}

/** "2026-10-19 14:30" / ISO → 相对时间；超过 7 天显示 MM-DD */
export function relTime(value) {
  if (!value) return ''
  const d = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(value).slice(5, 16)
  const diff = Date.now() - d.getTime()
  if (diff < 60e3) return t('time.justNow')
  if (diff < 36e5) return t('time.minutes', { n: Math.floor(diff / 60e3) })
  if (diff < 864e5) return t('time.hours', { n: Math.floor(diff / 36e5) })
  if (diff < 7 * 864e5) return t('time.days', { n: Math.floor(diff / 864e5) })
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}-${dd}`
}

/** 头像取首字：中文取第一个字，英文取首字母 */
export function initialOf(name) {
  const s = String(name || '拾').trim()
  return s ? s[0].toUpperCase() : '拾'
}

/** 分类点色板（与 main.css 的 --c-* 对应），用于头像底色轮转 */
const AVATAR_COLORS = ['#4A6CF7', '#2FA46A', '#E8823A', '#8B5CF6', '#E5484D', '#9A9A92']

/** 以昵称做稳定哈希取色：同一个人在任何页面头像颜色一致 */
export function avatarColor(seed) {
  const s = String(seed || '')
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}
