# Prompt 05 · 文档、测试资产与仓库卫生修正

> 前置：先读同目录 `索引与执行顺序.md`。
> 本 Prompt 解决的是**"文档说了但代码没做 / 数字与实测不符"**这一类问题。它应该**在 01~04 之后再执行**，因为部分数字要按最终实现回填。
>
> 核心原则：**宁可把数字改小、把"自动化"改成"手工"，也不能留一句无法自证的声明。** 答辩时被现场对出来的代价远大于承认覆盖不足。

---

## 任务总览

| 组 | ID | 标题 |
|---|---|---|
| A 测试资产 | A-01 | `test_api.py` 的幂等与清理是假自信（有实测证据） |
| | A-02 | `test_api.py` 漏测 4 个端点、注释不实、硬编码口令、键名不一致、无 preflight |
| | A-03 | 补齐核心业务单测（SearchService / ClaimService / FileStorageService） |
| | A-04 | `HashExperimentTest` 不是测试：无断言、相对路径、会覆盖已提交 CSV |
| B 文档数字 | B-01 | `Creator/01-交付文档/08` 用例总数与"自动化/手工"标注与实际不符（含逐条清单） |
| | B-02 | `Creator/01-交付文档/09` "P0 修复后均有自动化回归"不成立 |
| | B-03 | `Creator/01-交付文档/10` 三处不实声明 |
| | B-04 | `Creator/01-交付文档/04` 漏 5 个端点 + `admin/stats` 返回字段 + 错误码表不全 |
| | B-05 | `Creator/01-交付文档/06` 与 `schema.sql` 的 `idx_phash` 三方矛盾 |
| | B-06 | "前端八页/九页"、"下载底图"、"120 张图"等口径错误 |
| | B-07 | `项目结构.md` 状态的过时描述 |
| | B-08 | `Creator/01-交付文档/05` 无实际设计稿/截图，`Creator/01-交付文档/02` 无导出图 |
| C 仓库卫生 | C-01 | `_admintoken.txt`（真实 JWT）与 `_r.json` 已被 git 提交 |
| | C-02 | `.gitignore` 漏 `backend/backup/`；`uploads/_exp/` 78 个实验图入库 |
| | C-03 | `backup.sh` / `backup.md` 的多处硬伤 |
| | C-04 | `Creator/01-交付文档/03` 风险 R5/R7 状态与实际不符 |
| D 澄清 | D-01 | `Creator/01-交付文档/11` 实验数据是正确的，**不要改** |

---

# A 组 · 测试资产

## A-01（P1）`test_api.py` 的"幂等 + 六表回基线"是假自信

### 实测证据

`README.md`（§五）、`Creator/01-交付文档/07` §4、`Creator/01-交付文档/10` §3 都声称"连跑多轮，六表行数与上传文件数回基线"。实际不成立：

1. 清理逻辑只删启事本身：
   ```python
   # L212 附近
   DELETE /api/admin/items/{newId}
   ```
   服务端 `AdminService.deleteItem` 删消息的条件是 `.eq("ref_id", itemId).eq("type", "claim")`，
   而脚本 L178 造的私信由 `ItemService.sendDm` 写入 `type="dm"`、`ref_id=itemId` → **永远不被删除**。
   同理 `admin_log` 每次运行新增 4~5 行（登录限流/封禁/解封/审核/删除都会写日志）。
2. 自检只比对两个口径：
   ```python
   ok(end_stats == base_stats, "stats 回基线")
   ok(end_idx == base_idx, "索引数回基线")
   ```
   `stats` 只统计 `item` 表，`indexed-count` 只统计 `item_image` —— **`message` 与 `admin_log` 根本不在这两个口径里**，所以自检必然"通过"。
3. **实测反证**：当前库里 `message = 14`，而 `DataInitializer.seedRelations` 只播种 12 条 → 多出的 2 条正是测试脚本的私信残留；`admin_log = 8`，而种子为 0 → 全部来自测试运行。
4. 清理只在成功路径末尾（无 `try/finally`）：`L80~L208` 之间任何断言失败/异常/超时，都会留下启事 + 2 张图（含磁盘文件）+ 1 条认领 + 1 条私信。
5. 脚本自己新建的 2 张图会改变 `indexed-count` 基线，第二次连跑更容易失败（级联失败）。

### 修复要求

