# 任务书：把「拾光·校园失物招领平台」打包成 Windows 桌面安装包

> 把本文件整份交给 AI 编程 Agent，作为唯一任务说明。不要删减，不要跳读。

## 0. 你的角色与最终交付

你是一名负责 Windows 桌面交付的工程师 Agent，可以用 PowerShell、Maven、npm、jpackage/jlink。

**最终交付物**：一个 `拾光-Setup.exe`（或在不具备条件时退化为 `拾光-便携版.zip`），在一台**没有安装 JDK、MySQL、Node.js** 的干净 Windows 上安装后双击即可使用，含卸载入口。

**最终用户视角**：双击安装 → 桌面快捷方式 → 双击启动 → 自动拉起内嵌数据库与后端 → 打开应用界面 → 能注册登录、发布启事、以图搜图。全程不需要用户配置任何环境。

---

## 1. 项目背景（绝对路径）

| 项 | 值 |
|---|---|
| 项目根 | `D:\Visual_Stdio_Code\Dai_ma\软件工程\lost-found` |
| 后端 | `backend/`，Spring Boot 3.3.5 + JDK 21 + MyBatis-Plus 3.5.7 + MySQL + JImageHash(pHash) + JJWT + Knife4j |
| 后端产物 | `backend/target/lost-found-backend.jar`（约 50 MB，已存在） |
| 前端 | `frontend/`，Vue 3.5 + Vite 6 + Element Plus + Pinia + ECharts，`npm run build` 产出 `frontend/dist/`（已存在） |
| 数据库 | MySQL 8+/9.x，库名 `lost_found`，6 张表：user / item / item_image / claim_record / message / admin_log |
| 核心亮点 | 以图搜图 = pHash 64 位指纹 → `BIT_COUNT(phash ^ q)` 汉明距离 → 阈值 10 → Top-N 6 |
| 现有文档 | `README.md`、`项目结构.md`、`01-交付文档/12-部署手册-v1.0.md` |

## 2. 已核实的本机环境（直接使用，不必重新探测）

- `JAVA_HOME` = `D:\Visual_Stdio_Code\Java_peizhi\JDK`，`java` / `jpackage` / `jlink` 版本均为 **21.0.10**（jpackage 可用）
- Maven：`D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16\bin\mvn.cmd`（未加入 PATH，用全路径调用）
- Node v24.16.0 / npm 11.13.0
- 便携 MySQL 源目录：`D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64`（含 bin / lib / share / data，共约 1529 MB，其中 bin 904 MB）
- 本机已有 MySQL 服务在运行，**占用 3306 端口** → 打包版必须换端口
- **未安装**：WiX Toolset、Inno Setup、Docker、7-Zip、NSIS
- 已存在可复用产物：`backend/target/lost-found-backend.jar`、`frontend/dist/`
- `frontend/node_modules` 已存在，可直接 `npm run build`

## 3. 现状与必须规避的地雷（已逐条核实，出错基本都出在这里）

1. `config/WebMvcConfig.java` 只映射了 `/uploads/**`，**没有 SPA history 回退** → 直接访问或刷新 `/wall` 会 404。
2. `db/schema.sql` 含 `CREATE DATABASE` + 一堆 `DROP TABLE IF EXISTS`，而 `application.yml` 中 `spring.sql.init.mode: never`。**绝不能**拿它配 `mode: always`，否则每次启动清空数据。
3. `backend/application-local.yml`（被 gitignore，含真实 DB 口令与 JWT secret）缺失时启动直接失败。**不得打进安装包**。
4. `file.upload-dir` 默认 `./uploads`，是相对启动目录的路径。
5. `mapper/ItemImageMapper.java` 使用 MySQL 专有位运算 `BIT_COUNT(ii.phash ^ #{q})`，插入/查询依赖 MySQL 语义，**不许换数据库、不许改这段 SQL 的语义**。
6. `pom.xml` 未配置 `mainClass`；jar 清单为 `Main-Class: org.springframework.boot.loader.launch.JarLauncher`、`Start-Class: com.shiguang.lostfound.LostFoundApplication`。
7. `bootstrap/DataInitializer.java` 读取 `uploads/seed/manifest.json` 播种 60 条启事 + 120 图并重算 pHash，**幂等**（item 表非空则跳过），运行期不需要 Python。`uploads/seed/` 与 `uploads/_query/` 必须随包分发。
8. `frontend/dist/index.html` 引用外网字体 `fonts.loli.net`（离线时退化为系统字体，可接受；如要 100% 离线可自行本地化字体）。
9. `backend/` 下存在**不得进入安装包**的文件：`_admintoken.txt`（真实管理令牌，安全风险）、`_r.json`、`_run.log`、`_t.log`、`_verify.log`；以及 `backend/uploads/_exp/`（78 个实验图，约 3 MB）。
10. 项目**没有健康检查接口**，启动器需要自建一个。
11. `uploads` 目录还有一个 `2026/` 目录（运行时上传的历史图片），属于开发数据，不要打包。
12. `application.yml` 的 JDBC URL 把 `localhost:3306` 写死了，需要改成可用环境变量/启动参数覆盖。

