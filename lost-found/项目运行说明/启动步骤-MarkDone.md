# 拾光 · 校园失物招领平台 — 启动步骤 Mark-Done

> 用法：从上往下逐条执行，完成一条把 `[ ]` 改成 `[x]`。全部 `[x]` = 项目已在本地跑起来。
> 终端默认用 **Git Bash**（每条同时附 cmd 写法）。路径均为你本机实际安装位置。
> 本机环境核验日期：2026-10-15（标注「本机已验」的条目当时实测通过）。

---

## 〇、用什么软件启动（工具清单）

| 用途 | 软件 | 版本（本机已验） | 位置 / 说明 |
|---|---|---|---|
| Java 运行环境 | **JDK** | 21.0.10 LTS | `D:\Visual_Stdio_Code\Java_peizhi\JDK21`（启动后端必须把 JAVA_HOME 指到它） |
| 后端构建/启动 | **Apache Maven** | 3.9.16 | `D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16`（含阿里云镜像 settings.xml） |
| 数据库服务 | **MySQL** | 9.7（服务名 `MySQL`） | 客户端 `D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64\bin\mysql.exe`（不在 PATH，用全路径或 Navicat 代替） |
| 前端运行 | **Node.js（含 npm）** | node v24.16.0 / npm 11.13.0 | 已装，`node -v` 可查 |
| 辅助脚本（可选） | **Python** | 3.13.15 | `D:\Visual_Stdio_Code\Pathon_peizhi\python-3.13.15\python.exe`；跑 `scripts/` 下生成图/自测脚本才需要，另需 `pip install pillow requests` |
| 敲命令的终端 | **Git Bash**（推荐）或 cmd / PowerShell | — | 本文件命令两种写法都给 |
| 看界面 | **浏览器**（Chrome / Edge） | — | 打开 `http://localhost:5173` |
| 看代码（可选） | VS Code / IDEA | — | 仅阅读编辑用，启动不依赖 |

> 启动只需要 **4 样东西在跑/在用**：MySQL 服务、JDK+Maven（后端窗口）、Node（前端窗口）、浏览器。其余都是可选。

---

## 一、启动前检查（每次启动前过一遍）

- [x] MySQL 服务在运行（本机已验）
  ```bash
  netstat -ano | grep ":3306" | grep LISTENING     # Git Bash；有输出即在跑
  ```
  ```cmd
  netstat -ano | findstr ":3306" | findstr LISTENING
  ```
  没输出则启动服务：`net start MySQL`（管理员 cmd）或 服务管理器里启动 `MySQL`。

- [x] JDK21 可用（本机已验）
  ```bash
  "/d/Visual_Stdio_Code/Java_peizhi/JDK21/bin/java" -version    # 应显示 21.x
  ```

- [x] Maven 可用（本机已验）
  ```bash
  "/d/Visual_Stdio_Code/Maven/apache-maven-3.9.16/bin/mvn" -v   # 应显示 Apache Maven 3.9.16
  ```

- [x] Node / npm 可用（本机已验）
  ```bash
  node -v && npm -v
  ```

- [x] 本地敏感配置存在（本机已验，**勿删勿提交 git**）
  ```bash
  ls backend/application-local.yml
  ```
  缺失时按 README「三.0」创建（数据库口令 / JWT secret / 演示口令）。

---

## 二、数据库初始化（仅第一次）

- [ ] 建库建表（已做过就跳过；重复执行也无害，脚本是幂等的）
  ```bash
  "/d/Visual_Stdio_Code/Claude_Code/MysqL/mysql-9.7.0-winx64/bin/mysql" -u root -p < backend/src/main/resources/db/schema.sql
  ```
  ```cmd
  "D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64\bin\mysql" -u root -p < backend\src\main\resources\db\schema.sql
  ```

- [ ] 演示数据：**不用手动导入**。第一次启动后端时 `DataInitializer` 自动播种 60 条启事 / 120 条图片引用（30 个去重文件）/ 认领与消息种子并重算 pHash。
  （Z-06：data.sql 已删除——历史 dump 与种子逻辑失同步会带回脏数据，离线备份统一走 scripts/backup.sh）

- [ ] 验证库表存在
  ```bash
  "/d/Visual_Stdio_Code/Claude_Code/MysqL/mysql-9.7.0-winx64/bin/mysql" -u root -p -e "USE lost_found; SHOW TABLES;"
  ```
  应看到 6 张表：user / item / item_image / claim_record / message / admin_log。

---

## 三、启动后端（窗口 1，:8080）

