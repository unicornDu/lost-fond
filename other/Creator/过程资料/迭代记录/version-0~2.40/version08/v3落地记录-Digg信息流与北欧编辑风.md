# v3 视觉落地记录 · Digg 信息流 + 北欧编辑风

> 日期：2026-10-19 ｜ 范围：前端全部页面（未改后端、未改数据库）
> 设计输入：
> - `Creator/过程资料/迭代记录/version-0~2.40/version08/参考imgs/Digg信息流-复刻生成指南.md`（登录后应用）
> - `Creator/过程资料/迭代记录/version-0~2.40/version08/前端版本01/复刻生成指南.md`（公开首页）
> - 素材：`web-lostfond/frontend/src/assets/img/01–04.jpg`（用户提供，4 张全屏轮播图）
> 结论：**两套风格分别落在"登录前"与"登录后"**——公开首页走北欧极简 editorial（图像主导、宋体大标题），
> 登录后的应用走 Digg 信息流（三栏骨架、近黑单色、分类彩点）。原 v2「方案G 编辑叙事」退役。

---

## 1. 为什么是两套、各自落在哪

| 页面 | 路由 | 风格来源 | 核心特征 |
|---|---|---|---|
| 公开首页（未登录） | `/` | dsgninterior.se 复刻指南 | 4 屏全高图片轮播 + 固定透明页头 + 深棕页脚；衬线大标题；无卡片无阴影 |
| 登录 / 注册 | `/auth` | 沿用现有版式 | 仅随令牌换成近黑主色，去掉暖橙光晕 |
| 登录后全部页面 | `/home`、`/wall`、`/item/:id`、`/publish`、`/search-image`、`/profile`、`/admin/*` | Digg 信息流复刻指南 | 左 232 竖导航 / 中信息流 / 右 300 信息栏；纸白 + 近黑；分类小圆点是唯一彩色 |

两套风格共用同一套"不写死颜色"的纪律：应用端继续走 `styles/main.css` 的 `:root` 令牌（旧变量名保留、只换值），
公开页在自己的 `.w` 作用域里定义 `--w-*` 令牌，互不污染。

---

## 2. 应用端（登录后）· 设计系统 v3

### 2.1 令牌（`frontend/src/styles/main.css` → `:root`）

| 变量（保留旧名） | v3 色值 | 用途 |
|---|---|---|
| `--cream` / `--paper` | `#FAFAF8` / `#FFFFFF` | 纸白底 / 卡片面 |
| `--cream-deep` | `#F4F4F1` | 左导航选中、hover 底 |
| `--ink` / `--ink-soft` / `--ink-faint` | `#141412` / `#6F6F68` / `#A3A39B` | 三级文字 |
| `--line` / `--line-2` | `#EBEBE7` / `#E2E2DD` | 极浅分隔线 / hover 描边 |
| `--orange` / `--orange-deep` | `#161614` / `#0A0A09` | 唯一强调色改为**近黑**（旧名保留，全站零改动换肤） |
| `--orange-soft` | `#F1F1ED` | 强调浅底 |
| `--st-lost-bg/-ink` | `#FBEEEC` / `#C2402F` | 寻找中（淡红 pill） |
| `--st-found-bg/-ink` | `#E9F5EE` / `#1E7A4C` | 招领中（淡绿 pill） |
| `--st-done-bg/-ink` | `#EFEFEA` / `#6F6F68` | 已找回（灰 pill） |
| `--c-doc/key/elec/book/cloth/other` | `#4A6CF7` / `#E8823A` / `#2FA46A` / `#8B5CF6` / `#E5484D` / `#9A9A92` | 六个分类小圆点（全站唯一彩色） |

Element Plus 主色同步为 `#161614`，圆角基准 12px；主按钮去掉了 v2 的斜向扫光，改为 hover 上浮 1px + 中性投影。
`ItemCard` / `FeedCard` / `FeedRail` / `AppSidebar` 的圆角体系：卡片 18px、图片 14px、按钮与标签全圆角。

