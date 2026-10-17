# 拾光 · 校园失物招领平台（以图搜图）— 运行与联调说明

> 软件工程课程设计 · 全栈可运行版本。技术栈：Spring Boot 3.3 + MyBatis-Plus + MySQL + JJWT + JImageHash(pHash) + Vue3 + Vite + Element Plus + ECharts。
> 核心亮点：以图搜图 = 感知哈希 pHash（JImageHash）→ 64 位指纹 → MySQL `BIT_COUNT(phash ^ q)` 汉明距离（JOIN 启事表过滤待审/下架）→ 阈值过滤 → Top-N。

---

## 一、目录结构

```
lost-found/
├── README.md                          本文件：运行与联调说明
├── 项目结构.md                         结构总览 + 各模块完成状态（Mark-Done）
├── 00-项目知识点与结构详解-新手版.md      面向零基础组员的全套知识点讲解（答辩必读）
├── 启动步骤-MarkDone.md                 逐条勾选式启动步骤（工具清单/命令/验收/排错）
├── docs/                              交付文档全套 01-12
│   ├── 01-需求规格说明书-SRS / 02-UML建模全套 / 03-项目计划书
│   ├── 04-接口文档 / 05-UI设计稿 / 06-数据库设计(ER+数据字典)
│   └── 07-测试计划 / 08-测试用例表 / 09-缺陷跟踪表 / 10-测试报告
│       11-以图搜图准确率实验报告 / 12-部署手册
│
├── backend/                           Spring Boot 后端
│   ├── pom.xml
│   ├── application-local.yml          本地敏感配置（真实口令/JWT secret，已 gitignore，勿提交）
│   ├── src/main/java/com/shiguang/lostfound/
│   │   ├── controller/                Auth/Item/Search/Claim/User/Admin
│   │   ├── service/                   业务层（含 SearchService 以图搜图、FileStorageService）
│   │   ├── mapper/                    MyBatis-Plus Mapper（ItemImageMapper 含汉明距离 SQL）
│   │   ├── entity/ dto/ vo/           实体 / 入参 / 出参
│   │   ├── config/ interceptor/ util/ context/ bootstrap/
│   │   └── LostFoundApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml            占位符配置（数据源/JWT/上传目录/搜图参数）
│   │   ├── application-prod.yml       生产 profile（关闭 doc.html 等调试入口）
│   │   └── db/schema.sql 建库建表      （data.sql 已删除，见 Z-06：演示数据由 DataInitializer 播种）
│   ├── src/test/java/                 单元测试 PHashUtilTest / AuthServiceTest
│   │                                  + 哈希准确率实验 HashExperimentTest（输出 scripts/exp_*.csv）
│   ├── scripts/
│   │   ├── gen_seed_images.py         生成演示图片+manifest（同物多拍变体，离线读 uploads/_src）
│   │   ├── test_api.py                接口全量联调测试（50 断言，幂等，失败退出码非 0）
│   │   └── backup.sh（+backup.md）     数据库 + uploads 备份/恢复脚本
│   └── uploads/                       图片落盘（seed/ 演示图 + manifest，_query/ 留出查询图，_src/ 生成底图）
│
└── frontend/                          Vue3 前端
    ├── vite.config.js                 已配 /api 与 /uploads 代理 → :8080
    └── src/
        ├── api/ stores/ router/ components/ directives/ styles/ mock/
        └── views/                     9 个用户页 + admin/ 后台六页面（Layout/Dashboard/Audit/Users/Logs/Tools）
```

---

## 二、环境要求（本机已验证）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | 与 Spring Boot 3.3 配套 |
| Maven | 3.9.16 | 已装于 `D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16`（含阿里云镜像 settings.xml） |
| MySQL | 9.7（8.0+ 均可） | 连接串已加 `serverTimezone=Asia/Shanghai` |
| Node.js | 20+（本机 24） | 前端（含 echarts） |
| Python | 3.x + Pillow + requests | 跑 scripts/ 下的生成与测试脚本 |

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
`application.yml` 只留占位符 `${DB_PASSWORD:}` 等；默认激活 `local` profile 读取上述文件。**删除该文件后启动会因连不上库而明确报错**（不静默 fallback）。

### 1) 数据库
```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql     # 建库建表
# 演示数据：不用手动导入。先跑 scripts/gen_seed_images.py（离线，读 uploads/_src），
# 再启动后端由 DataInitializer 播种 60 条启事/120 条图片引用（30 个去重文件）/认领三态/消息 并重算 pHash
# （Z-06：data.sql 已删除——历史 dump 与种子逻辑失同步会带回脏数据，离线备份统一走 scripts/backup.sh）
```

