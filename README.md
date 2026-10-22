# 拾光 · 校园失物招领平台（以图搜图）

> 软件工程课程设计 · **Web 为主**：浏览器直接访问（后端同源托管前端），答辩与部署均以 Web 版为准；另提供 Windows 独立安装包（`App-LostFond`，可选）。

上传一张物品照片，系统在全部已审核启事里找出"同一件东西"的其它照片，不依赖文字描述，也不要求用户手写标签。

> **Windows 安装包**：发布在 [GitHub Releases](https://github.com/unicornDu/lost-fond/releases)。本地构建产物在 `App-LostFond/Softeare/`（安装程序 + 使用说明），因体积大不提交仓库；构建方式见 `App-LostFond/README.md`。该目录独立于 Web 版构建，不影响 Web 版代码与部署。

> 当前迭代：**v2.51**（2026-10-21）——公开首页 02–04 屏标题垂直居中、首页活动天际线月份标签不再被裁切且颜色加深。记录见 `Creator/过程资料/迭代记录/version-2.50/v2.51-MarkDone-首页入口屏标题垂直居中.md`。

```
查询图 → pHash（JImageHash）→ 64 位指纹 → MySQL BIT_COUNT(phash ^ q) 汉明距离
      → JOIN 启事表过滤待审/下架 → 阈值(12)过滤 → 按相似度取 Top-10
```

实测 56 个查询变体：召回 **48/56 = 85.7%**，误检 **0**，单张平均约 **54 ms**。

**本文只讲一件事：怎么从零把这套系统部署到自己电脑上跑起来。** 设计文档、迭代记录、答辩材料都在 `Creator/` 下，见文末导航。

---

## 一、技术栈

| 层 | 技术 | 端口 |
|---|---|---|
| 后端 | Spring Boot 3.3.5 · MyBatis-Plus 3.5.7 · JJWT 0.12.6 · JImageHash 1.0.0 · Knife4j · OpenPDF | `:8080` |
| 前端 | Vue 3.5 · Vite 6 · Pinia · Element Plus · ECharts 6 · **设计系统 v3（登录前：北欧编辑风；登录后：Digg 信息流三栏）** | `:5173` |
| 数据库 | MySQL 8+（库名 `lost_found`，八表：user / item / item_image / claim_record / message / admin_log / favorite / delivery） | `:3306` |
| 部署 | 单 Web：后端托管 `frontend/dist`，浏览器访问 :8080；可选 cloudflared 临时公网演示 | — |
| 构建 | JDK 21 · Maven 3.9+ · Node 20+ | — |

规模：51 个 REST 端点（另有 2 条 SPA 回退路由）· 141 条接口断言 · 112 个后端单测 · 31 个前端单测（7 个文件）· 32 条缺陷全闭环。

---

## 二、部署前准备

只需要四样通用工具，装完确保在 PATH 里：

| 依赖 | 版本 | 用途 | 自检命令 |
|---|---|---|---|
| JDK | **21** | 编译/运行后端 | `java -version` |
| Maven | 3.9+ | 构建后端 | `mvn -v` |
| MySQL | 8.0+ | 数据存储 | `mysql --version` |
| Node.js | 20+ | 构建/运行前端 | `node -v` |

四条命令都能打印出版本号，再往下走。MySQL 服务要处于运行状态（Windows：`net start MySQL`；macOS/Linux：`brew services start mysql` 或 `systemctl start mysql`）。

---

## 三、从零部署（五步）

### 第 1 步：克隆仓库

```bash
git clone <仓库地址>
cd 软件工程
```

### 第 2 步：建库建表（二选一）

**A. 首次初始化 / 已有库补表（推荐，可重复执行、不清数据）：**

```bash
mysql -u root -p < web-lostfond/backend/src/main/resources/db/schema-init.sql
```

**B. 彻底重置（⚠️ 会 DROP 全部 8 张表并清空数据，仅开发环境使用）：**

```bash
mysql -u root -p < web-lostfond/backend/src/main/resources/db/schema.sql
```

> 2026-10-18 修复：`schema.sql` 每张表前都有 `DROP TABLE IF EXISTS`，**执行即清库**；日常请用 `schema-init.sql`（`CREATE TABLE IF NOT EXISTS`）。已有数据升级前先跑 `backend/scripts/backup.sh` 备份。

验证：

```bash
mysql -u root -p -e "USE lost_found; SHOW TABLES;"
# 应看到 8 张表
```

### 第 3 步：生成本地配置

```bash
cd web-lostfond/backend
cp application-local.yml.example application-local.yml
# Windows cmd 用：copy application-local.yml.example application-local.yml
```

打开 `application-local.yml`，只需要改两处 `CHANGE_ME`：

| 字段 | 填什么 |
|---|---|
| `spring.datasource.password` | 你本机 MySQL 的 root 口令 |
| `jwt.secret` | 任意 32 位以上的随机字符串（`openssl rand -base64 48` 可生成） |

`seed.admin-password` / `seed.user-password` 已预填 `admin123` / `user123`，不改也能直接登录。

> 这个文件被 `.gitignore` 排除，不会进版本库。**缺了它会启动失败并明确报错**（不会静默 fallback 到空口令）。

### 第 4 步：启动后端（窗口 1）

```bash
cd web-lostfond/backend
mvn spring-boot:run
```

日志出现 `Started LostFoundApplication in x.x seconds` 即成功，**窗口保持开着**。

两个要点：

- **必须在 `web-lostfond/backend` 目录下启动** —— 上传目录 `./uploads` 是相对工作目录的路径，换目录会另建空目录导致图片 404；
- **首次启动自动播种** 60 条演示启事、120 条图片引用、认领与消息记录，并重算 pHash，不用手工导 SQL 数据。

想打成 jar 长期跑：

```bash
mvn clean package -DskipTests
java -jar target/lost-found-backend.jar
```

### 第 5 步：启动前端（窗口 2）

```bash
cd web-lostfond/frontend
npm install        # 仅第一次
npm run dev
```

看到 `Local: http://localhost:5173/` 即成功。浏览器打开 **http://localhost:5173**。

**只想开一个进程？** 先 `npm run build` 生成 `dist/`，然后只开后端，直接访问 **http://localhost:8080** —— 后端会托管 `dist`，是完整站点（只是没有热更新）。

---

## 四、验收

```bash
curl "http://localhost:8080/api/items?page=1&size=1"    # 返回 {"code":200,...} 即接口通
```

浏览器逐项点一遍：

- [ ] `http://localhost:5173` 落地页有内容、有图（图来自 `web-lostfond/backend/uploads/`）
- [ ] 登录 管理员 `10000001`（口令 = `application-local.yml` 的 `seed.admin-password`，模板默认 `admin123`），普通用户 `20240001`~`20240020`（`seed.user-password`，模板默认 `user123`）——**以你的本地配置为准，改过就不是默认值**
- [ ] 失物墙有真实数据（60 条启事）
- [ ] 以图搜图：上传 `web-lostfond/backend/uploads/_query/q-cup.jpg` → 命中该物品多条启事，相似度 ~100%
- [ ] 导航栏出现「后台管理」→ 看板 / 审核 / 用户 / 日志 / 运维 五个页面能打开
- [ ] **通知 / 聊天是独立页面**：左侧导航进入 `/notifications`、`/messages`；通知页可逐条已读或一键「全部标为已读」，聊天页从会话列表点进线程可直接回复（回车发送）
- [ ] **个人中心**：`我的发布 / 我的收藏 / 我的认领 / 收到 / 账号` 五个标签可用；「收到」里可审核认领申请，`账号` 里可改昵称、头像、手机号与密码
- [ ] **认领进度时间线**：用普通账号对一条招领提交认领（先填凭证 → 再填联系方式两步提交）后，详情页出现四节点进度卡（提交申请 → 发布者审核 → 交付确认 → 已找回），连线会从起点生长到当前进度
- [ ] **分享海报**：任意启事详情页点「分享海报」→ 弹层出现海报，点「保存图片」能下载 PNG（含二维码图案）
- [ ] **滚动进场**：首页/失物墙/搜图页向下滚动，各区块与卡片网格依次淡入上浮；导航栏滚离顶部后出现阴影

自动化自测（可选，需要 Python 3 + `pip install pillow requests`）：

```bash
cd web-lostfond/backend
python scripts/test_api.py                        # 141 断言，幂等，跑完八表回基线
python scripts/eval_recall.py --min-recall 0.75   # 召回门禁 85.7%
mvn test                                          # 112 用例
```

---

## 五、手机 / 公网访问（可选）

后端 `:8080` 已经同时托管 `frontend/dist`，手机访问只需要这一个端口，不需要另外开 Vite。

> 通过 `:8080` 看到的是构建产物：改过前端代码后，先在 `web-lostfond/frontend` 执行 `npm run build`，手机刷新才是新版本。

| 场景 | 手机打开 | 前提 |
|---|---|---|
| 同一 Wi-Fi / 热点 | `http://<电脑局域网 IP>:8080/` | 同一网络、电脑不休眠、后端在跑 |
| 任意网络（手机流量） | `https://xxx.trycloudflare.com` | 运行 Cloudflare 临时隧道 |

### 方式 A：同一 Wi-Fi / 热点

1. 电脑和手机连接同一个 Wi-Fi 或热点；
2. 电脑执行 `ipconfig`，取当前网卡的 IPv4 地址（例如 `172.20.10.2`，以实际为准；
   `172.26.16.1` 是 WSL 虚拟网卡，`169.254.x.x` 是未连接网卡，不要用）；
3. 手机浏览器打开 `http://<IPv4>:8080/`。

打不开时依次检查：后端窗口是否还在运行、Windows 防火墙是否放行 Java/8080、热点是否开了「AP 隔离/访客隔离」、电脑是否休眠。需要新增放行规则时（管理员权限）：

```bat
netsh advfirewall firewall add rule name="拾光 8080" dir=in action=allow protocol=TCP localport=8080
```

> 局域网走的是 HTTP，不是安全上下文，「使用当前位置」定位和剪贴板 API 可能不可用；需要这些能力请用方式 B 的 HTTPS 地址。

### 方式 B：Cloudflare 临时隧道

仓库已备好 `Project_operation_software/cloudflared/cloudflared.exe`，直接双击 `web-lostfond/scripts/tunnel.bat` 即可：

```text
[1/2] 检查后端 8080 → [2/2] 申请地址 → 打印 https://xxx-yyy-zzz.trycloudflare.com
```

把打印出的 HTTPS 地址发给对方，手机流量也能访问。首次运行如遇 SmartScreen，点「更多信息 → 仍要运行」。

**关闭**：在隧道窗口按 `Ctrl+C` 或直接关闭窗口；只最小化不算关闭。关闭后旧地址再打开会返回 530，可用 `tasklist | findstr cloudflared` 确认没有残留进程。

注意：

- **免账号隧道的域名每次启动都会变**；电脑休眠 / 关窗口 / 后端停止，地址立即失效；
- 链接**任何拿到的人都能打开**，包含演示数据与注册登录，只发给需要演示的人；
- 国内访问 `trycloudflare.com` 偶尔不稳，重开一次即可；
- `tunnel.bat` 必须保持 **GBK/ANSI + CRLF**，用 UTF-8 或 LF 重新保存会导致双击闪退。

> 完整的开启、关闭、排错与验证记录见 `web-lostfond/scripts/MarkDone-公网访问开启与关闭.md`。

---

## 六、常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动报连不上数据库 | `application-local.yml` 没生成或口令不对；或 MySQL 服务没起 |
| 端口 8080 / 5173 被占用 | `netstat -ano \| findstr :8080` 找 PID 结束进程，或改 `application.yml` / `vite.config.js` 的端口 |
| 图片全部 404 | 没有在 `web-lostfond/backend` 目录下启动后端（`uploads` 是相对路径） |
| 前端页面白屏（生产构建） | 改过 `vite.config.js` 的 `manualChunks` 后必须 `npm run build` 冒烟验证，dev 正常 ≠ build 正常 |
| 搜图结果全 100% 或全空 | pHash 未算，`POST /api/search/reindex` 重算 |
| 401 / 403 | token 过期、账号被封禁或权限不足，重新登录 |
| 双击 `tunnel.bat` 窗口一闪而过 | 文件被保存成了 UTF-8/LF；必须用 GBK(ANSI) + CRLF，详见 `web-lostfond/scripts/MarkDone-公网访问开启与关闭.md` |
| 手机打不开局域网地址 | 确认手机与电脑同一网络、后端在 `0.0.0.0:8080`、防火墙放行、热点未开 AP 隔离、电脑未休眠 |
| 手机页面部分内容显示不全 | 后台表格在窄屏保留横向滚动，可左右滑动查看其余列；个人中心标签在窄屏用左右箭头切换 |
| `npm install` 卡住 | 换镜像 `npm config set registry https://registry.npmmirror.com` |

---

## 七、目录地图

```
软件工程/
├── README.md                        ← 本文件（部署指南）
│
├── Creator/                         文档与过程资料
│   ├── README.md                    本目录说明
│   ├── CONTRIBUTING.md              git 协作规范与提交前检查单
│   ├── 交付文档/                    课程交付物：01-12 全套
│   │   └── 运行说明/                环境、启动、项目结构、新手版
│   ├── 过程资料/                    过程留痕（不交作业）
│   │   ├── 选题/                    选题论证、五人分工、准备清单
│   │   ├── 设计prompt/              页面设计 prompt（含已归档的多端方案）
│   │   ├── 迭代记录/                version-0~2.40/（历史 8 批）+ version-2.10 / 2.20 / 2.41 / 2.50（当前 v2.51）
│   │   └── 参考项目/                三个上游开源项目离线副本
│   └── Knowledge_List/              答辩复习知识清单（8 份 · 208 个自测点；`多端与工程化` 已随单 Web 收敛归档）
│
├── Project_operation_software/      运行期软件（二进制走 Releases，仓库只留台账）
├── assets/演示图/                   演示物品图 6 张
│
├── web-lostfond/                     Web 版代码仓（答辩版，保持原样）
│   ├── backend/                     Spring Boot（:8080）
│   ├── frontend/                    Vue3（:5173）
│   └── scripts/                     tunnel.bat、公网访问开启与关闭 MarkDone
│
└── App-LostFond/                    Windows 桌面版（独立构建）
    ├── backend/                     Spring Boot + H2（独立版 profile）
    ├── frontend/                    Vue3（Electron 共用）
    ├── desktop/                     Electron 壳 + 内置 JRE
    ├── build/                       图标生成脚本、构建产物
    └── Softeare/                    安装包（走 Releases，不入库）、SHA256SUMS、使用说明
```

`*.keystore`、`application-local.yml`、`node_modules/`、`dist/` 等本机产物已被 `.gitignore` 排除，**不在仓库里**。

---

## 八、演示账号与核心流程

| 角色 | 学号 | 口令 |
|---|---|---|
| 管理员 | `10000001` | `application-local.yml` 的 `seed.admin-password`（模板默认 `admin123`） |
| 演示发布/认领人 | `20240001` ~ `20240020` | `seed.user-password`（模板默认 `user123`） |

启动日志会打印当次生效的账号。三条主流程：

1. **发布 → 审核 → 可见**：新用户发布启事显示"待审核"，管理员通过后才上墙、才能被搜图命中。
2. **以图搜图**：上传 `q-cup.jpg` 等查询图 → 返回 Top-10 及相似度百分比（`q-card.jpg` 因卡套反光属 pHash 能力边界）。
3. **认领 → 交付**：认领审核通过后自动生成认领证明 PDF，双方在消息中心下载（浏览器内下载）。

---

## 九、前端设计系统 v3（2026-10-19 落地）

界面按"登录前 / 登录后"分成两个语境：

- **公开首页（登录前）**：北欧极简 editorial。4 屏固定舞台竖向翻页（滚轮/触摸/键盘一次一屏，第 4 屏即止、页面不产生滚动条），
  关于 / 流程 / 能力 / 数据改为导航点击后弹出的整页面板（面板内独立滚动，末尾是深棕 `#594037` CTA 与页脚）。
  标题用思源宋体（500 字重）、正文思源黑体、拉丁小标签 Century Gothic——全部系统字体，无外链。
- **登录 / 注册页**：左紫品牌图（四个纯色角色，眼睛与身体跟随鼠标）+ 右白表单，`49.4 : 50.6` 分栏；登录/注册同页切换。
- **登录后应用**：Digg 信息流。`232px 左竖导航 / 中信息流 / 300px 右信息栏` 三栏，
  纸白 `#FAFAF8` + 近黑 `#141412`，全站唯一彩色是六个分类小圆点；左右两栏各自 sticky 独立滚动，
  只有中栏跟随页面滚动。

| 项 | 说明 |
|---|---|
| 换肤方式 | 颜色全部集中在 `frontend/src/styles/main.css` 的 `:root`；**旧令牌名保留并映射新色值**（`--orange*` → 近黑），改一处即可全站换肤。公开页用自己的 `--w-*` 作用域，互不污染 |
| 骨架 | `AppShell`（三栏 + 右栏折叠 + 移动顶栏）/ `AppSidebar`（竖导航、失物/招领分高亮、未读角标、发布 CTA；不含个人中心，点头像进入）/ `FeedRail`（pHash 推广、找回战报、热门启事、为你推荐） |
| 卡片 | `FeedCard`（分类点 + 状态淡彩 pill + 2.35:1 图 + 地点 pill + meta 行）/ `ItemCard`（1.45:1 网格卡） |
| 页面 | 首页 = **信息总览（活动天际线，2D 热力图 ⇄ 3D）** + 工具条 + 单列信息流（最新/最热、全部/寻物/招领、列表-网格、加载更多）；失物墙 = 工具条 + 胶囊搜索 + 网格/列表；详情 = 白卡 + 三格事实卡 + **位置展开卡（v2.41.1 真实坐标 + 高德底图）** + 认领流程步骤；公开首页 = 固定舞台 4 屏翻页 + 信息弹层；登录页 = 左紫插图 + 右表单；通知 / 聊天 = 独立页；守望订阅 = 三档方案卡 + 月/学期切换 + 我的守望位；v2.51 起入口屏 02–04 标题垂直居中，活动天际线月份标签完整显示 |
| 响应式 | ≤1080px 收右栏；≤860px 收左栏改顶栏 + 抽屉；≤640px 首页头收为全屏菜单 |
| 动效 | 卡片 hover 位移/封面缩放、抽屉与弹层 `cubic-bezier(.22,1,.36,1)`、落地页 expo-out 视差；全部尊重 `prefers-reduced-motion` |
| 主题 | 亮/暗双主题（`composables/useTheme.js` + `html[data-theme="dark"]` 令牌 + EP 暗色变量），记忆与跟随系统 |
| 语言 | 简/繁/EN（`i18n/index.js`，539 词条 × 3），左栏切换，标题与 EP 内置文案同步 |
| 已退役 | v2「方案G 编辑叙事」（翡翠绿 + 顶部横向导航）与 v1「暖光手账风」；旧组件 `NavBar` / `GooeyNav` / `SplitText` 已删除，可在 git 历史找回 |
| 落地记录 | `Creator/过程资料/迭代记录/version-0~2.40/version08/v3落地记录-Digg信息流与北欧编辑风.md`；设计规范见 `Creator/交付文档/05-UI设计稿与规范-v1.0.md` 第 0 节 |

**主题与语言**：左栏底部设置区提供「亮色/暗色」与「简/繁/EN」两个控件。

- 暗色主题：`html[data-theme="dark"]` 覆盖全部应用令牌 + Element Plus 官方暗色变量；
  记忆在 `localStorage.shiguang_theme`，无记录跟随系统；`index.html` 预置主题避免首屏闪白。
  公开首页与后台保持自身配色（前台是暖色品牌页，后台图表已适配暗色）。
- 多语言：自研 ~90 行 i18n（未装 vue-i18n），**539 词条 × 3 语言**，覆盖导航、首页/失物墙/详情（含位置卡）/发布/
  以图搜图/通知/聊天/守望订阅/个人中心/登录注册/404/海报；分类与状态在显示层映射，用户发布的标题与描述保持原文。
  浏览器标题随语言切换，Element Plus 分页/空态文案由 `el-config-provider` 跟随。

> 公开首页 4 张轮播图已压到 1600px（`assets/img/01-1600.jpg`…，合计约 0.76 MB，原图保留为母版），`dist` 约 3.2 MB。