### 2.2 三栏骨架

- 新增 `components/AppShell.vue`：`232px | minmax(0,1fr) | 300px`，`max-width 1240px` 居中。
  左栏与右栏 `position: sticky; top: 0; height: 100vh; overflow-y: auto`，两栏各自独立滚动，只有中栏跟随页面。
  **2026-10-20 追加**：右栏可收起——外壳宽度 300→0（`.42s cubic-bezier(.22,1,.36,1)`），骑缝把手点击切换，
  状态存 `localStorage.shiguang_rail_closed`；折叠时中栏吃掉 300px，内容上限由 `--feed-max/--wall-max/--detail-max` 放宽。
- 新增 `components/AppSidebar.vue`：品牌字标 + 图标导航（首页 / 失物墙 / 招领墙 / 以图搜图 / 发布启事 /
  通知 / 聊天 / 后台管理）+ 底部近黑 pill CTA + 用户卡。
  失物墙与招领墙共用 `/wall`，按 `query.type` 区分选中；**2026-10-20 起**去掉「个人中心」项（点底部头像进入），
  通知 / 聊天分别跳 `/notifications`、`/messages` 独立页，右侧 `InboxDrawer` 已于 2026-10-20 下线；
  未读 30s 轮询 + `shiguang-unread-changed` 即时刷新（沿用旧 NavBar 逻辑）。
- 新增 `components/FeedRail.vue`：① pHash 推广卡（近黑）② 找回战报（`/api/stats`）
  ③ 热门启事（`/items?sort=hot`）④ 为你推荐（收藏品类规则，未登录不渲染）。全部接真实接口，不编造数字。
- 响应式：`≤1080px` 收右栏（三栏→两栏）；`≤860px` 收左栏，改移动顶栏 + `el-drawer` 抽屉。
- 右栏只在信息流类页面出现（`home / wall / detail`），表单类页面让出宽度。
- 页面滚动条改为 `scrollbar-gutter: stable`：不同页面高度切换时不再因滚动条出现/消失把居中布局横向推动
  （修「点左侧导航整列抖一下」，实测侧栏 x 恒为 95px）。

### 2.3 信息流与卡片

- 新增 `components/FeedCard.vue`（Digg 主卡）：分类圆点 + 状态淡彩 pill → 19px/800 标题 →
  两行摘要 → 2.35:1 大图（左下地点 pill，半透明近黑 + 毛玻璃）→ meta 行（头像/昵称/相对时间 + 浏览/收藏/帮转）。
- `components/ItemCard.vue` 重写为网格卡：1.45:1 封面 + 分类点 + 14.5px 标题 + 地点 + 底栏头像与状态 pill；
  保留收藏星标与以图搜图相似度徽标。
- `HomeView.vue` 去掉 Hero / 计数墙 / 特稿卡，改为「信息流抬头 + 工具条 + 单列流」：
  排序（最新/最热）、类型（全部/寻物/招领）、列表-网格切换、加载更多；数据为寻物 12 + 招领 12 合并按时间排序。
- `WallView.vue` 改为工具条（墙别 / 排序 / 视图切换）+ 胶囊搜索条 + 分类胶囊 + 网格或列表；
  搜索历史、分页、错误态全部保留。
- `DetailView.vue` 改为 Digg 详情：返回 pill → 白卡（分类行/状态、26px 标题、描述、2.1:1 大图、三格事实卡、
  发布者行、主按钮+幽灵按钮）→ 认领进度时间线 → 认领流程步骤卡 → 相似启事。
- `GradientSearchBar.vue` 去掉渐变光晕与旋转贴纸手感，改为单条胶囊（关键词 + 地点 + 圆形执行钮）。

### 2.4 删除 / 保留

