# 拾光 · 校园失物招领平台（以图搜图）— 运行与联调说明

> 软件工程课程设计 · **单 Web 形态**：浏览器访问即用（开发态 Vite，生产态由后端直接托管 `dist`）。
> 后端 Spring Boot 3.3 + MyBatis-Plus + MySQL + JJWT + JImageHash(pHash) ｜ 前端 Vue3 + Vite + Element Plus + ECharts。
> 核心亮点：以图搜图 = 感知哈希 pHash（JImageHash）→ 64 位指纹 → MySQL `BIT_COUNT(phash ^ q)` 汉明距离（JOIN 启事表过滤待审/下架）→ 阈值过滤 → Top-N。

**当前版本（2026-10-19）**：单 Web ｜ `frontend/dist` 与源码同步 ｜ 无客户端安装包 ｜ **前端设计系统 v3 已落地**（登录前北欧编辑风 / 登录后 Digg 信息流三栏）。
历史上做过 Android APK / iOS PWA / 桌面 Electron 三端，答辩改为浏览器演示后已整体下线，
产物与手册归档在 `Creator/02-过程资料/03-迭代记录/06-收尾/archive-多端/`，详见同目录 `99-修复记录.md`。

---

## 一、目录结构

```
软件工程/                                   ← 仓库根
├── README.md                              门牌：项目简介 + 文档地图 + 快速启动
├── Creator/                               文档与过程资料（作者侧）
│   ├── CONTRIBUTING.md                git 协作规范与提交前检查单
│   ├── 01-交付文档/                           ← 课程交付物（本文件在 运行说明/ 下）
│   │   ├── 01-需求规格说明书-SRS / 02-UML建模全套 / 03-项目计划书
│   │   ├── 04-接口文档 / 05-UI设计稿 / 06-数据库设计(ER+数据字典)
│   │   ├── 07-测试计划 / 08-测试用例表 / 09-缺陷跟踪表 / 10-测试报告
│   │   ├── 11-以图搜图准确率实验报告 / 12-部署手册
│   │   └── 运行说明/                          ← 本文件所在目录（操作手册）
│   │       ├── README.md                     运行与联调总览（环境/启动/账号/排错）
│   │       ├── 00-项目知识点与结构详解-新手版.md   面向零基础组员的答辩必读
│   │       ├── 01-启动步骤.md                逐条勾选式启动步骤（工具/命令/验收/排错）
│   │       └── 02-项目结构.md                结构总览 + 模块映射 + 完成状态清单
│   │
│   └── 02-过程资料/                           过程留痕（不交作业，按迭代批次归档）
│       ├── 01-选题/  02-设计prompt/  03-迭代记录/  04-参考项目/
│       ├── 05-前端重设计效果图/        方案A~H 效果图 + 整合版功能演示（设计稿，不参与构建）
│       └── 03-迭代记录/06-收尾/archive-多端/  已下线的桌面/移动端手册与说明
│
├── assets/演示图/                         演示物品图 6 张
│
└── lost-found/                            代码仓
    ├── backend/                           Spring Boot 后端（:8080，同时托管前端）
    │   ├── pom.xml / application-local.yml   本地敏感配置（gitignore，勿提交）
    │   ├── scripts/                       gen_seed_images.py / test_api.py / eval_recall.py / backup.sh
    │   ├── uploads/                       seed/ 演示图 + _query/ 查询图 + 运行时用户图
    │   └── src/main/java/com/shiguang/lostfound/
    │       ├── controller/                Auth/Item/Search/Claim/User/Admin/File/Frontend 八控制器
    │       ├── service/                   业务层（SearchService 以图搜图、FileDeliveryService 交付、PdfService…）
    │       ├── mapper/                    MyBatis-Plus（ItemImageMapper 含汉明距离 SQL）
    │       ├── entity/ dto/ vo/           实体 / 入参 / 出参
    │       ├── config/ interceptor/ util/ context/ bootstrap/
    │       └── LostFoundApplication.java
    │
    └── frontend/                          Vue3 前端（:5173 开发 / 由后端托管生产）
        ├── index.html / vite.config.js    /api、/uploads 代理 → :8080；生产构建 chunk 策略见 F-11 注释
        ├── tests/                         vitest：路由守卫 / user store / axios 拦截器
        └── src/
            ├── api/ stores/ router/ components/ directives/ styles/ constants/
            ├── utils/                     asset.js（资源地址）delivery.js（交付文件下载）
            └── views/                     10 个用户页 + admin/ 后台 6 页
```

---

## 二、环境要求（本机已验证）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 与 Spring Boot 3.3 配套 |
| Maven | 3.9.16 | 已装于 `D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16`（含阿里云镜像） |
| MySQL | 8.0+（本机 9.7） | 连接串已加 `serverTimezone=Asia/Shanghai` |
| Node.js | 20+（本机 24） | 前端（含 echarts） |
| Python | 3.x + Pillow + requests | 跑 `backend/scripts/` 下的生成与自测脚本 |

---

## 三、启动步骤