1. 把整个测试主体包进 `try / finally`，`finally` 里执行清理（删启事 + 断言 message/admin_log 行数回落）。
2. 扩展幂等自检口径：除了 `stats` 与 `indexed-count`，还要核对 `message` 与 `admin_log` 的**增量**。
   - 这需要一个"能读到这两张表"的口径。可选方案：
     - **A（推荐）**：在 `AdminController` 加一个仅管理员可用的 `/api/admin/db-counts`（返回六表行数），脚本用它做基线对比（顺带也方便运维）；
     - **B**：脚本直接用 `pymysql`/`mysql` 命令行查库（会引入新依赖或依赖本机客户端，不推荐）。
3. 如果清理无法做到完全回基线（例如 `admin_log` 按设计就是只增不改的审计日志），那么**必须修改文档措辞**，明确写"`admin_log` 为审计日志，设计上只增不减，不计入幂等基线"——两条路选一条，但不能再留假声明。
4. 顺带修 A-02。

### 验收

```
连续运行 3 次 python scripts/test_api.py，每次退出码为 0；
第 2、3 次运行前后的 message / admin_log / item / item_image 行数增量符合预期（或为 0）；
并给出三次的行数记录作为证据。
```

---

## A-02（P1）`test_api.py` 的漏测与硬编码

### 漏测端点（对照 `controller/` 实际存在的 24 个端点）

| 未测端点 | 位置 | 备注 |
|---|---|---|
| `POST /api/auth/register` | `AuthController` | **注册完全没测**（含重复学号 400、`@Valid` 校验） |
| `POST /api/items/{id}/mark-found` | `ItemController` | 脚本文件头注释**声称已补齐**，实际全文无调用 |
| `POST /api/admin/items/{id}/offline` | `AdminController` | 违规下架路径零覆盖 |
| `POST /api/admin/regen-thumbs` | `AdminController` | 文件头声称"7 个 /api/admin/* 正向"，实际只测了 4 个读 + audit + delete |

反向核对结论：**不存在"脚本调用但后端没有"的端点**（这部分是好的，✅）。

### 其他问题

1. **口令 fallback 硬编码**（L39/L40 附近）：
   ```python
   ADMIN_PWD = local.get("admin-password") or "Admin@2026"
   ```
   而**后端读的键名是 `seed.admin-password` / `seed.user-password`**（`DataInitializer` 的 `@Value("${seed.admin-password:}")`）——**键名不一致**。后果：全新 clone（`application-local.yml` 被 gitignore，必然缺失）时，脚本静默用默认口令、后端回退**随机口令** → 脚本必挂，且报错方向误导。
   → 修复：读 `seed.*` 键；读不到时**明确报错退出**，不要 fallback。
2. **无 preflight**：后端未启动时首个 `POST` 直接抛 `ConnectionError` traceback（退出码非 0 但信息误导）。请在开头加一次健康检查（如 `GET /api/stats`），失败时打印"后端未启动，请先运行 …"并退出。
3. 依赖 `uploads/_query/q-cup.jpg` 存在，缺失时同样 traceback → 加存在性检查。
4. 断言数不是 50：实际有 **51 个 `ok(...)` 调用点**（`grep -c 'ok(' ` 减掉 `def ok`），且因分支存在（`if msgs:` / `if recs:` / `else: ok(False,...)`）每次实际执行 47~51 条，脚本**从不打印断言总数** → 无法自证。
   → 修复：在结尾打印 `共 X 条断言，失败 Y 条`（X 为实际执行数），并统一文档口径。
5. 大量断言只看状态码，不看内容（例如 `similar` 只断言 `code==200`，不校验返回内容；`views` 的 `v2>=v1` 恒真近乎无信息量）→ 请挑 5~8 条最关键的补上内容断言。
6. **核心卖点的断言是"同图自证"**：L115 用同一张图发布后再搜自己，距离必然为 0，无法证明对"重拍/变换后"的图片有效。请结合 `03` Prompt 的 `eval_recall.py` 建立真正的召回断言。

### 验收

- 脚本能列出实际执行的断言总数；漏测的 4 个端点补上（含 `offline` 后"前台不可见"、`regen-thumbs` 的返回结构、`register` 的重复学号 400）。
- 删掉硬编码口令 fallback 后，在**有** `application-local.yml` 的机器上仍能跑通。

---

## A-03（P1）补齐核心业务单测

现状：`src/test/java` 只有 3 个类、11 个用例，且**没有任何 Spring 上下文/集成测试**。