| 处理 | 文件 | 原因 |
|---|---|---|
| 删除 | `components/NavBar.vue` | 顶部导航由左栏取代，未读/抽屉逻辑已迁入 AppSidebar |
| 删除 | `components/GooeyNav.vue`、`components/SplitText.vue` | 落地页与首页改版后无引用（git 历史可找回） |
| 保留 | `PosterDialog` / `GradientSearchBar`（重写样式） | 功能不变，随令牌换肤；`InboxDrawer` 已下线 |

---

## 3. 公开首页（登录前）· 北欧极简 editorial

### 3.1 字体决策（用户授权"字体你来发挥"）

| 用途 | 字体栈 | 理由 |
|---|---|---|
| 标题（中/英） | `Noto Serif SC` → `Noto Serif` → `Songti SC` → `STSong` → Georgia | 本机装有思源宋体可变字重（`NotoSerifSC-VF.ttf`）。超大字号用 **500 字重**而非 800，避免黑体的"通告感"，贴近画廊/杂志的编辑气质 |
| 正文 | `Noto Sans SC` → `Segoe UI` → `PingFang SC` → `Microsoft YaHei` | 宋体标题 + 黑体正文的对比，长文可读性优先 |
| 拉丁小标签 | `Century Gothic` 大写 + `.2em` 字距 | 几何无衬线的现代感，与宋体形成风格对位 |

不引入任何外链字体（内网/离线部署不破相），全部走系统已装字体并可回退。

### 3.2 结构（2026-10-19 晚定稿：固定舞台 + 弹层）

1. **4 屏固定舞台**：`html.w-landing` 锁 `overflow: hidden`，页面本身不产生滚动条。
   介绍屏 + 失物墙 / 招领墙 / 以图搜图三个入口屏（整屏可点）竖向叠在 `.deck` 里，
   每屏 `100svh`；文字区用"局部暗池"（径向渐变）保证可读，同时不压暗整张照片。
2. **翻页**：滚轮 / 触摸滑动 / ↑↓ / PageUp·PageDown / 空格一次只翻一屏，
   `translate3d(0, (i - cur) * 100%, 0)` + 1050ms `cubic-bezier(.76,0,.24,1)`；图片同时 `scale(1.1 → 1)`。
   **第 4 屏就是最后一屏**，继续向下滚不会出现第 5 屏（与目标站点一致）。
3. **关于 / 流程 / 能力 / 数据**不再是长页内容，改为页头导航点击后弹出的整页面板：
   `Teleport` 到 body，模糊压暗遮罩 + `min(74rem, 100%) × min(90svh, 58rem)` 纸白面板；
   顶栏为 拾光字标 + 四个分页 + 进入平台 + 关闭，下划线随面板内滚动跟随当前段。
   **每次打开「数据」都会重新请求 `/api/stats`**（挂载时也拉一次），并在说明里显示「更新于 HH:MM」——数字跟着库走，不是写死的。
   面板内部独立滚动（`overscroll-behavior: contain`），ESC / 关闭按钮 / 点击遮罩均可退出。
4. **CTA + 页脚**放在面板内容末尾（深棕整幅），不再依赖页面滚动才能看到。
5. 移动端（≤64rem）：面板变成整屏页（无遮罩留边），页头收为「菜单」；
   菜单项点击后直接打开对应分页面板，全屏深棕菜单仍保留错落入场。

### 3.3 工程细节

- 翻页锁 `SLIDE_LOCK = 1050ms` 与 CSS 过渡时长共用常量，一次手势只消化一屏，不会连翻。
- 触控板：滚轮增量累计超过 18px 才翻页；两次事件间隔 > 220ms 视为新手势，累计清零，避免残余增量误触发。
- 视差由图片 `scale(1.1 → 1)` 承担（不再用 scroll 事件改 `--py`），整页零 scroll 监听。
- 正文网格用 **CSS subgrid**，保证"标签列 + 标题 + 正文"三块在所有 section 严格对齐；栏间距用 `--w-col-gap` 统一。
- 弹层用 `Teleport` 到 body，作用域样式仍生效；但 CSS 变量定义在 `.w` 上不会随 Teleport 继承，
  因此令牌同时挂在 `.info` 上（`.w, .info { --w-* }`）。
