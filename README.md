# 拾光 · 校园失物招领平台（以图搜图）

> 软件工程课程设计 · **单 Web**：浏览器直接访问（后端同源托管前端），无需安装任何客户端

上传一张物品照片，系统在全部已审核启事里找出"同一件东西"的其它照片，不依赖文字描述，也不要求用户手写标签。

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
| 前端 | Vue 3.5 · Vite 6 · Pinia · Element Plus · ECharts 6 | `:5173` |
| 数据库 | MySQL 8+（库名 `lost_found`，八表：user / item / item_image / claim_record / message / admin_log / favorite / delivery） | `:3306` |
| 部署 | 单 Web：后端托管 `frontend/dist`，浏览器访问 :8080；可选 cloudflared 临时公网演示 | — |
| 构建 | JDK 21 · Maven 3.9+ · Node 20+ | — |

规模：45 个 REST 端点 · 141 条接口断言 · 99 个后端单测 · 12 个前端单测 · 32 条缺陷全闭环。

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

### 第 2 步：建库建表（只做一次）

```bash
mysql -u root -p < lost-found/backend/src/main/resources/db/schema.sql
```

脚本开头自带 `CREATE DATABASE IF NOT EXISTS lost_found`，**幂等**，重复执行无害，不会清掉已有数据。

验证：

```bash
mysql -u root -p -e "USE lost_found; SHOW TABLES;"
# 应看到 8 张表
```

### 第 3 步：生成本地配置

```bash
cd lost-found/backend
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
cd lost-found/backend
mvn spring-boot:run
```

日志出现 `Started LostFoundApplication in x.x seconds` 即成功，**窗口保持开着**。

两个要点：

- **必须在 `lost-found/backend` 目录下启动** —— 上传目录 `./uploads` 是相对工作目录的路径，换目录会另建空目录导致图片 404；
- **首次启动自动播种** 60 条演示启事、120 条图片引用、认领与消息记录，并重算 pHash，不用手工导 SQL 数据。

想打成 jar 长期跑：

```bash
mvn clean package -DskipTests
java -jar target/lost-found-backend.jar
```

### 第 5 步：启动前端（窗口 2）

```bash
cd lost-found/frontend
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

- [ ] `http://localhost:5173` 落地页有内容、有图（图来自 `lost-found/backend/uploads/`）
- [ ] 登录 管理员 `10000001` / `admin123`，普通用户 `20240001` / `user123`
- [ ] 失物墙有真实数据（60 条启事）
- [ ] 以图搜图：上传 `lost-found/backend/uploads/_query/q-cup.jpg` → 命中该物品多条启事，相似度 ~100%
- [ ] 导航栏出现「🛡️ 后台管理」→ 看板 / 审核 / 用户 / 日志 / 运维 五个页面能打开

自动化自测（可选，需要 Python 3 + `pip install pillow requests`）：

```bash
cd lost-found/backend
python scripts/test_api.py                        # 141 断言，幂等，跑完八表回基线
python scripts/eval_recall.py --min-recall 0.75   # 召回门禁 85.7%
mvn test                                          # 99 用例
```

---

## 五、公网访问（可选）

本机跑起来后，想让别人从外网访问，用 Cloudflare 临时隧道，免费、不限流量、自带 HTTPS：

```bash
# 1) 确认 cloudflared.exe 已在（仓库已备好一份，二进制走 Releases）
#    Project_operation_software/cloudflared/cloudflared.exe
#    需自行下载时：https://github.com/cloudflare/cloudflared/releases（选 windows-amd64.exe）
#    （花生壳 vicp.fun 免费版已停用，不再使用）

# 2) 直接双击
#    lost-found/scripts/tunnel.bat
```

它会先探测 8080 是否活着，然后打印一个 `https://xxx-yyy-zzz.trycloudflare.com` 地址，把这个地址发给别人即可。

注意：**免账号隧道的域名每次重启都会变**；电脑休眠 / 关掉窗口 / 后端停止，地址立即失效。

---

## 六、常见问题

| 现象 | 原因与处理 |
|---|---|
| 启动报连不上数据库 | `application-local.yml` 没生成或口令不对；或 MySQL 服务没起 |
| 端口 8080 / 5173 被占用 | `netstat -ano \| findstr :8080` 找 PID 结束进程，或改 `application.yml` / `vite.config.js` 的端口 |
| 图片全部 404 | 没有在 `lost-found/backend` 目录下启动后端（`uploads` 是相对路径） |
| 前端页面白屏（生产构建） | 改过 `vite.config.js` 的 `manualChunks` 后必须 `npm run build` 冒烟验证，dev 正常 ≠ build 正常 |
| 搜图结果全 100% 或全空 | pHash 未算，`POST /api/search/reindex` 重算 |
| 401 / 403 | token 过期、账号被封禁或权限不足，重新登录 |
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
│   ├── 01-交付文档/                 课程交付物：01-12 全套
│   │   └── 运行说明/                环境、启动、项目结构、新手版
│   ├── 02-过程资料/                 过程留痕（不交作业）
│   │   ├── 01-选题/                 选题论证、五人分工、准备清单
│   │   ├── 02-设计prompt/           页面设计 prompt（含已归档的多端方案）
│   │   ├── 03-迭代记录/             审查建议 → 首轮修复 → P0P1P2 → Z 批 → E 批 → 收尾
│   │   └── 04-参考项目/             三个上游开源项目离线副本
│   └── Knowledge_List/              答辩复习知识清单（8 份 · 208 个自测点；`06-多端与工程化` 已随单 Web 收敛归档）
│
├── Project_operation_software/      运行期软件（二进制走 Releases，仓库只留台账）
├── assets/演示图/                   演示物品图 6 张
│
└── lost-found/                      代码仓
    ├── backend/                     Spring Boot（:8080）
    └── frontend/                    Vue3（:5173）
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

## 九、继续深挖

| 我想…… | 看这里 |
|---|---|
| 逐条勾选的启动排查（含验收与排错） | `Creator/01-交付文档/运行说明/01-启动步骤.md` |
| 需求 / UML / 接口 / 数据库设计 | `Creator/01-交付文档/01`、`02`、`04`、`06` |
| 测试计划 / 用例 / 缺陷 / 报告 | `Creator/01-交付文档/07~10` |
| pHash 为什么这么调参 | `Creator/01-交付文档/11-以图搜图准确率实验报告-v1.0.md` |
| 部署到服务器 / 备份恢复 | `Creator/01-交付文档/12-部署手册-v1.0.md` |
| 已下线的桌面/移动端方案 | `Creator/02-过程资料/03-迭代记录/06-收尾/archive-多端/` |
| 改代码前先看历史决策 | `Creator/02-过程资料/03-迭代记录/` |
| 答辩复习 / 问答话术 | `Creator/Knowledge_List/00-使用说明与复习路线.md` |
| 提交代码前 | `Creator/CONTRIBUTING.md` |