| 测试目标 | 现状 | 要求 |
|---|---|---|
| `AuthService` | ✅ 5 个 Mockito 单测（质量最好，含记住我 TTL 的 `verify(jwtUtil).generate(1L,"20260005",0,43200L)`） | 补限流 5 连错 + 锁定期过后的行为（配合 `01` 的 B-04） |
| `PHashUtil` | ⚠️ 5 个，但 `differentImagesFar` 只断言 `dist>0`、`aHashDHashAvailable` 只断言自幂等 → 证据力弱 | 至少补一条"三算法在某类变换下有可区分差异"的断言 |
| `SearchService` / `ItemImageMapper` | ❌ **零覆盖**（headline 功能） | 用 `@Mock` 的 `ItemImageMapper` + `ItemMapper` 断言：审核过滤生效、Top-N 截断、`similarTo` 排除自身、`n<=0` 返回空 |
| `ClaimService` 状态机 | ❌ **零覆盖** | 覆盖 `apply` 白名单守卫、重复申请拒绝、`audit` 已处理过拒绝、物品已完成认领拒绝、approve 时批量拒绝同物品其他申请、reject 时"仅当无其他审核中才回退状态" |
| `FileStorageService.checkMagic` | ❌ 零覆盖 | 用 mock 的 `FileProperties` + `@TempDir`，覆盖 jpg/png/gif/bmp/webp 正例 + "改名 .jpg 的文本"反例 + 路径穿越 `../` 拒绝 |
| `ItemService.publish` | ❌ 零覆盖 | 至少覆盖"无图 → 400"、"图片数量超限 → 400"（配合 `02` 的 B-10） |

> 注意：上述都可以做成**不连数据库**的纯 Mockito 单测（`@TempDir` 处理文件），这样在没有 MySQL 的机器上 `mvn test` 也能全绿——这对答辩机器的容错很重要。

### 验收

`mvn test` 全绿，且用例数从 11 提升到 25+；给出 `Tests run: N, Failures: 0, Errors: 0` 的原始输出。

---

## A-04（P1）`HashExperimentTest` 不是测试

### 问题

1. **没有任何 `assert`**，只写 CSV + 打印表格 → `surefire` 显示 PASS 是假象（它永远不会失败）。
2. **强依赖工作目录**：`Paths.get("uploads/_exp")`、`scripts/exp_*.csv` 都是相对路径 → 必须从 `backend/` 目录运行 Maven，否则 `NoSuchFileException` 或**静默写到错误位置**。
3. **有副作用**：会**覆盖仓库里已提交的** `backend/scripts/exp_results.csv` 与 `exp_per_transform.csv`。

### 修复要求

1. 加前置校验：`uploads/_exp` 不存在时 `Assumptions.assumeTrue(...)` 跳过，或明确 `fail("请先准备 uploads/_exp 图集")`。
2. 加工作目录校验：若 `Paths.get("scripts").toAbsolutePath()` 的父目录不是项目根，则报错提示"请在 backend 目录下运行"。
3. 加真断言：例如"参与实验的查询图数量 == 预期值（mild 30 / strong 42）"、"每个 (组,算法,阈值) 组合都输出了结果"、"pHash 在 `jpeg30` 变换上召回 == 1.00"。
4. 避免覆盖已提交数据：默认输出到 `scripts/`，但若文件已存在则拒绝覆盖（需 `-Dexp.overwrite=true` 才覆盖），或改写到 `target/exp/` 下。
5. 在 `Creator/01-交付文档/11` 的复现章节里补充"必须从 backend 目录运行"的前提。

### 验收

从项目根目录跑 `mvn test` 时该测试应**明确提示目录错误或跳过**，而不是静默写错文件；从 `backend/` 跑时正常输出且断言通过。

---

# B 组 · 文档数字与声明

## B-01（P1）`Creator/01-交付文档/08-测试用例表` 数字与实际不符

### 问题 1：用例总数

文末写：

> 用例总数：**61**（自动化 38 / 手工 15 / 单元 5 / 实验 3 交叉计入），全部通过。

但按表内逐条统计，**实际有 71 条用例**：

| 章节 | 用例数 |
|---|---|
| M1 用户（TC-M1-01~11） | 11 |
| M2 发布（TC-M2-01~10） | 10 |
| M3 以图搜图（TC-M3-01~10） | 10 |
| M4 检索（TC-M4-01~09） | 9 |
| M5 认领（TC-竞争-01/02 + TC-M5-01~12） | 14 |
| M6 后台（TC-管理-01 + TC-M6-01~08） | 9 |
| 前端与通用（TC-FE-01~07 + TC-通用-01） | 8 |
| **合计** | **71** |

