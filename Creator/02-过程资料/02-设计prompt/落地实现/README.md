# 拾光 · 校园失物招领平台（以图搜图）

> 软件工程课程设计 · 一套代码四端运行：**Web / Android APK / iOS PWA / 桌面 Electron**

以感知哈希（pHash）为检索核心的校园失物招领平台：上传一张物品照片，系统在全部已审核启事中找出"同一件东西"的其它照片，
不依赖文字描述，也不要求用户手写标签。

```
查询图 → pHash（JImageHash）→ 64 位指纹 → MySQL BIT_COUNT(phash ^ q) 汉明距离
      → JOIN 启事表过滤待审/下架 → 阈值(12)过滤 → 按相似度取 Top-10
```

实测：56 个查询变体召回 **48/56 = 85.7%**，误检 **0**，单张平均耗时约 **54 ms**（详见 `Creator/01-交付文档/11-以图搜图准确率实验报告-v1.0.md` §2.4）。

---

## 一、技术栈

| 层 | 技术 | 端口 |
|---|---|---|
| 后端 | Spring Boot 3.3.5 · MyBatis-Plus 3.5.7 · JJWT 0.12.6 · JImageHash 1.0.0 · Knife4j · OpenPDF | `:8080` |
| 前端 | Vue 3.5 · Vite 6 · Pinia · Element Plus · ECharts 6 | `:5173` |
| 数据库 | MySQL 8+（库名 `lost_found`，八表：user / item / item_image / claim_record / message / admin_log / favorite / delivery） | `:3306` |
| 多端 | Capacitor 7（Android）· PWA（iOS）· Electron（桌面） | — |
| 构建 | JDK 21 · Maven 3.9.16 · Node 20+ | — |

规模：47 个 REST 端点 · 141 条接口断言 · 99 个后端单测 · 12 个前端单测 · 32 条缺陷全闭环。

---

## 二、30 秒跑起来

```bash
# 1) 建库建表（首次）
mysql -u root -p < lost-found/backend/src/main/resources/db/schema.sql

# 2) 本地敏感配置：在 lost-found/backend/ 下新建 application-local.yml（已 gitignore）
#    datasource.password / jwt.secret / seed.admin-password / seed.user-password

# 3) 后端（★必须在 backend 目录启动，upload-dir 是相对路径）
cd lost-found/backend && mvn spring-boot:run

# 4) 前端开发模式
cd lost-found/frontend && npm install && npm run dev     # http://localhost:5173
```

后端同时托管 `frontend/dist`，所以构建后直接开 **http://localhost:8080** 就能用整套站点。
逐条勾选的详细步骤（含验收与排错）见 **`Creator/01-交付文档/运行说明/01-启动步骤.md`**。

---

## 三、目录地图

```
软件工程/
├── README.md                  ← 本文件
├── Creator/                               文档与过程资料（作者侧）
│   ├── CONTRIBUTING.md            git 协作规范与提交前检查单
│
│   ├── 01-交付文档/               课程交付物：01-12 全套 + 运行说明/
│   │   └── 运行说明/              环境、启动、结构、桌面端、移动端
│
│   └── 02-过程资料/               过程留痕（不交作业）
│       ├── 01-选题/               选题论证、五人分工、准备清单
│       ├── 02-设计prompt/         页面设计 prompt 与双端/桌面落地方案
│       ├── 03-迭代记录/           version-0~2.40/（历史 8 批）+ version-2.10 / 2.20 / 2.41
│       └── 04-参考项目/           三个上游开源项目离线副本
│
├── assets/演示图/             演示物品图 6 张
│
└── lost-found/                代码仓
    ├── backend/               Spring Boot（:8080）
    ├── frontend/              Vue3（:5173）+ Capacitor 原生工程
    └── desktop/               Electron 壳
```

`lost-found/_android-sdk/`、`desktop/release*/`、`*.keystore` 等本机产物已被 `.gitignore` 排除，**不入库**。

---

## 四、文档地图

| 我想…… | 看这里 |
|---|---|
| 交作业 / 打包文档 | `Creator/01-交付文档/`（只有这个目录是交付物） |
| 先看全貌与导读 | `Creator/01-交付文档/00-文档总览.md` |
| 把项目跑起来 | `Creator/01-交付文档/运行说明/01-启动步骤.md` |
| 理解需求与验收口径 | `Creator/01-交付文档/01-需求规格说明书-SRS-v1.0.md` |
| 看类图/时序图/状态机 | `Creator/01-交付文档/02-UML建模全套-v1.0.md` |
| 查接口与错误码 | `Creator/01-交付文档/04-接口文档-v1.0.md` |
| 看数据库八表设计 | `Creator/01-交付文档/06-数据库设计-ER与数据字典-v1.0.md` |
| 复现测试与缺陷闭环 | `Creator/01-交付文档/07~10`（计划/用例/缺陷/报告） |
| 理解 pHash 为什么这么调参 | `Creator/01-交付文档/11-以图搜图准确率实验报告-v1.0.md` |
| 部署到服务器 / 公网 | `Creator/01-交付文档/12-部署手册-v1.0.md` |
| 改代码前先看历史决策 | `Creator/02-过程资料/03-迭代记录/` |
| 查谁负责哪块 | `Creator/02-过程资料/01-选题/01-失物招领平台-五人分工方案.md` |
| 提交代码前 | `Creator/CONTRIBUTING.md` |

---

## 五、演示账号与核心流程

| 角色 | 学号 | 口令 |
|---|---|---|
| 管理员 | `10000001` | 见 `application-local.yml` 的 `seed.admin-password`（不写进任何文档） |
| 演示发布/认领人 | `20240001` ~ `20240020` | 见 `seed.user-password` |

启动日志会打印当次生效的账号。三条主流程：

1. **发布 → 审核 → 可见**：新用户发布启事显示"待审核"，管理员通过后才上墙、才能被搜图命中。
2. **以图搜图**：上传 `q-cup.jpg` 等查询图 → 返回 Top-10 及相似度百分比（`q-card.jpg` 因卡套反光属 pHash 能力边界）。
3. **认领 → 交付**：认领审核通过后自动生成认领证明 PDF，双方在消息中心下载（桌面端原生打开）。

---

## 六、自测

```bash
cd lost-found/backend
python scripts/test_api.py                        # 141 断言，幂等，八表回基线
mvn test                                          # 99 用例（HashExperimentTest 默认 skip）
python scripts/eval_recall.py --min-recall 0.75   # 召回门禁 85.7%
```

前端：`cd lost-found/frontend && npx vitest run`（12 用例）。

---

## 七、协作

提交前请读 **`Creator/CONTRIBUTING.md`**。三条红线：

1. 不要 `git add .`——先 `git status` 看清单再加具体目录。
2. 不提交 `_android-sdk/`、`desktop/release*/`、`*.keystore`、`application-local.yml`。
3. 单个文件 > 10 MB 先问，别直接提交。