### 0) 本地敏感配置（首次必做）
真实数据库口令 / JWT secret / 演示口令放在 **`backend/application-local.yml`**（已 gitignore）。
该文件由脚本生成或手工创建，形如：
```yaml
spring:
  datasource:
    username: root
    password: <你的MySQL口令>
jwt:
  secret: <随机64位hex>
seed:
  admin-password: <管理员演示口令，自设，勿提交 git>
  user-password: <普通演示口令，自设，勿提交 git>
```
`application.yml` 只留占位符 `${DB_PASSWORD:}` 等；默认激活 `local` profile 读取上述文件。

### 1) 数据库
```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql     # 建库建表（八表）
# 演示数据：不用手动导入。先跑 scripts/gen_seed_images.py（离线，读 uploads/_src），
# 再启动后端由 DataInitializer 播种 60 条启事/120 条图片引用（30 个去重文件）/认领三态/消息 并重算 pHash
```

### 2) 后端（:8080）
```bash
cd backend                      # ★ 固定在此目录启动（upload-dir 为相对路径）
set JAVA_HOME=<你的JDK21路径>
"%MAVEN_HOME%\bin\mvn" spring-boot:run
# 或打包后： "%MAVEN_HOME%\bin\mvn" package -DskipTests && java -jar target/lost-found-backend.jar
# 生产：加 --spring.profiles.active=prod（关闭 /doc.html 等调试入口）
```
后端同时托管前端：`/**` → `frontend/dist`（`FRONTEND_DIST_DIR` 可覆盖）+ SPA 回退，
所以**浏览器直接开 `http://localhost:8080` 就是完整站点**——答辩用这一条即可，不必开前端 dev。
> 注意：`mvn package` 前先停掉正在运行的后端，否则 jar 被占用，`repackage` 会失败（jar 变成无主清单，启动报错）。

### 3) 前端（:5173，仅开发热更时需要）
```bash
cd frontend
npm install        # 首次（含 echarts）
npm run dev        # http://localhost:5173 （/api、/uploads 已代理到 :8080）
```

### 4) 接口文档（Knife4j）
开发态打开 `http://localhost:8080/doc.html`；生产 profile 下已关闭。

---

## 四、演示账号与流程（无需手工 SQL）

| 角色 | 学号 | 口令 | 说明 |
|---|---|---|---|
| 管理员 | 10000001 | 见 `application-local.yml` 的 `seed.admin-password`（**不要写进文档/提交**） | 可直接登录进 /admin 后台 |
| 演示发布/认领人 | 20240001 ~ 20240020 | 见 `seed.user-password` | 60 条启事的发布者 |
| 自建账号 | 前端注册任意 6-12 位数字学号 | 自设 ≥6 位 | BCrypt 入库，JWT 登录 |

启动日志会打印当次生效的演示账号与口令。
- **以图搜图演示**（top-n=10、阈值 12，2026-10-15 标定，实测见 `backend/scripts/recall_results.csv` 与 Creator/01-交付文档/11 §2.4）：
  - `q-cup.jpg` → 命中 8/8 条同物启事，最低相似度 81.3%
  - `q-keys.jpg` → 8/8，最低 82.8%
  - `q-earbuds.jpg` → 10/10，最低 85.9%
  - `q-umbrella.jpg` → 10/10，最低 81.3%
  - `q-scarf.jpg` → 8/10，最低 89.1%
  - `q-card.jpg` → 4/10（卡套反光变体汉明距离偏大，属 pHash 能力边界，见 Creator/01-交付文档/11 §2.4）
  - 整体召回 48/56 = 85.7%，误检 0；单张平均耗时约 54ms。
- **管理后台**：管理员登录后左侧竖导航出现「后台管理」→ 看板（ECharts 趋势+分类环形）/ 审核 / 用户封禁⇄解封 / 操作日志 / 运维工具。
- **审核流演示**：新注册账号发布一条启事 → 个人中心显示「待审核」标签；管理员通过后才在墙页可见、可被搜图命中。
- **文件交付演示**：认领审核通过 → 失主与认领人各收到一份「认领证明 PDF」；管理端可生成统计报表 CSV、搜图页可生成匹配报告 PDF（浏览器内下载）。

---

## 五、联调/自测与备份

```bash
cd backend
python scripts/test_api.py     # M1-M6+E批+P批 全量接口测试（141 断言）；退出码非 0 即有失败
"%MAVEN_HOME%\bin\mvn" test    # 单测 99 用例（HashExperimentTest 默认 skip）
python scripts/eval_recall.py --min-recall 0.75   # 召回门禁（85.7%，误检 0）
bash scripts/backup.sh         # 数据库 + uploads 备份（见 scripts/backup.md）
```
前端：`npx vitest run`（12 用例）｜ `npm run build`（生产构建，产物进 `dist/`）。
- `test_api.py` 幂等：使用种子账号、唯一标题发布并在结尾由管理员删除清理，连跑多次八表行数回基线。
- 准确率实验结果见 `scripts/exp_results.csv`、`exp_per_transform.csv`，分析结论见 `Creator/01-交付文档/11`。

---

## 六、前端界面（设计系统 v3，2026-10-19）

界面按"登录前 / 登录后"分两套语境，共用一个原则：颜色只写在变量里，改主题只改一处。