按"方式"列统计，自动化约 42 条、手工约 18 条（都不是 38 / 15）。请**重新逐条统计**并把文末数字改对（同时说明"单元 5 / 实验 3 / 代码审查 2 与自动化、手工存在交叉计入"的口径）。

### 问题 2：标为"自动化 ✅"但 `test_api.py` 里没有对应断言

我用脚本里 51 个 `ok(...)` 调用点逐条比对，以下用例的"自动化"标注**不成立**：

| 用例 | 声明方式 | 实际情况 |
|---|---|---|
| TC-M1-02 重复学号注册 | 自动化 | 脚本**从不调用 `/api/auth/register`** |
| TC-M2-07 时间格式非法（`happenTime=abc`） | 自动化 | 无此断言（只测了超长标题、非法 type） |
| TC-M3-06 待审核项不出现在搜索结果 | 自动化 | 无此断言 |
| TC-M4-09 列表只含 `audit=1` | 自动化 | 无此断言 |
| **TC-竞争-01 一物多申请-通过路径** | 自动化 | 脚本只有 B 一个申请人，**没有 C**，竞争场景完全未构造 |
| TC-M5-02 重复申请拦截 | 自动化 | 无此断言 |
| TC-M6-03 审核"驳回" | 自动化 | 只测了通过（`approve=true`） |
| TC-M6-04 违规下架 | 自动化 | 脚本**从不调用 `offline`** |

处理方式（二选一，但必须与实际一致）：

- **补**：把上述断言加进 `test_api.py`（推荐对 TC-M1-02 / TC-竞争-01 / TC-M6-04 补，这三条业务价值最高）；
- **改**：把"方式"列改成"手工"或"待补"。

### 问题 3：标为"单元测试"但没有对应单元测试

| 用例 | 声明方式 | 实际情况 |
|---|---|---|
| TC-M1-06 连错 5 次锁定 | 单元测试 | `AuthServiceTest` 5 个用例里**没有**限流用例 |
| TC-M2-04 假图片魔数拦截 | 单元测试 | `PHashUtilTest` 里**没有** `checkMagic` 测试（脚本 L86-89 传的是真 `.txt`，走的是"无图 400"分支，根本没触发魔数校验） |
| TC-M5-03 认领自己的启事 | 单元测试 | **ClaimService 零单测** |
| TC-M5-04 已下架/已完成不可申请 | 单元测试 | 同上 |

处理方式同问题 2（配合 A-03 补齐后即可保留"单元测试"标注）。

### 问题 4：TC-M2-10 的预期对种子数据不成立

TC-M2-10"缩略图生成"，预期"列表加载 `*_thumb` 小图"，标注"手工"。实测 `backend/uploads/` 下 `*_thumb*` 文件数为 **0**，列表页拿到的都是原图（见 `02` 的 B-08）。修好缩略图机制后本条才能成立。

### 问题 5：TC-M3-02 的预期与实测不符

TC-M3-02"上传 `q-cup.jpg` → 命中该物品**多条** lost/found ~100%"，实测**只有 2 条**（cup 同族已过审启事 8 条）。请按 `03` Prompt 的标定结果改写。

---

## B-02（P1）`Creator/01-交付文档/09` 的回归声明不成立

`Creator/01-交付文档/09-缺陷跟踪表` 文末原文：

> P0 全部集中在核心链路……修复后均有**自动化断言防回归**（`test_api.py` 中对应 TC-M3-02/03/06、TC-竞争-01/02）。

实际：

- TC-M3-06、TC-竞争-01 在脚本里**没有断言**（见 B-01 问题 2）；
- TC-竞争-02 标"代码审查+单元"，但**没有任何 ClaimService 单测**；
- **缺陷 #4（认领"拒绝"路径状态机自相矛盾，P0）目前没有任何自动化或单元回归**。

处理方式：要么补上回归断言（推荐给缺陷 #4 补 `ClaimServiceTest`，见 A-03），要么把这句话改成如实表述（"缺陷 #4 的验证方式为代码审查 + 手工走查，尚无自动化回归，列为技术债"）。

同时把本次修复的 B-01~B-12、F-01~F-11、A-01~A-04 登记为新的缺陷条目（编号接 #9），并写清"回归方式"。

