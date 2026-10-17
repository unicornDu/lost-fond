# Prompt 04 · P2 前端修复：交互缺陷、打包体积、外网依赖、夸大文案

> 前置：先读同目录 `00-索引与执行顺序.md`。
> 前端代码在 `lost-found/frontend/src`，技术栈 Vue 3.5 + Vite 6 + Element Plus + Pinia + ECharts 6。
> 本 Prompt 的问题均可通过"读代码 + 浏览器实测"验证，不需要数据库。
> （如果你更希望前端 Prompt 单独放 `Zcode_前端` 目录，直接移动本文件即可，内容不依赖目录。）

---

## 任务总览

| ID | 严重度 | 标题 | 主要文件 |
|---|---|---|---|
| F-01 | P1 | 驳回理由被前端丢弃、后端也不存 | `views/admin/AuditView.vue`（+ 后端 `AdminController`，与 `01` 联动） |
| F-02 | P1 | 进入 `/profile?tab=msg` 不会标记已读 | `views/ProfileView.vue` |
| F-03 | P1 | 首页"以图搜图"卡片声称支持拖入，实际丢弃文件 | `components/SearchUploadCard.vue` |
| F-04 | P2 | 表单长度限制与后端/DB 不一致 | `views/PublishView.vue` |
| F-05 | P2 | 删除/下架操作吞掉后端错误 | `views/admin/AuditView.vue`、`views/admin/UsersView.vue` |
| F-06 | P2 | 打包体积：ECharts 与 Element Plus 全量引入 | `views/admin/DashboardView.vue`、`main.js`、`vite.config.js` |
| F-07 | P2 | 落地页/登录页依赖外网图片（内网部署会破图） | `views/WelcomeView.vue`、`views/AuthView.vue` |
| F-08 | P2 | 编造的社交证明与不存在的功能宣传 | `views/WelcomeView.vue` |
| F-09 | P2 | 死代码与遗留 mock 未清理 | `src/mock/data.js`、`src/api/index.js` |
| F-10 | P2 | 管理员工具无入口（reindex / regen-thumbs） | `views/admin/*`、`src/api/index.js` |
| F-11 | P2 | 分页/列表交互细节 | `views/WallView.vue`、`views/admin/*` |

---

## F-01（P1）驳回理由被丢弃

### 现象

`views/admin/AuditView.vue` 的 `audit(row, approve)`：

```js
if (!approve) {
  const r = await ElMessageBox.prompt('驳回理由（选填）', '驳回启事', {...})
  await api.adminAudit(row.id, false); void r      // ← r 被直接丢掉
}
```

后端 `POST /api/admin/items/{id}/audit` 也只接受 `approve` 一个参数（`admin_log.detail` 只写 `"审核=驳回"`）。结果是：**管理员填了理由，什么也没保存**，属于误导性交互。

### 修复要求（二选一，推荐方案 A）

- **方案 A（推荐）**：后端 `auditItem` 增加可选 `reason` 参数并写入 `admin_log.detail`（如 `"审核=驳回，理由=xxx"`），前端把 `r.value` 传过去。
- **方案 B（最小改动）**：去掉这个 prompt 弹窗，改成 `ElMessageBox.confirm('确定驳回？')`，不假装收集理由。

同时：`views/admin/AuditView.vue` 的「已驳回」页签要补"重新通过"按钮，配合 `01` Prompt 的 B-03（否则已驳回的启事只能删、不能救）。

### 验收

- 驳回时填理由 → 查 `admin_log` 能看到理由落库（方案 A）；或界面不再索要理由（方案 B）。
- 对一条已驳回启事点"重新通过" → 墙页/搜图里恢复可见。

---

## F-02（P1）进入 `/profile?tab=msg` 不标记已读

### 现象

`views/ProfileView.vue`：

```js
const tab = ref(route.query.tab === 'msg' ? 'msg' : 'pub')
...
watch(tab, async (t) => {
  if (t === 'msg' && unreadCount.value > 0) { await api.readAll(); ... }
})
```

`watch` 没有 `{ immediate: true }`，所以：
- 从 NavBar 铃铛点进来（`router.push({path:'/profile', query:{tab:'msg'}})`）时 `tab` 初值就是 `'msg'`，**watcher 不会触发** → 红点不消失、消息仍标未读。

另外 `watch` 在 `loadAll()` 完成前触发时 `unreadCount` 还是 0，也会跳过。

### 修复要求

