/**
 * 演示数据（Mock）。后端接入后整文件可删。
 * 字段与后端接口约定保持一致：type: lost=寻物启事 / found=招领启事
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

const img = (seed) => `https://picsum.photos/seed/lf-${seed}/640/480`

export const items = [
  { id: 1,  type: 'lost',  title: '蓝色保温杯，杯身有小熊贴纸', category: '其他',    location: '图书馆',   time: '2026-09-27 14:30', status: '寻找中', owner: '林同学', views: 86,  desc: '杯身有一只小熊贴纸，杯盖是米色的，可能在三楼自习区座位附近落下。对我很重要，感谢！' },
  { id: 2,  type: 'lost',  title: '校园卡一张（卡号尾号 0527）', category: '证件卡类', location: '第二食堂', time: '2026-09-27 12:10', status: '寻找中', owner: '陈同学', views: 132, desc: '午饭时大概掉在二楼打饭窗口附近，卡套是透明的带挂绳。捡到请与我联系，请喝奶茶！' },
  { id: 3,  type: 'found', title: '捡到一副白色无线耳机',       category: '电子设备', location: '第一教学楼', time: '2026-09-27 09:45', status: '待认领', owner: '王同学', views: 201, desc: '在三楼教室抽屉里发现，充电盒有点划痕。描述对得上就来认领吧。' },
  { id: 4,  type: 'lost',  title: '黑色 Sketchers 运动鞋单只',   category: '衣物饰品', location: '体育馆',   time: '2026-09-26 19:20', status: '寻找中', owner: '赵同学', views: 54,  desc: '打完球装袋时少了一只，42 码。可能有好心人误拿，拜托了！' },
  { id: 5,  type: 'found', title: '一串钥匙带小熊挂件',           category: '钥匙',    location: '宿舍楼',   time: '2026-09-26 18:00', status: '待认领', owner: '刘同学', views: 167, desc: '楼下花坛边捡到的，四把钥匙 + 一个小熊挂件。' },
  { id: 6,  type: 'lost',  title: '考研数学复习全书（上册）',     category: '书籍资料', location: '第一教学楼', time: '2026-09-26 16:40', status: '寻找中', owner: '孙同学', views: 73,  desc: '书里夹了我的笔记，比书本身重要得多，一定归还，重谢！' },
  { id: 7,  type: 'found', title: '捡到学生证（刘某某）',         category: '证件卡类', location: '校车站',   time: '2026-09-26 08:30', status: '待认领', owner: '周同学', views: 245, desc: '早高峰在校车站捡到，已交给班委暂存，请带学生证失主凭证件信息认领。' },
  { id: 8,  type: 'lost',  title: '银色 Apple Pencil（二代）',   category: '电子设备', location: '实验楼',   time: '2026-09-25 21:05', status: '已找回', owner: '吴同学', views: 98,  desc: '做实验时落在桌面，已被好心同学送到值班室，感谢！' },
  { id: 9,  type: 'found', title: '灰色围巾，格纹流苏款',       category: '衣物饰品', location: '操场',     time: '2026-09-25 17:35', status: '待认领', owner: '郑同学', views: 61,  desc: '晚跑时在主席台座椅上捡到，洗得挺干净，失主来认领吧。' },
  { id: 10, type: 'lost',  title: '粉色雨伞，伞柄有蝴蝶结',      category: '其他',    location: '图书馆',   time: '2026-09-25 15:20', status: '寻找中', owner: '林同学', views: 47,  desc: '雨天在门口伞架寄放后被误拿的可能性很大，伞骨有一根略弯。' },
  { id: 11, type: 'found', title: '捡到机械键盘（87 键）',        category: '电子设备', location: '宿舍楼',   time: '2026-09-24 22:10', status: '已认领', owner: '钱同学', views: 189, desc: '楼下快递架旁捡到，键帽是白灰配色。已由失主凭键位特征认领。' },
  { id: 12, type: 'lost',  title: '高等数学练习册 A 本',          category: '书籍资料', location: '第二食堂', time: '2026-09-24 12:50', status: '寻找中', owner: '冯同学', views: 39,  desc: '封面写有名字，内页有大量铅笔演算。' },
  { id: 13, type: 'found', title: '捡到饭卡一张（尾号 3341）',    category: '证件卡类', location: '第一教学楼', time: '2026-09-23 18:25', status: '待认领', owner: '褚同学', views: 156, desc: '一楼自习室桌肚里发现，余额不少，失主速来！' },
  { id: 14, type: 'lost',  title: 'AirPods Pro 2 充电盒',        category: '电子设备', location: '操场',     time: '2026-09-23 07:55', status: '寻找中', owner: '卫同学', views: 121, desc: '晨跑时从腰包滑落，贴纸壳，序列号可提供。' },
  { id: 15, type: 'found', title: '捡到黑色钱包（内有证件）',     category: '证件卡类', location: '体育馆',   time: '2026-09-22 20:40', status: '已认领', owner: '蒋同学', views: 302, desc: '钱包内有身份证和银行卡，已通过失物招领中心联系上失主归还。' },
  { id: 16, type: 'lost',  title: '小米手环 8 黑色',              category: '电子设备', location: '宿舍楼',   time: '2026-09-22 07:30', status: '寻找中', owner: '沈同学', views: 66,  desc: '可能掉在楼道或水房，表带内侧有划痕，有定位最后出现位置。' },
  { id: 17, type: 'found', title: '一本《三体 II》内夹银杏叶',    category: '书籍资料', location: '图书馆',   time: '2026-09-21 16:15', status: '待认领', owner: '韩同学', views: 84,  desc: '还书车上多出来的一本，里面夹着一枚很大的银杏叶书签，很浪漫，速来认领。' },
  { id: 18, type: 'lost',  title: '玩偶挂件（牛奶小羊）',         category: '衣物饰品', location: '校车站',   time: '2026-09-21 08:05', status: '已找回', owner: '杨同学', views: 143, desc: '挂在书包侧袋被蹭掉了，已有人在招领墙发布，已认领！机制真的好用。' },
].map((it, i) => ({ ...it, img: img(i + 1) }))

export const lostItems = items.filter((it) => it.type === 'lost')
export const foundItems = items.filter((it) => it.type === 'found')
export const itemById = (id) => items.find((it) => String(it.id) === String(id))
export const similarTo = (item, n = 3) =>
  items.filter((it) => it.id !== item?.id && it.category === item?.category).slice(0, n)

export const stats = {
  reunited: 128,
  lostCount: 42,
  foundCount: 36,
}

/** 以图搜图 Mock 结果：相似度降序，Top1 突出 */
export function mockSearchResults() {
  const pool = [...items].sort(() => Math.random() - 0.5).slice(0, 6)
  const sims = [97.3, 92.1, 87.6, 83.4, 78.2, 71.5]
  return pool
    .map((it, i) => ({ ...it, similarity: sims[i] }))
    .sort((a, b) => b.similarity - a.similarity)
}