---

## B-03（P1）`Creator/01-交付文档/10-测试报告` 三处不实

| 原文 | 实际 |
|---|---|
| §3"接口：**6 模块全部端点均被自动化覆盖**（含正向+鉴权负向+参数边界）" | 24 个端点里有 4 个未测（见 A-02） |
| §3"幂等性：`test_api` 连跑多轮，**六表行数与上传文件数回基线**（自检断言）" | 自检只覆盖 `item` 与 `item_image`，`message`/`admin_log` 不回基线（见 A-01，有实测反证） |
| §1"接口自动化回归 50 断言……**50/50 PASS**" | 实际 51 个调用点、每次执行 47~51 条、脚本不打印总数（见 A-02） |

另外 §1"功能测试用例（含手工）61 用例 → 61/61 通过"要按 B-01 重新统计。

修法：**按修好后的脚本真实输出回填**，并保留一份原始 `test_api.py` 输出作为附录（这是最有说服力的证据）。

---

## B-04（P1）`Creator/01-交付文档/04-接口文档` 有 5 个端点缺失

逐条核对 `controller/` 后，文档**漏了**：

| 缺失端点 | 出处 |
|---|---|
| `GET /api/items/{id}/similar` | `ItemController`（`n` 默认 3） |
| `POST /api/items/{id}/dm` | `ItemController`（body `{content}`） |
| `POST /api/user/messages/{id}/read` | `UserController`（单条已读） |
| `GET /api/admin/logs` | `AdminController`（`page,size`） |
| `POST /api/admin/regen-thumbs` | `AdminController` |
| （另）`POST /api/search/reindex` 的返回是 `{reindexed,total}` | 文档写的是 `{reindexed,total}` ✅ |

其他不一致：

- `GET /api/admin/stats` 实际返回还包含 **`userCount`** 与 **`dailyTrend`**，文档只写了 `total/reunited/successRate/categoryRatio`。
- `ClaimVO` 实际字段比文档多：**`claimantName`、`claimantAccount`、`proof`、`contact`**（防盲审的关键字段，文档没写）。
- `ItemVO` 实际还有 **`thumb`** 与 **`auditStatus`** 字段，文档的 `ItemVO` 定义里没有。
- 错误码表需要补：**405 方法不匹配**、**未知路径 404**（配合 `02` 的 B-06）；并说明"错误响应的 HTTP 状态码在修复后与 `code` 一致"。
- `Creator/01-交付文档/04` 没写 `/api/items/{id}` 的可见性规则（B-01 修复后需补充"待审核/已下架/已关闭启事仅发布者本人与管理员可见，其他人返回 404"）。

---

## B-05（P1）`Creator/01-交付文档/06` 与 `schema.sql` 的 `idx_phash` 三方矛盾

| 来源 | 说法 |
|---|---|
| `Creator/01-交付文档/06-数据库设计` §索引取舍 | "`item_image.phash` 上**不建普通索引**：检索条件是函数 `BIT_COUNT(phash ^ q)`，普通 B+Tree 索引永远不会被命中（EXPLAIN 已证实），建了只是写放大" ✅ 结论正确 |
| `backend/src/main/resources/db/schema.sql` 第 68 行 | `KEY idx_phash (phash)` ❌ **与文档矛盾** |
| 实际数据库 | `SHOW INDEX FROM item_image` 只有 `PRIMARY` 与 `idx_item` → **没有 `idx_phash`** |

后果：按 `schema.sql` 新建库会多出一个文档明确说不要的索引，三方不一致。

### 修复要求

1. **从 `schema.sql` 删除 `KEY idx_phash (phash)`**（这是唯一自洽的选择，因为文档的理由充分且被 EXPLAIN 证实）。
2. 在 `schema.sql` 该位置加注释说明"phash 不建索引，理由见 Creator/01-交付文档/06 §索引取舍"。
3. 顺手核对 `Creator/01-交付文档/06` 的数据字典与 `schema.sql` 的**其余部分**是否逐列一致（列名、类型、长度、NULL、默认值、注释），把发现的其它差异一并列出并修正。
4. 明确 `Creator/01-交付文档/06` 里"无物理外键（审查 2.36），一致性由 Service 层 `@Transactional` 保证"这条：**对文件系统不成立**（见 `02` 的 B-11），请在文档里补一句限定："数据库层面的事务一致性；磁盘文件的清理为补偿式，可能存在孤儿文件"。