1. `watch` 加 `{ immediate: true }`。
2. 并且把"进入消息页即全部已读"放到 `loadAll()` 成功之后判断（避免数据未加载完就判断未读数）。
3. 确认 NavBar 的未读红点在返回后能刷新（它 30s 轮询一次 + `watch(route.fullPath)`，但读取的是 `api.unread()` 而不是 Pinia，所以进入消息页后需要触发一次 `pollUnread`——可用一个共享的未读 store 或在页面里 emit，最小改动是让 NavBar 的 `watch` 也监听 `route.query`）。

### 验收

浏览器实测：有未读消息时点铃铛 → 进入消息页后未读徽标立即消失、消息条目的高亮状态变已读；刷新页面后仍是已读。

---

## F-03（P1）"拖入图片"实际丢弃文件

### 现象

`components/SearchUploadCard.vue`：

```html
<div @drop.prevent="dragging = false; go()">
  ...
  <span class="try-btn">拖入图片 或 点击开始搜图 →</span>
```

首页卡片被拖入图片时只调用 `go()`（跳转 `/search-image`），**拖入的文件被完全丢弃**，用户到了搜图页还得重新选图。同样的问题也存在于"点击"路径（无文件输入）。

### 修复要求

任选其一：

- **A（推荐）**：把拖入的文件通过路由状态或 Pinia 传给 `/search-image`（例如 `router.push({ path:'/search-image', state:{ file } })` 不可行——`state` 不能传 File 对象；请改用 Pinia store 暂存 `File`，或直接在首页卡片内做上传，复用 `/api/search/image` 后展示结果）；
- **B**：改文案为"点击进入以图搜图"，去掉"拖入图片"的承诺，并移除 `@dragover/@drop` 处理。

无论哪种，都要保证**不再有"接受拖入却丢弃"的行为**。

### 验收

拖一张图到首页卡片 → 要么直接开始搜图并显示结果，要么界面明确不承诺拖入（文案与行为一致）。

---

## F-04（P2）表单长度限制不一致

`views/PublishView.vue`：

| 字段 | 前端 | 后端 `ItemForm` | `schema.sql` |
|---|---|---|---|
| 标题 | `maxlength="30"` | `@Size(max=60)` | `VARCHAR(60)` |
| 描述 | `maxlength="300"` | `@Size(max=500)` | `VARCHAR(500)` |
| 地点 | 无限制（`allow-create` 输入） | `@Size(max=50)` | `VARCHAR(50)` |
| 分类 | 无限制 | `@Size(max=20)` | `VARCHAR(20)` |

### 修复要求

把前端的 `maxlength` 与 `show-word-limit` 统一到后端的值（60 / 500 / 50 / 20），避免用户看到"还能输"却被后端 400 拒绝。同时给"地点"的 `allow-create` 加 `maxlength`。

另外：`PublishView` 的 `rules` **没有图片必填校验**，而 SRS FR2.1 要求"图片 1–3 张（必填）"，目前只在提交后由后端返回 400。请加一条前端校验（提交前判断 `fileList.length >= 1`），并顺带说明"最多 3 张"。

### 验收

在发布页粘贴超长文本 → 输入框在正确字数处截断；不选图点提交 → 立即提示"请至少上传一张图片"，不发请求。

---

## F-05（P2）删除/下架吞掉后端错误

`views/admin/AuditView.vue`：

```js
await ElMessageBox.confirm(...)
  .then(async () => { await api.adminOffline(row.id); ElMessage.success('已下架'); load() })
  .catch(() => {})          // ← 后端 403/500 被静默吞掉
```

`views/admin/UsersView.vue` 的 `toggleBan` 同样。结果是操作失败时界面既没报错也没变，用户以为成功了。

### 修复要求

区分"用户取消"与"接口失败"：

```js
try { await ElMessageBox.confirm(...) } catch { return }   // 取消
try { await api.adminOffline(row.id); ElMessage.success('已下架'); load() }
catch (e) { ElMessage.error(e.message || '操作失败') }      // 失败要提示
```

对整个 admin 目录做一次排查，把 `.catch(() => {})` 形式的静默吞错全部改掉（`ProfileView` 里读消息失败可以继续静默，那属于非关键路径，但请加注释说明）。

### 验收

停掉后端或在后端返回 403 的情况下点"下架" → 界面明确报错。

---

## F-06（P2）打包体积

构建实测输出：

```
dist/assets/DashboardView-*.js   1,129.98 kB │ gzip: 380.11 kB
dist/assets/index-*.js           1,273.67 kB │ gzip: 411.97 kB
(!) Some chunks are larger than 500 kB after minification.
```

原因：

