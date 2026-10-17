# 拾光 · 校园失物招领平台（以图搜图）— 运行与联调说明

> 软件工程课程设计 · 一套代码四端运行（Web / Android APK / iOS PWA / 桌面 Electron）。
> 后端 Spring Boot 3.3 + MyBatis-Plus + MySQL + JJWT + JImageHash(pHash) ｜ 前端 Vue3 + Vite + Element Plus + ECharts。
> 核心亮点：以图搜图 = 感知哈希 pHash（JImageHash）→ 64 位指纹 → MySQL `BIT_COUNT(phash ^ q)` 汉明距离（JOIN 启事表过滤待审/下架）→ 阈值过滤 → Top-N。

**当前版本（2026-10-17）**：Web / PWA 与 `frontend/dist` 同步 ｜ Android **1.0.2（versionCode 3）** ｜ 桌面 **1.0.1**。
本次更新：修复生产构建白屏（F-11/F-12）、原生图片路径（F-13）、状态栏重叠与平板断点（F-14），
详见 `Prompt/修改建议_prompt/version_06/99-修复记录.md`。

---

## 一、目录结构

```
lost-found/
├── docs/                              交付文档全套 01-12
│   ├── 01-需求规格说明书-SRS / 02-UML建模全套 / 03-项目计划书
│   ├── 04-接口文档 / 05-UI设计稿 / 06-数据库设计(ER+数据字典)
│   └── 07-测试计划 / 08-测试用例表 / 09-缺陷跟踪表 / 10-测试报告
│       11-以图搜图准确率实验报告 / 12-部署手册
│
├── 项目运行说明/                       ← 本文件所在目录（操作手册）
│   ├── README.md                      运行与联调总览（环境/启动/账号/排错）
│   ├── 项目结构.md                     结构总览 + 模块映射 + 完成状态清单
│   ├── 启动步骤-MarkDone.md            逐条勾选式启动步骤（工具/命令/验收/排错）
│   ├── 桌面端运行说明.md               Electron 壳：开发/打包/局域网/公网分发
│   ├── 移动端运行说明.md               Android APK 构建签名分发 + iOS PWA
│   └── 00-项目知识点与结构详解-新手版.md  面向零基础组员的答辩必读
│
├── backend/                           Spring Boot 后端（:8080）
│   ├── pom.xml / application-local.yml  本地敏感配置（gitignore，勿提交）
│   ├── scripts/                       gen_seed_images.py / test_api.py / eval_recall.py / backup.sh
│   ├── uploads/                       seed/ 演示图 + _query/ 查询图 + downloads/ APK + 运行时用户图
│   └── src/main/java/com/shiguang/lostfound/
│       ├── controller/                Auth/Item/Search/Claim/User/Admin/File/Frontend 八控制器
│       ├── service/                   业务层（SearchService 以图搜图、FileDeliveryService 交付、PdfService…）
│       ├── mapper/                    MyBatis-Plus（ItemImageMapper 含汉明距离 SQL）
│       ├── entity/ dto/ vo/           实体 / 入参 / 出参
│       ├── config/ interceptor/ util/ context/ bootstrap/
│       └── LostFoundApplication.java
│
├── frontend/                          Vue3 前端（:5173）
│   ├── index.html / vite.config.js    /api、/uploads 代理 → :8080；生产构建 chunk 策略见 F-11 注释
│   ├── vite.desktop.config.js         桌面专用构建（base=./ + 禁用 manualChunks）
│   ├── capacitor.config.json          Android 套壳配置（含 edge-to-edge 边距，F-14）
│   ├── android/                       Capacitor 原生工程（签名/图标/明文白名单）
│   ├── .env.mobile                    移动端后端地址（构建时烙入）
│   └── src/
│       ├── api/ stores/ router/ components/ directives/ styles/ constants/
│       ├── utils/                     desktop.js(多端适配) token-storage.js native-camera.js delivery.js
│       └── views/                    10 个用户页 + admin/ 后台 6 页
│
└── desktop/                           Electron 壳（:无端口，加载 renderer + 探活后端）
    ├── main.cjs / preload.cjs         主进程与安全白名单
    ├── prepare-renderer.js            打包前置：桌面配置构建 → renderer/
    ├── default-config.json            发行默认后端地址（当前=花生壳公网域名）
    └── release2/                      安装包输出：ShiGuang Setup 1.0.1.exe
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
| Android SDK | platform-35 / build-tools 35 | 仅重打 APK 需要，已装在 `lost-found/_android-sdk/` |

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
所以浏览器直接开 `http://localhost:8080` 也能用整套站点（公网穿透同理）。

### 3) 前端（:5173，开发热更）
```bash
cd frontend
npm install        # 首次（含 echarts）
npm run dev        # http://localhost:5173 （/api、/uploads 已代理到 :8080）
```

### 4) 桌面端（Electron）
```bash
# 开发模式（三个终端：后端 / 前端 dev / 壳）
cd desktop && ELECTRON_RENDERER_URL=http://localhost:5173 npm run dev
# 打包安装包
cd desktop && npm run dist          # → release2/ShiGuang Setup 1.0.1.exe
```
详见 **桌面端运行说明.md**（含局域网/公网分发、配置优先级、踩坑）。