- [ ] 打开一个 **Git Bash** 窗口，执行：
  ```bash
  cd /d/Visual_Stdio_Code/Dai_ma/软件工程/lost-found/backend
  export JAVA_HOME="/d/Visual_Stdio_Code/Java_peizhi/JDK21"
  "/d/Visual_Stdio_Code/Maven/apache-maven-3.9.16/bin/mvn" spring-boot:run
  ```
  cmd 写法：
  ```cmd
  cd /d D:\Visual_Stdio_Code\Dai_ma\软件工程\lost-found\backend
  set JAVA_HOME=D:\Visual_Stdio_Code\Java_peizhi\JDK21
  D:\Visual_Stdio_Code\Maven\apache-maven-3.9.16\bin\mvn spring-boot:run
  ```

- [ ] 等到日志出现这两行 = 后端成功（**此窗口保持开着**）：
  ```
  Started LostFoundApplication in x.x seconds
  演示账号 → 管理员: 10000001 / …（口令见 application-local.yml）
  ```

- [ ] 接口自检（另开窗口或浏览器）：
  ```bash
  curl http://localhost:8080/api/stats        # 返回 {"code":200,...} 即通
  ```
  接口文档：`http://localhost:8080/doc.html`（Knife4j，仅开发态）。

---

## 四、启动前端（窗口 2，:5173）

- [ ] 打开**第二个** Git Bash 窗口：
  ```bash
  cd /d/Visual_Stdio_Code/Dai_ma/软件工程/lost-found/frontend
  npm install        # 仅第一次（依赖含 echarts）
  npm run dev
  ```
  cmd 写法相同（cd /d … 后两行不变）。

- [ ] 看到 `➜  Local:   http://localhost:5173/` = 前端成功（**此窗口保持开着**）。

---

## 五、浏览器验收（逐页点一遍）

- [ ] 打开 `http://localhost:5173` → 落地页正常显示。
- [ ] 登录：学号 `10000001`，口令 = `backend/application-local.yml` 里 `seed.admin-password` 的值（启动日志也打印了；普通演示号 20240001~20240020 用 `seed.user-password`）。
- [ ] 首页/失物墙有真实数据和图片（图来自 `backend/uploads/`）。
- [ ] 以图搜图：上传 `backend/uploads/_query/q-cup.jpg` → 命中该物品多条启事、相似度 ~100%。
- [ ] 导航栏出现「🛡️ 后台管理」→ 看板（ECharts 图）/ 审核 / 用户 / 日志四个页面能打开。
- [ ] 个人中心四页签（我的发布/我的认领/收到的申请/消息通知）正常。

---

## 六、停止与重启

- [ ] 停止：后端、前端两个窗口各按一次 `Ctrl+C`；MySQL 服务不用停。
- [ ] 重启：重复第三、四步即可（数据在 MySQL 里，不会丢）。
- [ ] 重置演示数据（可选）：`TRUNCATE` 六张表 → 重启后端 → 自动重新播种并重算 pHash。

---

## 七、常见失败速查

| 现象 | 原因 / 处理 |
|---|---|
| `找不到或无法加载主类 …LostFoundApplication` | target 目录状态脏了 → `mvn clean spring-boot:run` |
| 启动报连不上数据库 | `backend/application-local.yml` 缺失或口令不对；或 MySQL 服务没起（`net start MySQL`） |
| 端口 8080/5173 被占用 | `netstat -ano \| grep :8080` 找 PID 结束进程，或改 yml / vite.config 端口 |
| 图片不显示 | 必须在 `backend/` 目录下启动（upload-dir 是相对路径）；确认 vite 代理在 |
| 搜图全 100% 或全空 | 见 README 第六节；漏算哈希可 `POST /api/search/reindex` |
| 前端白屏 | `npm install` 没跑完；或重启 `npm run dev` |
| 401 | token 过期/账号被封禁 → 重新登录 |

---

## 八、复制粘贴速查卡（Git Bash）

```bash
# 窗口1：后端
cd /d/Visual_Stdio_Code/Dai_ma/软件工程/lost-found/backend
export JAVA_HOME="/d/Visual_Stdio_Code/Java_peizhi/JDK21"
"/d/Visual_Stdio_Code/Maven/apache-maven-3.9.16/bin/mvn" spring-boot:run

# 窗口2：前端
cd /d/Visual_Stdio_Code/Dai_ma/软件工程/lost-found/frontend
npm run dev

# 浏览器
#   用户端  http://localhost:5173
#   接口文档 http://localhost:8080/doc.html
```