- `views/admin/DashboardView.vue`：`import * as echarts from 'echarts'`（全量引入）
- `main.js`：`app.use(ElementPlus)` + 全量 `import 'element-plus/dist/index.css'`（369.68 kB CSS）
- `vite.config.js`：没有 `build.rollupOptions.output.manualChunks`

### 修复要求

1. ECharts 改按需引入：

```js
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([LineChart, PieChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])
```

2. Element Plus 保持全量引入可以接受（改按需要引 unplugin-vue-components 等新依赖，与 `00` 的"不引入新重型依赖"冲突），但**请把 `element-plus/dist/index.css` 与 echarts 分别拆到独立 chunk**，在 `vite.config.js` 加：

```js
build: {
  chunkSizeWarningLimit: 900,
  rollupOptions: { output: { manualChunks: { echarts: ['echarts'], 'element-plus': ['element-plus'] } } }
}
```

3. 目标：`npm run build` 不再出现 >1000 kB 的单文件（`DashboardView` 降到 500 kB 以内）。
4. 顺带检查 `views/admin/DashboardView.vue` 的图表销毁逻辑（`onBeforeUnmount` 里 `dispose()` 已有，✅）与 `window.resize` 监听清理（已有，✅）。

### 验收

重新构建并给出前后体积对比表；`DashboardView` 打开后折线图与环形图正常渲染。

---

## F-07（P2）外网图片依赖

`views/WelcomeView.vue`（3 张）与 `views/AuthView.vue`（1 张）共 4 处：

```html
<img src="https://picsum.photos/seed/lf-p1/420/320" />
<img src="https://picsum.photos/seed/shiguang-campus/1000/1400" />
```

而 `01-交付文档/01-需求规格说明书` §4 明确写"**校园内网/单机部署**"→ 断网/内网环境这两页会出现破图。

### 修复要求

1. 把图片替换为**本地资源**：放到 `frontend/src/assets/` 或 `frontend/public/`，用相对路径引用。
   - 素材可以直接复用 `backend/uploads/seed/` 里的演示图（保温杯/钥匙/耳机/学生证/雨伞/围巾），它们是本项目自有素材且已提交 git；
   - 或改用纯 CSS 渐变/插画占位，避免版权与体积问题。
2. 顺便清理 `src/mock/data.js` 里的 `https://picsum.photos/...`（见 F-09）。
3. 全局搜索确认没有残留的外部 http(s) 引用：
   ```
   grep -rn "https\?://" frontend/src --include=*.vue --include=*.js --include=*.css
   ```
   期望只剩注释、`http://localhost`（无）与 `data:` URI。

### 验收

断开网络后 `npm run dev` → 落地页与登录页图片全部正常显示。

---

## F-08（P2）编造的社交证明与不存在的功能

`views/WelcomeView.vue` 有三处站不住的内容：

| 行 | 内容 | 问题 |
|---|---|---|
| L43 | "校园账号**实名**注册，**信用分**约束爽约与冒领" | 全项目**没有任何**实名认证或信用分逻辑（后端、DB、文档都没有） |
| L91 | `★★★★★` 五星好评 | 没有评价/评分系统，属编造 |
| L112 | "已有这些院系与组织**加入**"（校团委、校图书馆、青年志愿者协会…） | 编造的机构背书 |

另外 `AuthView.vue` 的"微信登录"按钮和"忘记密码"链接是占位（`01-交付文档/07` 已声明超出范围），点击只弹"暂未接入"——建议保留但**视觉上弱化**（例如加"演示环境"角标），避免评委以为功能缺失。

### 修复要求

- 把 L43 改为只承诺真实具备的能力（"校园学号注册 + 认领凭证审核 + 站内通知"）；
- 删除 `★★★★★`，或改成真实指标（如"已帮 {{ stats.reunited }} 件失物找到主人"，数据是真实的）；
- 把"已有这些院系与组织加入"改为不承诺合作关系的表述（如"面向全校院系与组织"）；
- 这三处属于**对外展示的诚信问题**，在课程答辩里比技术缺陷更扎眼。

### 验收

逐条给出修改后的文案，并确认页面数字仍来自 `/api/stats`（真实数据）。

---

## F-09（P2）死代码与遗留 mock

`src/mock/data.js` 现在仍然包含（`项目结构.md` 却声称"仅保留常量 CATEGORIES/LOCATIONS/catEmoji"）：

- `items`（18 条假启事，含 picsum 图片 URL）
- `lostItems` / `foundItems` / `itemById` / `similarTo`
- `stats`（假统计数字：reunited 128 / lostCount 42 / foundCount 36）
- `mockSearchResults()`
- `const img = seed => https://picsum.photos/...`