---

## B-06（P1）若干口径错误

| 位置 | 现在写的 | 实际 |
|---|---|---|
| `Creator/01-交付文档/03-项目计划书` WBS、"M3 可运行系统" | "前端**八页** + 管理后台" | 9 个 view（含 `NotFoundView`），README/项目结构写的是"九页" |
| `Creator/01-交付文档/07-测试计划` §1 | "前端**八页面**主流程" | 同上 |
| `00-项目知识点与结构详解-新手版.md` L314 | "用 Python+PIL **下载**底图" | `gen_seed_images.py` 是**纯离线**读 `uploads/_src`（脚本自己注释"无外网依赖"） |
| `README.md`、`项目结构.md`、`Creator/01-交付文档/06`、`Creator/01-交付文档/07` | "**120 张图**" / "演示图 120 张" | 实际是 **120 条图片引用 / 30 个去重图片文件**（实测 `uploads/seed/` 下 30 个 jpg，多启事复用同图） |
| `Creator/01-交付文档/02` 活动图 | "前端校验 必填/**图片≥1**/≤5MB" | `PublishView` 的 `rules` **没有图片必填校验**（只在服务端），配合 F-04 修 |
| `Creator/01-交付文档/02` 类图 | 只列了部分字段 | 属"核心域模型"简化，可接受；但请在文档里注明"为可读性省略了时间戳等字段"，避免被质疑 |

---

## B-07（P2）`项目结构.md` 的状态过时

1. "待做 ⬜ git 建仓首提交（.gitignore 已备好……由组员自行执行）"——**项目已经在 `D:\Visual_Stdio_Code\Dai_ma\软件工程` 这个 git 仓库中被跟踪**（2 次提交 `verison-01` / `version-02`，lost-found 下 240 个跟踪文件）。请更新为实际状态，并补充"当前跟踪文件含 `_admintoken.txt` 等应当移除的文件（见 C-01）"。
2. `mock/data.js 仅保留常量 CATEGORIES/LOCATIONS/catEmoji` —— 与实际不符（见 `04` F-09）。
3. 前端文件清单需要补：`admin/ToolsView.vue`（F-10 新增）、`constants/catalog.js`（F-09 改名）。
4. 后端文件清单需要补：`dto/DmReq.java`（`02` B-10 新增）、`scripts/eval_recall.py` + `recall_results.csv`（`03` 新增）、新增的单测类（A-03）。
5. `startup`/待做清单里的"演示视频录制""答辩 PPT""JMeter 并发压测"保持，并补上本次修复项。

---

## B-08（P2）交付物厚度：`Creator/01-交付文档/05` 没有设计稿，`Creator/01-交付文档/02` 没有导出图

- `Creator/01-交付文档/05-UI设计稿与规范` 文档自己写着"以已实现界面为准的**设计规格说明**（墨刀/Figma 高保真稿**可依本文档 1:1 复刻**）；**截图以运行系统为准**（`npm run dev` 后逐页截取）" → 即：**高保真稿没有，截图也没有**。如果课程要求提交设计稿/截图，这是缺项。
- `Creator/01-交付文档/02-UML建模全套` 的 5 张图全是 **Mermaid 源码**，没有导出的 PNG/SVG。若老师要求图片形式，同样缺项。

### 修复要求

1. 产出**至少 6 张关键页面截图**（落地页、首页、失物墙、以图搜图结果、发布页、后台看板），放到 `Creator/01-交付文档/images/`，并在 `Creator/01-交付文档/05` 里引用。
   - 截图必须在**真实运行**的环境下拍（后端 + 前端都起着，数据是 60 条种子）。
   - 以图搜图那张要拍到相似度与耗时，这是核心亮点。
2. 把 `Creator/01-交付文档/02` 的 Mermaid 导出为图片（VSCode Mermaid 插件 / `mermaid-cli` 都可），放到 `docs/images/` 并在文档里同时保留源码。
3. 如果课程只要求 Markdown，请在 `Creator/01-交付文档/05`/`Creator/01-交付文档/02` 顶部**明确注明**"本交付物为文本规格 + Mermaid 源码，未附截图/图片"，避免被认为遗漏。

---

# C 组 · 仓库卫生与安全

## C-01（P1）真实 JWT 与响应 dump 已进 git

实测（`git ls-files` 命中）：

