/**
 * 修复 F-09：原 src/mock/data.js 精简并重命名——只保留真正被引用的常量。
 * 假启事/假统计/mockSearchResults 等死代码已删除（构建产物里曾多出 data-*.js chunk）。
 */
export const CATEGORIES = [
  { name: '证件卡类', emoji: '🪪' },
  { name: '钥匙', emoji: '🔑' },
  { name: '电子设备', emoji: '🎧' },
  { name: '书籍资料', emoji: '📚' },
  { name: '衣物饰品', emoji: '🧣' },
  { name: '其他', emoji: '📦' },
]

export const LOCATIONS = [
  '第一教学楼', '图书馆', '第二食堂', '操场', '宿舍楼', '实验楼', '校车站', '体育馆',
]

export const catEmoji = (name) => CATEGORIES.find((c) => c.name === name)?.emoji || '📦'

/**
 * v3 设计系统（Digg 信息流）：分类小圆点是全站唯一的彩色来源。
 * 颜色令牌见 styles/main.css 的 --c-*，这里只做「分类名 → 令牌」映射，
 * 新增分类时同步补一行即可（缺省落到灰色「其他」）。
 */
const CATEGORY_DOT = {
  证件卡类: 'var(--c-doc)',
  钥匙: 'var(--c-key)',
  电子设备: 'var(--c-elec)',
  书籍资料: 'var(--c-book)',
  衣物饰品: 'var(--c-cloth)',
  其他: 'var(--c-other)',
}
export const catDot = (name) => CATEGORY_DOT[name] || 'var(--c-other)'
