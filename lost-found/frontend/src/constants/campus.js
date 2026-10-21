/**
 * 校园平面示意图（v2.41 位置显示用）
 *
 * 为什么不是真实地图：`item.location` 是发布者手填的文本（最多 50 字，发布页用
 * allow-create 的下拉，也允许自建），库里没有经纬度，接高德/腾讯地图也落不到校园
 * 内部的楼栋。所以这里画一张**示意平面图**：8 个预设地点 + 一个兜底"自定义地点"，
 * 把文本匹配到楼块上，只表达"在校园的哪个方位"，不宣称精确坐标。
 *
 * 坐标是 840×540 的 viewBox：北侧教学区、中部食堂与校车站、南侧宿舍与运动区，
 * 正中十字主路 + 环形道路，正门在南侧中间。
 * 版式约定：楼名统一贴楼块**右下角**（text-anchor=end），图钉落在楼块中部，
 * 这样"名称 + 图钉 + 图钉上的地点文案"三者不会叠在一起。改布局时同步改 blocks/label/pin。
 */

export const CAMPUS = { width: 840, height: 540 }

/** 地点未命中任何楼块时的兜底落点（十字主路交叉口，语义="校园内某处"） */
export const CAMPUS_CENTER = { x: 364, y: 266 }

export const CAMPUS_PLACES = [
  {
    key: 'teach',
    name: '第一教学楼',
    zoneKey: 'loc.zone.teach',
    aliases: ['第一教学楼', '教学楼', '教一', '教室', '阶梯教室', '主楼'],
    blocks: [{ x: 86, y: 66, w: 230, h: 116 }],
    pin: { x: 196, y: 122 },
    label: { x: 302, y: 172 },
  },
  {
    key: 'library',
    name: '图书馆',
    zoneKey: 'loc.zone.teach',
    aliases: ['图书馆', '图书室', '阅览室', '自习室', '借阅'],
    blocks: [{ x: 400, y: 66, w: 150, h: 100 }],
    pin: { x: 452, y: 128 },
    label: { x: 538, y: 154 },
  },
  {
    key: 'lab',
    name: '实验楼',
    zoneKey: 'loc.zone.teach',
    aliases: ['实验楼', '实验室', '实训楼', '实训', '机房', '计算机房'],
    blocks: [{ x: 586, y: 66, w: 176, h: 122 }],
    pin: { x: 646, y: 128 },
    label: { x: 750, y: 176 },
  },
  {
    key: 'canteen',
    name: '第二食堂',
    zoneKey: 'loc.zone.live',
    aliases: ['第二食堂', '食堂', '餐厅', '饭堂', '二食', '就餐'],
    blocks: [{ x: 86, y: 220, w: 180, h: 96 }],
    pin: { x: 156, y: 268 },
    label: { x: 254, y: 304 },
  },
  {
    key: 'bus',
    name: '校车站',
    zoneKey: 'loc.zone.transit',
    aliases: ['校车站', '校车', '班车', '校巴', '车站', '公交', '站点'],
    blocks: [{ x: 292, y: 232, w: 52, h: 52, r: 26 }],
    pin: { x: 318, y: 258 },
    label: { x: 318, y: 306, anchor: 'middle' },
  },
  {
    key: 'gym',
    name: '体育馆',
    zoneKey: 'loc.zone.sport',
    aliases: ['体育馆', '球馆', '篮球场', '羽毛球馆', '室内馆', '健身房', '运动馆'],
    blocks: [{ x: 560, y: 216, w: 202, h: 112 }],
    pin: { x: 640, y: 276 },
    label: { x: 750, y: 316 },
  },
  {
    key: 'dorm',
    name: '宿舍楼',
    zoneKey: 'loc.zone.live',
    aliases: ['宿舍楼', '宿舍', '寝室', '公寓', '寝室楼', '宿管', '值班室'],
    blocks: [
      { x: 78, y: 362, w: 100, h: 56 },
      { x: 78, y: 430, w: 100, h: 56 },
      { x: 190, y: 362, w: 100, h: 56 },
    ],
    pin: { x: 128, y: 400 },
    label: { x: 280, y: 408 },
  },
  {
    key: 'track',
    name: '操场',
    zoneKey: 'loc.zone.sport',
    aliases: ['操场', '田径场', '跑道', '运动场', '足球场', '看台'],
    track: { cx: 476, cy: 420, rx: 92, ry: 56 },
    blocks: [],
    pin: { x: 476, y: 404 },
    label: { x: 476, y: 452, anchor: 'middle' },
  },
]

/**
 * 文本 → 楼块。先精确匹配名称，再按「命中别名的长度」取最长的一条：
 * "3号宿舍楼东门" 会命中 宿舍楼 的 "宿舍楼"，"第二食堂二楼" 命中 第二食堂，
 * 而不是被更短的 "食堂"/"门" 之类抢走。
 * @returns {object|null}
 */
export function findPlace(location) {
  const raw = String(location || '').trim()
  if (!raw) return null
  const exact = CAMPUS_PLACES.find((p) => p.name === raw)
  if (exact) return exact
  let best = null
  let bestLen = 0
  for (const p of CAMPUS_PLACES) {
    for (const a of p.aliases) {
      if (a.length > bestLen && raw.includes(a)) {
        best = p
        bestLen = a.length
      }
    }
  }
  return best
}