### 5) 移动端（Android APK / iPhone PWA）
```bash
cd frontend
npx vite build --mode mobile --outDir dist --emptyOutDir   # 烙入 .env.mobile 的后端地址
npx cap copy android
cd android && gradlew.bat assembleRelease                  # 中文路径已用 android.overridePathCheck=true 放行
# 产物：android/app/build/outputs/apk/release/app-release.apk
# 分发：复制为 backend/uploads/downloads/app-release.apk → 官网「📱 下载 Android 版」
```
详见 **移动端运行说明.md**（签名、明文白名单、真机验收、iOS 添加主屏幕）。

### 6) 接口文档（Knife4j）
开发态打开 `http://localhost:8080/doc.html`；生产 profile 下已关闭。

---

## 四、演示账号与流程（无需手工 SQL）

| 角色 | 学号 | 口令 | 说明 |
|---|---|---|---|
| 管理员 | 10000001 | 见 `application-local.yml` 的 `seed.admin-password`（**不要写进文档/提交**） | 可直接登录进 /admin 后台 |
| 演示发布/认领人 | 20240001 ~ 20240020 | 见 `seed.user-password` | 60 条启事的发布者 |
| 自建账号 | 前端注册任意 6-12 位数字学号 | 自设 ≥6 位 | BCrypt 入库，JWT 登录 |

启动日志会打印当次生效的演示账号与口令。
- **以图搜图演示**（top-n=10、阈值 12，2026-10-15 标定，实测见 `backend/scripts/recall_results.csv` 与 docs/11 §2.4）：
  - `q-cup.jpg` → 命中 8/8 条同物启事，最低相似度 81.3%
  - `q-keys.jpg` → 8/8，最低 82.8%
  - `q-earbuds.jpg` → 10/10，最低 85.9%
  - `q-umbrella.jpg` → 10/10，最低 81.3%
  - `q-scarf.jpg` → 8/10，最低 89.1%
  - `q-card.jpg` → 4/10（卡套反光变体汉明距离偏大，属 pHash 能力边界，见 docs/11 §2.4）
  - 整体召回 48/56 = 85.7%，误检 0；单张平均耗时约 54ms。
- **管理后台**：管理员登录后 NavBar 出现「🛡️ 后台管理」→ 看板（ECharts 趋势+分类环形）/ 审核 / 用户封禁⇄解封 / 操作日志 / 运维工具。
- **审核流演示**：新注册账号发布一条启事 → 个人中心显示「待审核」标签；管理员通过后才在墙页可见、可被搜图命中。
- **文件交付演示**：认领审核通过 → 失主与认领人各收到一份「认领证明 PDF」；管理端可生成统计报表 CSV、搜图页可生成匹配报告 PDF（桌面端点击直接原生打开）。

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
- 准确率实验结果见 `scripts/exp_results.csv`、`exp_per_transform.csv`，分析结论见 `docs/11`。

---

## 六、常见问题（风险预置对应）

| 现象 | 原因 / 处理 |
|---|---|
| MySQL 时区报错 | 连接串已含 `serverTimezone=Asia/Shanghai&useSSL=false`，勿删 |
| 跨域报错 | 前端走 Vite 代理本就是同源；CorsConfig 只放行显式来源（含 `null` 供桌面 file://、`https://localhost` 供 Android WebView） |
| 启动报连不上数据库 | `backend/application-local.yml` 缺失或口令不对 |
| **打包/部署后页面全白（只有米色背景）** | 生产构建 chunk 循环导致入口报错（F-11，已修）；若改过 `vite.config.js` 的 `manualChunks`，务必 `npm run build` 后用浏览器控制台验证一次。**dev 正常不代表 build 正常** |
| **手机 App 顶部压状态栏** | Android 15 强制 edge-to-edge；已用 `--safe-top` + `adjustMarginsForEdgeToEdge:"force"` 修复（F-14）；旧包需装 1.0.2 |
| **覆盖安装 APK 后仍白屏** | 1.0.0 留下的 Service Worker 在回放旧缓存：先卸载再装；1.0.1 起 APK 不含 SW，可正常覆盖 |
| 以图搜图全 100% 或全空 | 检查 `ItemImageMapper` 用 `^`（按位异或）而非 `XOR`；检查 phash 是否入库（`/api/search/indexed-count`） |
| 发布后搜不到自己 | ① 需管理员审核通过后才可被搜到；② pHash 漏算可 `POST /api/search/reindex` |
| 图片不显示 | 确认 `/uploads/**` 代理与 `file.upload-dir` 指向 backend/uploads；缩略图缺失可在后台「重建缩略图」 |
| pHash 对旋转不鲁棒 | 属算法特性：演示变体只用缩放/裁剪/亮度/对比度/灰度，不用旋转（见 docs/11） |
| 8080 被占用 | 本机 TCP 动态端口曾被改成 1024 起，出站连接会抢 8080；可用 `--server.port=8081` + `BACKEND_PORT=8081` 临时绕开 |