### 2) 后端（:8080）
```bash
cd backend                      # ★ 固定在此目录启动（upload-dir 为相对路径）
set JAVA_HOME=<你的JDK21路径>     # 用环境变量，不硬编码
"%MAVEN_HOME%\bin\mvn" spring-boot:run
# 或打包后： "%MAVEN_HOME%\bin\mvn" package -DskipTests && java -jar target/lost-found-backend.jar
# 生产：加 --spring.profiles.active=prod（关闭 /doc.html 等调试入口）
```

### 3) 前端（:5173）
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
| 管理员 | 10000001 | 见 application-local.yml 的 `seed.admin-password`（以该文件为准，启动日志亦会打印） | 可直接登录进 /admin 后台 |
| 演示发布/认领人 | 20240001 ~ 20240020 | 见 `seed.user-password`（以该文件为准，启动日志亦会打印） | 60 条启事的发布者 |
| 自建账号 | 前端注册任意 6-12 位学号 | 自设 ≥6 位 | BCrypt 入库，JWT 登录 |

启动日志会打印当次生效的演示账号与口令。
- **以图搜图演示**（top-n=10、阈值 12，2026-10-15 标定，实测见 `backend/scripts/recall_results.csv` 与 docs/11 §2.4）：
  - `q-cup.jpg` → 命中 8/8 条同物启事，最低相似度 81.3%
  - `q-keys.jpg` → 8/8，最低 82.8%
  - `q-earbuds.jpg` → 10/10，最低 85.9%
  - `q-umbrella.jpg` → 10/10，最低 81.3%
  - `q-scarf.jpg` → 8/10，最低 89.1%
  - `q-card.jpg` → 4/10（卡套反光变体汉明距离偏大，属 pHash 能力边界，见 docs/11 §2.4）
  - 整体召回 48/56 = 85.7%，误检 0；单张平均耗时约 54ms。
- **管理后台**：管理员登录后 NavBar 出现「🛡️ 后台管理」→ 看板（ECharts 趋势+分类环形）/ 审核页签 / 用户封禁⇄解封 / 操作日志。
- **审核流演示**：新注册账号发布一条启事 → 个人中心显示「待审核」标签；管理员在后台审核通过后才在墙页可见、可被搜图命中。

---

## 五、联调/自测与备份

```bash
cd backend
python scripts/test_api.py     # M1-M6 全量接口测试（50 断言）；退出码非 0 即有失败
"%MAVEN_HOME%\bin\mvn" test   # 单元测试 + 哈希准确率实验（PHashUtilTest/AuthServiceTest/HashExperimentTest）
bash scripts/backup.sh        # 数据库 + uploads 备份（见 scripts/backup.md）
```
- `test_api.py` 幂等：使用种子账号、唯一标题发布并在结尾由管理员删除清理，连跑多次六表行数回基线。
- 准确率实验结果见 `scripts/exp_results.csv`、`exp_per_transform.csv`，分析结论见 `docs/11-以图搜图准确率实验报告`。
- 前端 `npm run build` 通过即前端可编译。

---

## 六、常见问题（风险预置对应）

| 现象 | 原因 / 处理 |
|---|---|
| MySQL 时区报错 | 连接串已含 `serverTimezone=Asia/Shanghai&useSSL=false`，勿删 |
| 跨域报错 | 前端走 Vite 代理本就是同源，不应出现；CorsConfig 仅放行 `web.cors.allowed-origins` 显式来源（Z-03：已移除通配+凭证放行），确有跨域需求在配置里加来源 |
| 启动报连不上数据库 | `backend/application-local.yml` 缺失或口令不对——该文件为本地必需（见三.0） |
| 以图搜图全 100% 或全空 | 检查 `ItemImageMapper` 用 `^`（按位异或）而非 `XOR`（逻辑异或）；检查 phash 是否已入库（`/api/search/indexed-count`） |
| 发布后搜不到自己 | ① 发布需管理员审核通过后才可被搜到（搜图 SQL 已 JOIN item 过滤待审/下架）；② 若 pHash 漏算可用 `POST /api/search/reindex` 补 |
| 图片不显示 | 确认 `/uploads/**` 代理与 `file.upload-dir` 指向 backend/uploads；缩略图缺失时管理员后台可「重建缩略图」 |
| pHash 对旋转不鲁棒 | 属算法特性：演示变体只用缩放/裁剪/亮度/对比度/灰度，不用旋转（详见 docs/11 实验报告） |