- 弹层滚动区用 `tabindex="-1"` 接收焦点，但取消焦点环（否则面板顶部会出现一条橙色细线）。
- 首屏图 `loading="eager"`，其余图片按需加载；`prefers-reduced-motion` 下翻页与缩放全部瞬时完成。

### 3.4 登录 / 注册页（2026-10-19 晚，按 `登录页设计/屏幕截图` 复刻）

| 项 | 落地 |
|---|---|
| 版式 | 左右 `49.4 : 50.6` 分栏，整屏 `calc(100svh - safe-top)`；左紫右白，去掉旧版「左表单 + 右大图」 |
| 左栏 | 紫色渐变 `#7173F6 → #6365F1 → #5650E8`（叠加 28px 栅格与左上柔光）；顶部字标、底部三个说明链接（`ElMessageBox` 承载） |
| 角色 | 四个纯色块用绝对定位百分比还原参考图比例（紫 `#6C3FF5` / 黑 `#2D2D2D` / 橙 `#FF9B6B` / 黄 `#E8D754`），紫与黑为白眼球 + 黑瞳，橙与黄为圆点眼；底边对齐 |
| 鼠标跟随 | 全局 `pointermove` → 舞台中心归一化 `--mx/--my`；瞳孔 `left/top` 按百分比偏移，身体按 `--lx/--ly/--rot/--sq` 做平移 / 旋转 / 压缩（`transform-origin: 50% 100%`）；`prefers-reduced-motion` 与 `pointer: coarse` 直接跳过 |
| 表单 | 原生 input（不套 el-input）以便精确还原 48px 高 / 9px 圆角 / `#F9FAFB` 底；密码自绘眼睛切换；「记住我 30 天」补发 `rememberMe`，修掉旧版复选框不生效的问题 |
| 注册 | 同页切换，注册态多 昵称 + 确认密码；`/auth?tab=register` 仍可直达 |
| 响应式 | ≤62rem 隐藏插图，顶部字标 + 返回首页，表单整宽且页面可滚 |
| 已删除 | 旧版右侧 seed 大图 + `assetUrl` 依赖、Element Plus 输入框封装、旧入场动画类 |

---

## 4. 文件级改动

### 新增
```
frontend/src/components/AppShell.vue      三栏骨架 + 右栏折叠 + 移动顶栏
frontend/src/components/AppSidebar.vue    左侧竖导航（桌面 + 抽屉两套 DOM）
frontend/src/components/FeedRail.vue      右栏四块面板
frontend/src/components/FeedCard.vue      信息流主卡
frontend/src/utils/display.js             状态文案/相对时间/头像首字与配色的纯函数
frontend/src/composables/useTheme.js      亮/暗主题状态（localStorage + prefers-color-scheme）
frontend/src/i18n/index.js                简/繁/英词条（441 key）+ t()/setLocale()/分类与状态映射
```

### 重写
```
frontend/src/App.vue                      登录后统一进 AppShell，公开页独立
frontend/src/views/WelcomeView.vue        公开首页（北欧编辑风，约 900 行）
frontend/src/views/HomeView.vue           信息流
frontend/src/views/WallView.vue           网格/列表墙
frontend/src/views/DetailView.vue         Digg 详情
frontend/src/components/ItemCard.vue      网格卡
frontend/src/components/GradientSearchBar.vue  胶囊搜索条
frontend/src/styles/main.css              设计系统 v3 令牌与通用件
frontend/index.html                       theme-color / favicon / 首屏底色
```

### 删除
```
frontend/src/components/NavBar.vue
frontend/src/components/GooeyNav.vue
frontend/src/components/SplitText.vue
```