实测这些导出**无人引用**（只有 `CATEGORIES`、`LOCATIONS`、`catEmoji` 被 5 个文件 import）。构建产物里还打出了 `dist/assets/data-*.js`（5.39 kB）。

`src/api/index.js` 的 `export const USE_MOCK = false` 也无人引用。

### 修复要求

1. 把 `src/mock/data.js` 精简为只剩常量，并**重命名为 `src/constants/catalog.js`**（或保留文件名但删除死代码——二选一，重命名更好，因为 `mock/` 这个目录名本身就有误导性）。同步更新 5 个 import 处：`components/ItemCard.vue`、`views/DetailView.vue`、`views/HomeView.vue`、`views/PublishView.vue`、`views/WallView.vue`。
2. 删除 `USE_MOCK`。
3. 重建后确认 `dist/assets/data-*.js` 不再产生（或变成体积很小的常量 chunk）。
4. 更新 `项目结构.md` 的对应描述。

### 验收

`npm run build` 通过；`grep -rn "mock" frontend/src` 无残留（除注释）；漏下的假数据数字（128/42/36）在仓库里搜不到。

---

## F-10（P2）管理员工具无入口

`src/api/index.js` 里定义了但**全项目无人调用**：

```js
adminRegenThumbs: () => http.post('/admin/regen-thumbs'),
adminReindex: () => http.post('/search/reindex'),
```

而 `01-交付文档/01` FR6.4 要求"`reindex`/`regen-thumbs` 管理员工具"，`01-交付文档/08` TC-管理-01 声称"后台正向流程全部可用"。

### 修复要求

在 `views/admin/` 下新增一个"运维工具"页签（可挂在 `DashboardView` 底部，或新建 `ToolsView.vue` + 路由 `/admin/tools`），放两个带二次确认的按钮：

- 「重算缺失图片指纹」→ `api.adminReindex()`，展示返回的 `reindexed` / `total`；
- 「补生成缩略图」→ `api.adminRegenThumbs()`，展示返回的 `generated`。

并在 `AdminLayout.vue` 的 `links` 里加导航项。

> 注意：这两个接口的行为在 `02` Prompt 的 B-08 里会调整（`regen-thumbs` 收敛写入范围），前端展示的文案要与之一致。

### 验收

管理员登录 → 后台能看到并成功触发两个工具，页面显示返回数字；普通用户访问该路由被 `roles: ['admin']` 守卫拦回。

---

## F-11（P2）分页与列表交互细节

1. `views/WallView.vue`：翻页（`onPage`）与筛选变化后**没有回到页面顶部**，用户翻到第 2 页时仍停在页尾；建议 `load()` 成功后 `window.scrollTo({top: 0, behavior: 'smooth'})`（仅翻页时）。
2. `views/WallView.vue` 的 `size` 固定 12，没有每页条数选择器；`el-pagination` 也没配 `page-sizes`。可选优化。
3. `views/admin/AuditView.vue` / `UsersView.vue` / `LogsView.vue`：都有 `onMounted(load)`，但搜索/筛选后不清空旧列表，会出现"报错后仍显示上一次数据"的错觉（`WallView` 已经做了 `list.value = []`，其它页面请对齐）。
4. `views/admin/AuditView.vue` 的分页 `@current-change="load"` + `v-model:current-page="page"` 是正确的，无需改；但 `changeTab` 里手动 `page.value = 1` 之后要确保只发一次请求（避免 `load` 被触发两次）。
5. `components/NavBar.vue` 的未读轮询：`watch(() => route.fullPath, pollUnread)` 会在每次路由切换时请求一次，加上 30s 定时器 → 切页频繁时请求偏多。可加 5 秒节流。

### 验收

逐条给出修改说明与浏览器实测结果（不需要自动化）。

---

## 交付要求

1. 逐条给出：改动文件 + 关键 diff 摘要 + 浏览器实测步骤与结果。
2. 最后必须跑通：`npm run build` 成功、体积对比、断网后落地页/登录页正常、管理员后台所有页面可访问。
3. 更新 `lost-found/项目结构.md` 里前端的文件清单与描述（尤其是 `mock/` 改名、新增 admin 页面、`api/index.js` 的接口列表）。
4. **不要**改 `backend/` 下任何文件（F-01 方案 A 涉及后端时，请把后端改动写进 `01`/`02` 的 Prompt 里执行）。
5. **不要**引入新的重型依赖；如需 `unplugin-*` 等构建插件，先说明理由与体积收益。