```
TRACKED   lost-found/backend/_admintoken.txt     ← 一枚真实签发的管理员 JWT
TRACKED   lost-found/backend/_r.json             ← 接口响应 dump（含 6 条启事数据）
untracked lost-found/backend/_t.log / _run.log / _verify.log / frontend/_dev.log   ← 已被 *.log 忽略
untracked lost-found/backend/application-local.yml                                  ← 已被忽略（正确）
```

`_admintoken.txt` 内容是一枚 `HS512` 签名的 JWT，payload 为 `{"sub":"1","account":"10000001","role":1,...}`。虽然 `exp` 已过期（2026-10-15），但**这属于凭据泄露**，且与 `Creator/01-交付文档/03` 风险表 R5"敏感信息进 git 已消除"、`Creator/01-交付文档/08` TC-通用-01"敏感信息不进库 ✅"相矛盾。

### 修复要求

1. `git rm --cached lost-found/backend/_admintoken.txt lost-found/backend/_r.json`，并删除文件本体。
2. 在 `lost-found/.gitignore` 追加：

```gitignore
# 运行/调试残留（不得入库）
backend/_*.txt
backend/_*.json
backend/backup/
docs/images/_tmp/
```

3. 因为该 JWT 的签名密钥就在 `application-local.yml` 里（gitignored），**泄露的实际影响有限**，但请在 `Creator/01-交付文档/09-缺陷跟踪表` 里登记为一条真实缺陷（如"调试残留凭据入库"），并说明处置。
4. 检查是否还有其它"应当被忽略却没被忽略"的路径（例如 `backend/scripts/recall_results.csv` 是否要入库——建议入库，它是评测证据）。

## C-02（P2）实验图集入库的取舍

`lost-found/backend/uploads/` 下 **121 个文件已被跟踪**，其中 `_exp/` 有 **78 张实验变体图**（`ref.jpg` + `pos_*` + `mild_*`，共 6 个物品 × 13 张）。

- 好处：`HashExperimentTest` 开箱即跑、实验可复现；
- 代价：仓库体积。

请**明确表态**并落到文档：如果保留，在 `Creator/01-交付文档/11` 说明"实验图集已入库以保证可复现"；如果要瘦身，则改由 `gen_seed_images.py` 生成并 gitignore（但会破坏"开箱可复现"）。**不要既不决定也不说明。**

## C-03（P1）`backup.sh` / `backup.md` 的硬伤

| # | 问题 | 证据 |
|---|---|---|
| 1 | `backup.md` 的恢复命令用占位符 `lost_found_YYYYMMDD.sql`，而 `backup.sh` 实际生成的是 `lost_found_$(date +%Y%m%d_%H%M%S).sql` → **照抄必然"文件不存在"** | `backup.sh` 里 `TS=$(date +%Y%m%d_%H%M%S)` |
| 2 | `backup.sh` 用 `sed` 从 `application-local.yml` 取口令，取不到时**静默 fallback 成空口令** → `mysqldump` 会**交互式卡住**（无人值守场景表现为挂死） | `sed -nE 's/^[[:space:]]*username:...'` + `DB_USER:-root` |
| 3 | `backup.md` 的 Windows 部分是 **`.bat` 语法**（`%date:~0,4%`）与 `tar czf` 混写，照抄不可用 | `backup.md` §"Windows 等价命令" |
| 4 | 恢复流程只有 `CREATE DATABASE IF NOT EXISTS`，**没有 DROP**，而 dump 不带 `DROP TABLE` → 恢复到非空库必然 `ERROR 1050 Table already exists` | `backup.md` §恢复 |
| 5 | `backup/` 未进 `.gitignore`（脚本自己注释里提醒过"请勿进 git"，但没落实） | `git check-ignore backend/backup` → NOT ignored（见 C-01） |
| 6 | `data.sql`（可选的导入方案 a）带 `SET @@GLOBAL.GTID_PURGED=...`（需 SUPER 权限）与 `LOCK TABLES`，普通账号导入会报错 | 文件第 24 行 |

### 修复要求

1. `backup.md` 的文件名占位符改为实际格式，并给出一段**可直接复制**的命令（含真实示例文件名）。
2. `backup.sh` 取不到凭据时**必须报错退出**（`echo` + `exit 1`），不要静默空口令。
3. 恢复流程改为：`DROP DATABASE IF EXISTS lost_found; CREATE DATABASE lost_found ...;` 再导入；并说明这是**破坏性操作**。
4. Windows 部分要么改成 PowerShell 命令（与本机一致），要么明确标注"需 Git-Bash / WSL"。
5. `.gitignore` 加 `backend/backup/`（同 C-01）。
6. `Creator/01-交付文档/12-部署手册` 的"上线前检查清单"里"执行过一次 `scripts/backup.sh` 并验证可恢复"应改成**必须实际演练过恢复**（当前流程按字面走不通，等于没验证）。