### 顺带修
- `SearchImageView` 的「已接入真实后端」说明条：原为 flex 行内散装文本，在窄栏里被拆成竖排乱码，改为整段 `span`。
- 全局遗留暖橙阴影（`rgba(255,125,46,…)`）全部替换为中性黑阴影；`#FFF7EC` 统一为 `#FAFAF8`。
- `.demo-tip` 从蜜黄提示块改为中性纸底 + 左侧墨线（不再破坏单色体系）。
- `PublishView` / `ProfileView` 在数据变更后派发 `shiguang-items-changed`，侧栏计数与右栏榜单即时刷新。
- **轮播图优化**：用 ffmpeg 生成 1600px 宽、`-q:v 3` 的 `01-1600.jpg`…（合计约 0.76 MB），原图 `01–04.jpg` 保留为母版、不参与打包；`dist` 从 9.1 MB 降到 3.1 MB。

---

## 5. 暗色主题与多语言（2026-10-19 追加）

初版 V3 把这两项标为"未做"。本轮按用户要求补齐，均不引入第三方依赖（i18n 用 90 行自研模块，未装 vue-i18n）。

### 5.1 暗色主题

| 层 | 做法 |
|---|---|
| 令牌 | `styles/main.css` 新增 `html[data-theme="dark"]` 覆盖：`--cream:#131312` / `--paper:#1C1C1A` / `--cream-deep:#1F1F1D` / `--ink:#F2F1EC` / `--ink-soft:#A6A69E` / `--ink-faint:#6E6E66` / `--line:#2A2A27` / `--line-2:#3C3C37` |
| 强调色 | 暗色下**反白**：`--orange:#F2F1EC`（按钮浅底深字）、`--orange-soft:#262624`；状态签改 `rgba(色,.16)` 底 + 提亮字色；分类彩点保持色相不变 |
| Element Plus | `main.js` 引入官方 `theme-chalk/dark/css-vars.css`，切换时同时给 `<html>` 加 `.dark` 与 `data-theme="dark"`；暗色下 `--el-color-primary` 反白，并单独覆盖 `--el-button-text-color` 让主按钮文字翻回深色 |
| 状态 | `composables/useTheme.js`：`theme` ref + `initTheme()` + `toggleTheme()`，写 `localStorage.shiguang_theme`，无记录时跟随 `prefers-color-scheme` |
| 首屏闪烁 | `index.html` 在样式之前加一段内联脚本，先读 localStorage 把 `data-theme` / `.dark` 落到 `<html>`，并按主题设首屏底色 |
| 墨底文字 | 全站 `background: var(--ink)` 的地方，文字从 `#fff` 改为 `var(--paper)`——亮色下仍是白字，暗色下自动翻成深字（侧栏 CTA、激活胶囊、时间线打勾、聊天气泡、详情主按钮等 12 处） |
| 图表 | 后台 ECharts 的坐标轴/图例/折线颜色改为从 CSS 令牌读取，并监听 `shiguang-theme-changed` 销毁重建（canvas 不跟 CSS 变量走） |

### 5.2 简 / 繁 / 英多语言

| 项 | 做法 |
|---|---|
| 实现 | `i18n/index.js`：`locale` ref + `t(key, params)`（`{n}` 占位替换）+ `setLocale()`；模板里用 `$t('nav.home')`（`main.js` 注入 globalProperties），脚本里 `import { t }` |
| 响应式 | `t()` 在渲染期读取 `locale.value`，切语言后所有用到 `$t` 的模板自动重渲染；Element Plus 内置文案（分页/空态/日期）由 `App.vue` 的 `el-config-provider` 跟随 |
| 词条 | **441 个 key × 3 语言**，用脚本校验三份字典 key 完全一致；覆盖导航、首页/失物墙/详情、发布表单、以图搜图、个人中心六页签、登录注册、用户主页、404、聊天/通知抽屉、分享海报、状态与分类 |
| 数据映射 | 分类名与状态是后端中文数据，不改库：`catLabel()` / `claimStatusLabel()` / `itemStatusLabel()` 在显示层映射；启事标题、描述、地点等 UGC 保持原文 |
| 标题 | 路由 `meta.titleKey` + `afterEach` 生成浏览器标题；`App.vue` 监听 `locale` 在切语言时即时刷新 |
| 持久化 | `localStorage.shiguang_locale`，并同步 `<html lang>` |
| 入口 | 左栏底部设置区：`亮色/暗色` 单按钮（图标与文案都指向"切换目标"）+ `简/繁/EN` 下拉（选中项 ✓） |

