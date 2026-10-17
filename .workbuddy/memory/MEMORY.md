# 项目长期记忆（软件工程 / 拾光·校园失物招领平台）

## 项目定位
- 软件工程课程设计：**拾光 · 校园失物招领平台（以图搜图）**，全栈可运行。
- 核心亮点：JImageHash pHash → 64 位指纹 → MySQL `BIT_COUNT(phash ^ q)` 汉明距离（JOIN item 过滤待审/下架）→ 阈值(12)过滤 → Top-N(10)。
- 实测召回 48/56=85.7%，误检 0，单张约 54ms（docs/11）。

## 技术栈与端口
- 后端：Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + JJWT 0.12.6 + Knife4j + OpenPDF，:8080，JDK21 / Maven 3.9.16（`D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16`）。
- 前端：Vue3.5 + Vite6 + Pinia + Element Plus + ECharts6，:5173，`/api`、`/uploads` 代理到 8080。
- 数据库：MySQL 8+/9.x，库名 `lost_found`，八表（user/item/item_image/claim_record/message/admin_log/favorite/delivery）。
- 三端同源码：Web / Android（Capacitor 7 套壳）/ iOS（PWA），桌面 Electron 壳（`lost-found/desktop`）。
- 公网分发：花生壳免费穿透 `https://131kt2646vt74.vicp.fun` → 127.0.0.1:8080，后端同时托管 frontend/dist。

## 目录约定
- `lost-found/`：可运行工程（backend / frontend / desktop / docs 01-12 / 项目运行说明）。
- `Prompt/`：选题、前端页面 prompt、落地项目实现方案、分版本修改建议（version_01~06，各含修复记录）。
- 关键文档：`lost-found/项目运行说明/README.md`（运行）、`项目结构.md`（模块映射+完成状态）、`桌面端运行说明.md`、`移动端运行说明.md`；桌面端总纲在 `Prompt/落地项目实现/失物招领平台-桌面端完整实施方案.md`。

## 工程惯例
- 敏感配置在 `backend/application-local.yml`（gitignored），application.yml 只留占位符。
- **禁止把 element-plus 按组件首字母拆成两个以上 chunk**：组件间双向依赖会跨 chunk 成环，生产构建直接 TDZ 白屏（F-11，2026-10-17）。现在 element-plus 单 chunk（~974kB）/ echarts 独立 / 其余 vendor。
- 三端资源路径统一走 `assetUrl()`：`IS_DESKTOP || IS_NATIVE` 时拼 `API_ORIGIN`，Web/PWA 用相对路径；hash 路由判断同样用 `IS_DESKTOP || IS_NATIVE`。
- mobile（Capacitor）构建不注入 PWA/SW；Web/PWA 才需要 SW。
- 自测：`backend/scripts/test_api.py`（141 断言，幂等，八表回基线）+ `mvn test`（99 用例）+ `eval_recall.py --min-recall 0.75`。
- 本地特有坑：8080 曾被动态端口占用（改 dynamicport start=49152）；Electron 需 ELECTRON_MIRROR 镜像；中文路径下 Android 构建走 ASCII junction。
- git 由组员提交，未提交改动较多（47 修改 + 新模块 desktop/android/delivery 等）。