## 4. 硬性约束（违反即返工）

- 不改以图搜图算法与 SQL 语义，不改数据库类型（继续用 MySQL）。
- 所有用户数据（数据库 datadir、uploads、生成的配置）必须写到 `%LOCALAPPDATA%\拾光\`，安装目录必须视为只读。
- 打包版端口固定：**MySQL 13306**、**后端 18080**；后端 `server.address` 只绑 **127.0.0.1**（避免防火墙弹窗）。
- 生产启动一律加 `--spring.profiles.active=prod`（关闭 Knife4j/doc.html）。
- 不得把 `application-local.yml`、MySQL 现有 `data` 目录、`_admintoken.txt`、`uploads/_exp`、`uploads/2026`、`node_modules`、`target` 打进安装包。
- 不修改既有业务逻辑与测试；确有必要改动时，先在报告中说明原因与影响面。
- 每执行一步都要有可验证的结果。命令失败时原样报告 stdout/stderr，**不许静默跳过或伪造成功**。
- 无法确定的事必须实测确认，不要凭猜测下结论。

## 5. 分阶段实施步骤

### 阶段 A：让后端能独立托管前端

- A1 在 `backend/pom.xml` 增加 `maven-resources-plugin`（或用 `frontend-maven-plugin`），在 `prepare-package` 阶段把 `frontend/dist/**` 复制到 `target/classes/static/`；同时在 `frontend/` 不存在 dist 时给出明确告警而不是静默成功。
- A2 修改 `config/WebMvcConfig.java`：为静态资源注册回退解析器，使 `/api/**`、`/uploads/**` 之外的未命中路径返回 `index.html`（history 模式）；确保 `/api/**` 仍走 JwtInterceptor。
- A3 新增健康接口 `GET /api/health`，返回统一响应体 `{"code":200,...}`，供启动器轮询（放在 `controller/` 下，例如 `HealthController`）。
- A4 把 `application.yml` 的 JDBC URL 改为可覆盖形式（如 `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/lost_found?...`），开发默认值保持 3306 不变。
- A5 新增 `db/schema-init.sql`：只含 `CREATE TABLE IF NOT EXISTS`（及必要的索引），**不含** `CREATE DATABASE`、`USE`、`DROP`、`TRUNCATE`；内容与 `schema.sql` 的六表结构一致。**不要删除或修改原 `schema.sql`**。
- A6 配置 `spring.sql.init.mode: always` + `schema-locations: classpath:db/schema-init.sql`（仅生产打包场景需要，允许用 profile 区分；不允许影响现有开发流程）。

**验收 A**：`mvn clean package -DskipTests` 后，`java -jar` 启动，浏览器访问 `http://127.0.0.1:8080/` 能打开前端首页，`/wall` 直接刷新不 404，`GET /api/health` 返回 200，图片仍能显示，以图搜图仍能命中 `backend/uploads/_query/q-cup.jpg`。

### 阶段 B：配置与数据外置（首启自举）

- B1 新建 `packaging/` 目录，用于存放所有打包脚本与模板。
- B2 写 `packaging/scripts/New-ShiguangConfig.ps1`：若 `%LOCALAPPDATA%\拾光\config\application-local.yml` 不存在，则生成它，内容包含：随机 32 字节 hex 的 `jwt.secret`、随机强口令的 `spring.datasource.password`、随机演示口令 `seed.admin-password` / `seed.user-password`；同时把演示账号与口令写入 `%LOCALAPPDATA%\拾光\初始账号.txt`（UTF-8，含管理员学号 10000001）。
- B3 数据目录统一约定为 `%LOCALAPPDATA%\拾光\{config,data,uploads,logs}`；脚本需保证目录存在。
- B4 启动参数统一由启动器注入：`--spring.config.additional-location=file:<config>/`、`--file.upload-dir=<uploads>`、`--server.port=18080`、`--server.address=127.0.0.1`、`--spring.datasource.url=...:13306/...`、`--spring.profiles.active=prod`。

**验收 B**：删掉 `%LOCALAPPDATA%\拾光\` 后重跑脚本，配置重新生成；启动日志中能打印出演示账号口令，与 `初始账号.txt` 一致。

### 阶段 C：便携运行目录与启动器

目标目录结构：

```
拾光\
├─ app\        lost-found-backend.jar（static 已并入）
├─ runtime\    jlink 裁出的 JRE 21
├─ mysql\      bin + lib + share（不含 data，不含 *.pdb / *.lib / mecab *.dic）
├─ uploads\    seed\ + _query\
├─ config\     （运行时由脚本生成，不预置）
├─ data\       （运行时由脚本生成，不预置）
├─ 拾光.cmd     启动器
└─ LICENSE-MySQL.txt
```

- C1 `packaging/scripts/New-Runtime.ps1`：调用 `jlink` 生成精简 JRE：
  `--add-modules java.base,java.desktop,java.sql,java.naming,java.management,java.instrument,java.logging,jdk.unsupported,jdk.crypto.ec,jdk.charsets,java.transaction.xa,java.xml --strip-debug --no-header-files --no-man-pages --compress=zip-6`
  （`java.desktop` 用于缩略图 AWT，`jdk.crypto.ec` 用于 MySQL 加密连接，不得删。）
- C2 `packaging/scripts/New-MysqlBundle.ps1`：从本机 MySQL 源目录复制 `bin`、`lib`、`share` 与 `LICENSE` 到 `build\拾光\mysql`，删除 `*.pdb`、`*.lib`、`lib\mecab\*.dic`、`*-debug.dll`；**绝不复制 data 目录**。完成后报告裁剪前后体积。
- C3 `packaging/scripts/Initialize-Mysql.ps1`：若 `%LOCALAPPDATA%\拾光\data` 不存在 → 执行 `mysqld --initialize-insecure --basedir=<mysql> --datadir=<data> --port=13306` → 以 `--skip-networking=0 --bind-address=127.0.0.1 --port=13306` 启动 → 用 `mysqladmin --protocol=tcp --port=13306 -u root ping` 轮询就绪 → `ALTER USER 'root'@'localhost' IDENTIFIED BY '<生成的随机口令>'` → `CREATE DATABASE IF NOT EXISTS lost_found DEFAULT CHARACTER SET utf8mb4`。全程记录日志到 `%LOCALAPPDATA%\拾光\logs\mysql-init.log`。
- C4 `packaging/scripts/Start-Shiguang.cmd`（或 `.ps1` + 一个瘦 `.cmd` 包装）：按顺序执行
  1) 生成/读取配置；2) 初始化并启动 MySQL（已在运行则跳过）；3) 启动后端并轮询 `http://127.0.0.1:18080/api/health`（超时 90 秒，超时弹出明确错误并输出日志路径）；4) `start http://127.0.0.1:18080`；5) 窗口保持，用户关闭窗口时执行 `mysqladmin shutdown` 关闭 mysqld，确保不留残留进程。
- C5 写 `packaging/scripts/Build-Portable.ps1`：串起 C1–C4 + 后端构建 + 前端构建，产出完整的 `build\拾光\` 便携目录。

**验收 C**：直接运行 `build\拾光\拾光.cmd`，在开发机上完整跑通；关闭后 `Get-Process mysqld` 应为空。

### 阶段 D：jpackage 外壳（可选，失败即回退）

- D1 优先尝试：`jpackage --type app-image --name 拾光 --app-version 1.0.0 --input app --main-jar lost-found-backend.jar --main-class org.springframework.boot.loader.launch.JarLauncher --runtime-image runtime --icon packaging/拾光.ico --win-console --java-options "-Dfile.encoding=UTF-8" --java-options "-Djava.awt.headless=true"`。
- D2 如果 jpackage 启动 Spring Boot fat jar 失败（历史上存在此坑），**立即回退到方案 B**：不使用 jpackage 外壳，保留 `拾光.cmd`，由 Inno Setup 直接为 `.cmd` 建快捷方式。回退时必须在报告中写明失败原因与原始错误。
- D3 图标：若没有 `.ico`，用现有 SVG/PNG 生成一个 256×256 的 ico（可用 PowerShell + .NET 或 Python Pillow），或明确说明缺图标并继续。

**验收 D**：生成的 `拾光.exe`（或 `拾光.cmd`）双击能完成阶段 C 的全部流程。

### 阶段 E：Inno Setup 安装包

- E1 本机未安装 Inno Setup。请用官方地址下载并静默安装（`ISCC.exe` 通常位于 `C:\Program Files (x86)\Inno Setup 6\ISCC.exe`）；若因权限或网络无法安装，**不要硬凑**，改为交付便携 zip 并在报告中说明。
- E2 编写 `packaging/拾光.iss`：
  - `[Setup]`：AppName=拾光校园失物招领、版本 1.0.0、默认目录 `{autopf}\拾光`、`PrivilegesRequiredOverridesAllowed=dialog`（允许普通用户装到用户目录、避免必须管理员）
  - `[Files]`：收 `app\`、`runtime\`、`mysql\`、`uploads\`、`拾光.cmd`；**不收** `config\`、`data\`、`uploads\2026\`
  - `[Icons]`：桌面 + 开始菜单快捷方式指向启动器
  - `[Run]`：安装完成后可选"立即启动拾光"
  - `[UninstallRun]`：卸载前先 `mysqladmin shutdown`（端口 13306）
  - `[Code]`：安装/卸载前检测并结束残留的 `mysqld.exe`（限 13306 实例）与 `java.exe`（限本项目进程），避免文件占用导致卸载失败
  - 卸载默认**保留** `%LOCALAPPDATA%\拾光\`（用户数据），并提供说明
- E3 用 `ISCC.exe` 编译，产物输出到 `dist-installer\拾光-Setup.exe`，报告最终体积。

**验收 E**：`拾光-Setup.exe` 存在且可执行，双击安装流程走通，桌面出现快捷方式。

### 阶段 F：净机验收（必做，不能只在本机测）

- F1 优先使用本机可用的虚拟机或 Windows 沙盒；若确实无法获得干净环境，必须至少做到：用一个**没有 `JAVA_HOME`、PATH 里没有 java/mysql** 的受限账户/PowerShell 会话验证启动器不依赖系统环境变量。
- F2 逐项验收并记录证据：
  1. 安装成功（普通用户权限，装到用户目录也允许）
  2. 双击启动 → 界面正常打开
  3. 注册/登录成功
  4. 发布一条启事 → 管理员审核通过 → 墙页可见
  5. 以图搜图上传 `q-cup.jpg` → 命中且相似度接近 100%
  6. 关闭应用（含 mysqld）→ 重新启动 → 数据与图片仍在
  7. 卸载 → 无残留进程、无残留服务；用户数据目录按设计保留
- F3 记录：安装包体积、首次启动耗时、内存占用、遇到的问题。

### 阶段 G：交付文档

- G1 写 `packaging/README-打包说明.md`：从零复现整套打包（依赖、脚本调用顺序、每个产物位置、常见错误）。
- G2 写 `packaging/用户使用说明.md`：安装、首次启动会发生什么、演示账号在哪看、数据存在哪、如何备份、如何卸载、常见问题（端口被占用、Windows 防火墙、杀毒软件误报）。
- G3 报告中明确列出所有改动过的文件与新增文件清单。

## 6. 需要产出的文件清单（最低要求）

- `backend/pom.xml`（修改：静态资源复制）
- `backend/src/main/java/com/shiguang/lostfound/config/WebMvcConfig.java`（修改：SPA 回退）
- `backend/src/main/java/com/shiguang/lostfound/controller/HealthController.java`（新增）
- `backend/src/main/resources/application.yml`（修改：可覆盖的数据源 URL、初始化脚本配置）
- `backend/src/main/resources/db/schema-init.sql`（新增，不含 DROP）
- `packaging/scripts/New-Runtime.ps1`、`New-MysqlBundle.ps1`、`Initialize-Mysql.ps1`、`New-ShiguangConfig.ps1`、`Build-Portable.ps1`（新增）
- `packaging/拾光.cmd`（新增）
- `packaging/拾光.iss`（新增）
- `packaging/README-打包说明.md`、`packaging/用户使用说明.md`（新增）
- 产物：`build/拾光/`（便携目录）、`dist-installer/拾光-Setup.exe`

## 7. 工作方式要求

1. **先输出一份实施计划**（阶段、涉及文件、验证方式），再动手改代码；计划经确认后按阶段推进。
2. 每完成一个阶段，汇报：改动的文件（带路径）、执行的命令、验收结果（贴关键输出）。
3. 每个脚本都要处理失败路径：端口被占用、目录无写权限、MySQL 未就绪、后端启动超时，都要给出**可读的中文错误信息**并写日志文件。
4. 所有新增脚本统一 UTF-8（含 BOM 或显式设置控制台编码），避免中文乱码。
5. 不确定的环境事实要实测，不要假设；执行失败不要伪造结果，原样上报以便排查。
6. 不要修改与本任务无关的文件；不要执行 `git commit`/`git push` 等版本库操作。

## 8. 完成标准（Definition of Done）

- 干净 Windows 上：安装 → 启动 → 注册登录 → 发布 → 以图搜图命中，全流程无需用户安装任何环境。
- 重启后数据、图片、用户上传内容全部保留。
- 卸载有正常入口，且不残留进程/服务。
- `packaging/README-打包说明.md` 能让另一名工程师在另一台机器上复现出同样的安装包。
- 提交一份包含体积、耗时、已知问题与回退方案的最终报告。