### 5.3 覆盖边界（有意为之）

- **公开首页 `/` 始终中文**：它是中文编辑排版的品牌页，语言开关在登录后的左栏；暗色下也保持暖色纸白，不跟随应用主题。
- **后台 `/admin/*` 保持中文**：内部运维工具，未纳入词条（图表已适配暗色）。
- **用户生成内容不翻译**：启事标题/描述/地点/昵称按原文显示，符合"UGC 不机翻"的常规做法。

---

## 6. 验收

| 项 | 结果 |
|---|---|
| `npm run build` | 通过（16.5s）；`element-plus` **819.58 kB** 单 chunk（含官方暗色变量与 en/zh-tw 语言包，F-11 约定保持）、`echarts` 531.18 kB、`vendor` 296.37 kB、`index` 96.87 kB（词条）、各页面按路由分包 |
| `npm test`（vitest） | **12/12 通过**（api 拦截器 / 路由守卫 / user store），零改动 |
| 逐页核对 | 1440×900 桌面：落地页 4 屏 + 关于/流程/能力/数据/CTA/页脚、首页、失物墙、详情、以图搜图、发布、个人中心、后台、404、登录；390×844 移动：首页、落地页首屏、抽屉、菜单覆盖层；1000×800 平板：右栏收起 |
| 主题 × 语言 | 亮/暗 × 简/繁/英 组合抽查首页、失物墙、详情、发布、搜图、个人中心、登录、后台；暗色下重查输入框、主按钮、选中态、图表坐标轴；语言切换后浏览器标题同步 |
| 公开首页交互 | 1440×900 / 390×844 / 1024×768 三档：滚轮一次翻一屏、第 4 屏后不再前进（`scrollHeight === innerHeight`、`scrollY` 恒为 0）；过渡中帧确认竖向滑入；导航四个分页各自定位；面板内滚轮只滚面板不改屏；ESC / 关闭 / 遮罩均可退出；移动菜单打开对应分页 |
| 断点 | 1080px 收右栏、860px 收左栏、640px 收页头导航，切换过程无布局跳动 |
| 无障碍 | 所有动效尊重 `prefers-reduced-motion`；抽屉/弹窗保留 Esc 与遮罩关闭；标题层级 h1/h2 语义完整 |

截图仅用于本地核对，未入库。

---

## 7. 已知项 / 未纳入本次范围

1. **轮播图已优化**：使用 `ffmpeg -vf scale=1600:-2 -q:v 3` 生成 `01-1600.jpg`…，合计约 0.76 MB；原图保留为母版。替换素材时按同样参数重新生成、改 `WelcomeView` 的 import 即可。
2. 轮播采用原生滚动而非 Swiper，因此没有"整屏吸附"的强轮播手感；换取了零依赖与移动端不抢滚动的稳定性。
3. `/uploads` 演示图仍沿用种子数据；落地页使用的是用户新提供的 4 张插画图，与启事图片无关。
4. 公开首页与后台不参与语言/主题切换（见 5.3）；若后续要求"全站多语言"，需再抽 124 + 6 页的文案。

---

## 8. 回退方式

本次改动全部在前端，且未提交。若需回到 v2：

```bash
cd web-lostfond/frontend
git checkout -- src
git clean -fd src/components src/utils src/composables src/i18n   # 删除新增组件、主题与词条模块
git status --short                       # 确认只剩 assets/img（用户素材）
```

（提交前请按 `Creator/CONTRIBUTING.md` 先 `git status` 看清单，禁用 `git add .`。）