| 项 | 内容 |
|---|---|
| 登录前（公开首页） | 北欧极简 editorial：4 屏固定舞台竖向翻页（滚轮/触摸/键盘一次一屏，第 4 屏即止）+ 关于/流程/能力/数据弹出面板（面板内独立滚动，含深棕 CTA 与页脚）+ 右侧页码；标题思源宋体、正文思源黑体、拉丁标签 Century Gothic（全系统字体） |
| 登录 / 注册页 | 左紫品牌图（紫 #7173F6→#5650E8 渐变 + 28px 栅格 + 四个纯色角色，眼睛与身体跟随鼠标）+ 右白表单；`49.4 : 50.6` 分栏，登录/注册同页切换，≤62rem 收起插图 |
| 登录后（应用） | Digg 信息流：`232px 左竖导航 / 中信息流 / 300px 右信息栏` 三栏；纸白 `#FAFAF8` + 近黑 `#141412`；左右栏各自 sticky 独立滚动，只有中栏跟随页面 |
| 设计令牌 | 应用端 `frontend/src/styles/main.css` 的 `:root`（`--cream/--paper/--ink*/--line/--st-*/--c-*`）；公开页用 `.w` 作用域内的 `--w-*`，互不污染 |
| 兼容策略 | 旧令牌名（`--cream*`、`--orange*`、`--ink*`）全部保留，值映射到 v3 色板——其余页面零改动即完成换肤；Element Plus 主色同步为 `#161614` |
| 组件 | `AppShell`（三栏 + 右栏折叠）/ `AppSidebar`（无个人中心，点头像进入）/ `FeedRail` / `FeedCard` / `ItemCard`（网格卡）/ `PosterDialog`（分享海报）/ `GradientSearchBar`（胶囊搜索条） |
| 版式 | 首页 = 工具条 + 单列信息流（最新/最热、全部/寻物/招领、列表-网格、加载更多）；失物墙 = 工具条 + 胶囊搜索 + 网格/列表；详情 = 单列白卡 + 三格事实卡 + 认领流程步骤；通知 = `/notifications`（两分栏）、聊天 = `/messages`（会话→线程） |
| 响应式 | ≤1080px 收起右栏；≤860px 收起左栏，改顶部工具条 + 抽屉；≤640px 公开页页头收为全屏菜单 |
| 动效与降级 | 卡片悬停位移/封面缩放、抽屉与弹层缓动、落地页视差；全站尊重 `prefers-reduced-motion` |
| 主题 | 左栏底部「亮色/暗色」；`html[data-theme="dark"]` + Element Plus 暗色变量；记忆在 localStorage，无记录跟系统 |
| 语言 | 左栏底部「简/繁/EN」；441 词条 × 3，覆盖全部用户端页面；分类与状态显示层映射，UGC 保持原文 |
| 覆盖边界 | 公开首页保持中文暖色品牌页；后台保持中文（图表已适配暗色） |
| 落地记录 | `Creator/02-过程资料/03-迭代记录/version08/09-v3落地记录-Digg信息流与北欧编辑风.md` |

> 改主题色只需编辑 `main.css` 的 `:root`；改完务必 `npm run build` 冒烟（F-11：dev 正常 ≠ build 正常）。

---

## 七、常见问题（风险预置对应）

| 现象 | 原因 / 处理 |
|---|---|
| MySQL 时区报错 | 连接串已含 `serverTimezone=Asia/Shanghai&useSSL=false`，勿删 |
| 跨域报错 | 前端走 Vite 代理本就是同源；生产由后端托管 dist，同源零 CORS |
| 启动报连不上数据库 | `backend/application-local.yml` 缺失或口令不对 |
| **打包/部署后页面全白（只有米色背景）** | 生产构建 chunk 循环导致入口报错（F-11，已修）；若改过 `vite.config.js` 的 `manualChunks`，务必 `npm run build` 后用浏览器控制台验证一次。**dev 正常不代表 build 正常** |
| `mvn package` 报 Unable to rename …jar | 后端还在跑，jar 被占用：先停进程再打包 |
| 以图搜图全 100% 或全空 | 检查 `ItemImageMapper` 用 `^`（按位异或）而非 `XOR`；检查 phash 是否入库（`/api/search/indexed-count`） |
| 发布后搜不到自己 | ① 需管理员审核通过后才可被搜到；② pHash 漏算可 `POST /api/search/reindex` |
| 图片不显示 | 确认 `/uploads/**` 代理与 `file.upload-dir` 指向 backend/uploads；缩略图缺失可在后台「重建缩略图」 |
| pHash 对旋转不鲁棒 | 属算法特性：演示变体只用缩放/裁剪/亮度/对比度/灰度，不用旋转（见 Creator/01-交付文档/11） |
| 8080 被占用 | 本机 TCP 动态端口曾被改成 1024 起，出站连接会抢 8080；可用 `--server.port=8081` + `BACKEND_PORT=8081` 临时绕开 |
| 想离线/异地演示 | 电脑开着后端 + `lost-found/scripts/tunnel.bat`（cloudflared），把打印出的 https 地址发给别人用浏览器打开 |
