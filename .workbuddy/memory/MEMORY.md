# 项目长期记忆（软件工程 / 拾光·校园失物招领平台）

## 项目定位
- 软件工程课程设计：**拾光 · 校园失物招领平台（以图搜图）**，全栈可运行。
- 核心亮点：JImageHash pHash → 64 位指纹 → MySQL `BIT_COUNT(phash ^ q)` 汉明距离（JOIN item 过滤待审/下架）→ 阈值(12)过滤 → Top-N(10)。
- 实测召回 48/56=85.7%，误检 0，单张约 54ms（`01-交付文档/11-以图搜图准确率实验报告-v1.0.md` §2.4）。

## 技术栈与端口
- 后端：Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + JJWT 0.12.6 + Knife4j + OpenPDF，:8080，JDK21 / Maven 3.9.16（`D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16`）。
- 前端：Vue3.5 + Vite6 + Pinia + Element Plus + ECharts6，:5173，`/api`、`/uploads` 代理到 8080。
- 数据库：MySQL 8+/9.x，库名 `lost_found`，八表（user/item/item_image/claim_record/message/admin_log/favorite/delivery）。
- 三端同源码：Web / Android（Capacitor 7 套壳）/ iOS（PWA），桌面 Electron 壳（`lost-found/desktop`）。
- 公网分发：花生壳免费穿透 `https://131kt2646vt74.vicp.fun` → 127.0.0.1:8080，后端同时托管 frontend/dist。

## 目录约定（2026-10-17 完成整理，四层分离）
- 仓库根：`README.md`（门牌+文档地图）、`CONTRIBUTING.md`（git 规范）、`01-交付文档/`、`02-过程资料/`、`assets/演示图/`、`lost-found/`、`文档整理方案-v1.0.md`（整理过程存档）。
- `01-交付文档/`：**唯一交付物目录**。01-12 课程文档 + `运行说明/`（README / 00-新手版 / 01-启动步骤 / 02-项目结构 / 03-桌面端 / 04-移动端）+ `00-文档总览.md`。
- `02-过程资料/`：`01-选题/`（含 archive/3 篇被合并版取代）、`02-设计prompt/{前端,落地实现}/`、`03-迭代记录/{01-全量审查建议,02-首轮修复记录,03-P0P1P2修复Prompt,04-Z批收口,05-E批功能增量,06-收尾}/`、`04-参考项目/`（JImageHash、vue-element-admin、314loveshiwu，已剔 .class/.DS_Store/target）。
- 索引台账：`02-过程资料/00-索引.md`（有效版/归档标注）、`01-交付文档/00-文档总览.md`（导读+基线数据+按角色阅读顺序）。
- `lost-found/` 只留代码（backend/frontend/desktop），**已无 docs/ 与 项目运行说明/**；文档内指向彼此的路径均为仓库根相对写法。
- 归档约定：某文档被取代 → 移入同级 `archive/`，不删不改名，并在索引里改状态。

## 工程惯例
- 敏感配置在 `backend/application-local.yml`（gitignored），application.yml 只留占位符。
- **禁止把 element-plus 按组件首字母拆成两个以上 chunk**：组件间双向依赖会跨 chunk 成环，生产构建直接 TDZ 白屏（F-11，2026-10-17）。现在 element-plus 单 chunk（~974kB）/ echarts 独立 / 其余 vendor。
- 三端资源路径统一走 `assetUrl()`：`IS_DESKTOP || IS_NATIVE` 时拼 `API_ORIGIN`，Web/PWA 用相对路径；hash 路由判断同样用 `IS_DESKTOP || IS_NATIVE`。
- mobile（Capacitor）构建不注入 PWA/SW；Web/PWA 才需要 SW。
- 自测：`backend/scripts/test_api.py`（141 断言，幂等，八表回基线）+ `mvn test`（99 用例）+ `eval_recall.py --min-recall 0.75`。
- 本地特有坑：8080 曾被动态端口占用（改 dynamicport start=49152）；Electron 需 ELECTRON_MIRROR 镜像；中文路径下 Android 构建走 ASCII junction。
- **提交铁律**（`CONTRIBUTING.md`）：禁裸用 `git add .`，先 `git status` 看清单；`_android-sdk/`、`desktop/release*/`、`*.keystore`、`application-local.yml` 一律不入库（根 `.gitignore` 已覆盖）。
- 仓库体积基线：pack ≈ 28.5 MiB（2026-10-17 整理后）。整理前曾因误 add 膨胀到 697 MB，已 `git gc --prune=now` 回收。
- **提交命名沿用项目习惯**：`versionNN_简述`，**一个版本一笔提交**（`version04_第一次漏洞修改` / `version05_debug02` / `version06-software01` / `version07_文档整理`）。用户明确偏好这个风格，不要用 `type(scope): 摘要` 的 conventional commits。细节写正文。
- git 回滚点：`version07_文档整理`（文档整理）；整理前状态在标签 `baseline-before-reorg`（6750e0a），`git show baseline-before-reorg` 可对照。