## C-04（P2）`Creator/01-交付文档/03` 风险状态与实际不符

| 风险 | 声明 | 实际 |
|---|---|---|
| R5 敏感信息进 git | **已消除** | `_admintoken.txt`（真实 JWT）已入库（见 C-01）→ 应改为"已缓解，仍有一条调试凭据残留，处置中" |
| R7 演示设备环境差异 | **已缓解**（"部署手册 12 统一 MAVEN_HOME/JAVA_HOME"） | 本机持久 `JAVA_HOME` 指向 **JDK 25**，不显式 `set JAVA_HOME=...\JDK21` 就编译失败（Lombok 报一堆"找不到符号"），而 `mvn -v` 默认输出 Java 25，与 `启动步骤-MarkDone.md` 环境表写的"JDK 21.0.10"不一致 → 应改为"已缓解但存在踩坑风险，需在演示前 `mvn -v` 确认" |
| M5 验收里程碑 | "视频/PPT **进行中**" | 这是唯一真实的未完成项，保持 🔄 即可，但建议补上预计完成时间 |

---

# D 组 · 澄清（防止误改）

## D-01 `Creator/01-交付文档/11` 的实验数据是**正确**的，不要动

`backend/scripts/exp_results.csv`（30 行）与 `exp_per_transform.csv`（12 行）经独立复核：

- **可逐字节复现**（用仓库自带的 JImageHash 1.0.0 重算，结果完全一致）；
- 两个文件**内部自洽**（逐变换均值 = 组级 recall@10，差异 ≤0.002）；
- 行数与图集实际内容吻合（30 = 2 组 × 3 算法 × 5 阈值；12 = mild 5 变换 + strong 7 变换；查询数 30 = 5×6、42 = 7×6）；
- `Creator/01-交付文档/11` §2.1 的表里标"F1"的数值 = P = R（除 `strong,aHash,12` 一行为 `0.7976/0.8095/0.8035`），与 CSV 一致；§3.2 引用的 `Precision(0.798)<Recall(0.810)` 也对得上；
- **没有发现四舍五入造假、行数错位或占位符**。

只有两点可以改进（属"表述"而非"数据"）：

1. §3.2 用"aHash 在 t=12 出现 Precision<Recall 的跨物体误检分化"来解释"为什么仍选 pHash"——这个论据只来自**一行数据**，说服力偏弱。建议补一句诚实表述："在 6 对象小库上 aHash 数字略优；选择 pHash 主要基于其对缩放/压缩的稳定性与文献共识，本实验规模不足以给出统计显著结论。"
2. 本实验的指标定义下 **P=R 等价于"每次命中只检索到 1 个物体"**，这种"漂亮"有伪装性。建议在 §1.2 或 §3 明确说明，避免答辩时被认为在美化指标。

**禁止**：修改 `exp_*.csv` 的任何数值、修改 `HashExperimentTest` 的算法参数以"让 pHash 更好看"。`03` Prompt 引入的新评测是**第三套协议**，必须作为**新增章节**写入 `Creator/01-交付文档/11`，并明确标注协议差异。

---

# 交付要求

1. 分 A/B/C/D 四组逐条交付：改动文件 + 关键内容 + 验证方式。
2. 文档类改动请**同时**更新 `README.md`、`项目结构.md`、`Creator/01-交付文档/04`、`Creator/01-交付文档/08`、`Creator/01-交付文档/09`、`Creator/01-交付文档/10`、`Creator/01-交付文档/11`（新增章节）、`Creator/01-交付文档/12`、`Creator/01-交付文档/03`、`00-项目知识点与结构详解-新手版.md` 中受影响的部分——**不允许改了代码却留下旧数字**。
3. 最后给出一份"修复前后对照表"（问题 / 修复前 / 修复后 / 证据），可直接作为答辩材料的"质量保障过程"章节。
4. **不要**改 `backend/uploads/seed|_src|_query|_exp/` 下的图片与 `manifest.json` 的图片路径；**不要**改 `exp_*.csv`。
