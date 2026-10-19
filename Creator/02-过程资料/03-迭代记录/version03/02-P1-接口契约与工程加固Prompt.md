# Prompt 02 · P1 后端加固：接口契约、反向代理、缩略图、种子数据、参数校验

> 前置：先读同目录 `00-索引与执行顺序.md`（项目路径、JAVA_HOME 红线、验收命令）。
> 本 Prompt 覆盖后端 7 个 P1 问题，均有实测证据。

---

## 任务总览

| ID | 严重度 | 标题 | 主要文件 |
|---|---|---|---|
| B-06 | P1 | 未知路径/方法不匹配返回 500；HTTP 状态码与业务 code 恒不匹配 | `GlobalExceptionHandler.java`、`common/Result.java` |
| B-07 | P1 | `viewDedup` 无界增长；反代后取不到真实 IP | `ItemService.java`、`application.yml` |
| B-08 | P1 | 缩略图机制对种子数据不生效；`regen-thumbs` 会污染已提交目录且无 UI 入口 | `FileStorageService.java`、`AdminService.java` |
| B-09 | P1 | 种子数据自相矛盾（违反自身业务不变量 + 待审却已团聚） | `DataInitializer.java` |
| B-10 | P1 | 参数校验缺口：私信长度、审核意见长度、图片数量上限 | `ItemService.java`、`ClaimAuditReq.java`、`ClaimController.java` |
| B-11 | P1 | 上传文件与数据库事务不一致，回滚留下孤儿文件 | `ItemService.publish`、`FileStorageService` |
| B-12 | P2 | JWT 拦截器对已删除用户的边界处理 | `JwtInterceptor.java`、`AuthController.me` |

---

## B-06（P1）404/405 与 HTTP 状态码

### 现象与证据（实测）

```
GET    /api/not-exist   → HTTP 200, {"code":500,"msg":"服务器繁忙，请稍后重试"}   ← 应为 404
POST   /api/items/my    → HTTP 200, {"code":500,...}                            ← 方法不匹配，应为 405
PUT    /api/items       → HTTP 200, {"code":500,...}                            ← 应为 405
DELETE /api/items/1     → HTTP 200, {"code":500,...}                            ← 应为 405
GET    /uploads/..%2fapplication-local.yml → HTTP 400 + Tomcat 原生 HTML 错误页  ← 非统一 JSON
```

`GlobalExceptionHandler` 缺少 `HttpRequestMethodNotSupportedException` 与"无 handler"异常的处理，全部落进兜底的 `@ExceptionHandler(Exception.class)` → 500。同时 `Result.fail(...)` 没有 `@ResponseStatus`，所以**所有业务错误体的 HTTP 状态码恒为 200**，`Creator/01-交付文档/10-测试报告` 把它当"既定契约"，但容器级错误返回的是 HTML，契约并不真的统一。

### 修复要求

1. 补专项 handler，返回正确的业务 code **与** 匹配的 HTTP 状态：
   - `org.springframework.web.servlet.resource.NoResourceFoundException`（Spring Boot 3.2+ 的默认无 handler 异常）→ 404
   - `org.springframework.web.HttpRequestMethodNotSupportedException` → 405
   - `org.springframework.web.servlet.NoHandlerFoundException`（若启用 `throw-exception-if-no-handler-found`）→ 404
2. 让 HTTP 状态码与 `code` 一致。推荐做法：在 `Result.fail` 的各个 handler 上使用 `@ResponseStatus` 或直接返回 `ResponseEntity<Result<Void>>`，把 `code` 映射为 HTTP status（400→400、401→401、403→403、404→404、413→413、500→500）。
3. **兼容性已确认，可以放心改**（我核对过两端）：
   - 前端 `frontend/src/api/index.js` 的 axios 响应拦截器在错误分支里读 `err.response.data.msg`，并且用 `body?.code === 401 || err.response?.status === 401` 双条件判断清登录态 → 状态码改成真实值**不会**破坏 401 跳转与错误提示。
   - `scripts/test_api.py` 全程只读 JSON body 的 `code`（`r.json()["code"]`），**从不检查 `r.status_code`** → 不受影响。
4. 可选但推荐：给容器级错误也统一成 JSON（配置 `server.error.whitelabel.enabled=false` 无法完全覆盖；实际上 Tomcat 在 URL 层面就拒绝了含 `%2f` 的请求，这类保持原样即可，但请在 `Creator/01-交付文档/04` 的错误码表里说明）。

### 验收标准

```powershell
curl.exe -s -o - -w "`n[HTTP %{http_code}]`n" http://localhost:8080/api/not-exist
# 期望 HTTP 404 + {"code":404,...}
curl.exe -s -o - -w "`n[HTTP %{http_code}]`n" -X POST http://localhost:8080/api/items/my
# 期望 HTTP 405 + {"code":405,...}
curl.exe -s -o - -w "`n[HTTP %{http_code}]`n" http://localhost:8080/api/items/999999
# 期望 HTTP 404 + {"code":404,"msg":"启事不存在或已被删除"}
# 回归：确认 401/403 仍正确，且 test_api.py 退出码 0
```

---

## B-07（P1）`viewDedup` 无界增长 + 反代后拿不到真实 IP

### 现象与根因

`ItemService`：

```java
private static final long VIEW_DEDUP_MS = 30 * 60 * 1000L;
private final ConcurrentHashMap<String, Long> viewDedup = new ConcurrentHashMap<>();

String dedupKey = id + "|" + (clientKey == null ? "_" : clientKey);
```

1. 这个 map **只增不减**（没有任何清理逻辑），长期运行会持续吃内存。
2. `ItemController.detail` 用 `request.getRemoteAddr()` 作为匿名访客标识：

```java
String clientKey = uid != null ? ("u" + uid) : request.getRemoteAddr();
```

而 `Creator/01-交付文档/12-部署手册` 明确推荐 **Nginx 反向代理**，`application.yml` **没有配置 `server.forward-headers-strategy`** → 反代后所有匿名访客的 `getRemoteAddr()` 都是 Nginx 的地址（如 `127.0.0.1`），30 分钟去重退化成"全站共用一把 key"，浏览量几乎不涨。

### 修复要求

1. 给 `viewDedup` 加清理机制。推荐：把 `Long` 值改为"过期时间戳"的封装（或用 Caffeine 之类的轻量缓存——**但项目约束是不引入新依赖**，所以请用一个简单方案）：
   - 每次写入前，若 `map.size() > 阈值`（如 10000）则惰性清理已过期条目（`ConcurrentHashMap` 支持 `entrySet().removeIf(...)`）；
   - 或用一个 `ScheduledExecutorService` 每 10 分钟清理一次过期项（注意线程池优雅关闭）。
2. 让匿名访客标识在反向代理后仍然有效：
   - **方案 A（推荐）**：在 `application.yml` 加 `server.forward-headers-strategy: native`（Tomcat 的 `RemoteIpValve`），让它按 `X-Forwarded-For` 重写 `getRemoteAddr()`；
   - **方案 B**：显式读 `X-Forwarded-For` / `X-Real-IP` 头（只在可信代理后使用，注意取第一个非内网地址，防止伪造）。
   - 无论哪种，都必须在 `Creator/01-交付文档/12-部署手册` 的 Nginx 配置里补上 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;`（现在只设了 `X-Real-IP`）。
3. 顺带：登录即可用的用户用 `"u"+uid` 已经是好设计，保留。

### 验收标准

- 用 `X-Forwarded-For: 1.2.3.4` 与 `5.6.7.8` 分别请求同一条启事详情，`views` 应各 +1（证明不同 IP 不被误判为同一访客）。
- 用同一个 `X-Forwarded-For` 连续请求两次，`views` 只 +1（去重仍生效）。
- 说明清理策略生效的测试方式（例如把阈值临时调小，观察 map 大小回落）。

---

## B-08（P1）缩略图机制形同虚设

### 现象与证据

1. `FileStorageService.thumbUrl(url)` 的实现是"**推导 `_thumb` URL 后 stat 一下文件是否存在**，不存在就回退原图"。
2. 实测：`backend/uploads/` 下 `*_thumb*` 文件数为 **0**（种子图从未生成过缩略图）→ **所有列表页拿到的都是原图**，`Creator/01-交付文档/08` TC-M2-10"列表加载 `*_thumb` 小图 ✅"对种子数据不成立。
3. `regen-thumbs`（`FileStorageService.ensureThumbs()`）会遍历 `uploads/` 并**在 `uploads/seed/` 里生成几十个新文件**——而 `uploads/seed/` 是**已经提交进 git 的目录**（121 个 tracked 文件），会产生大量 git 噪音。且该接口**前端没有任何入口**（`api.adminRegenThumbs` / `api.adminReindex` 在 `frontend/src/api/index.js` 里定义了但无人调用）。
4. `thumbUrl()` 是在 `ItemService.toVO()` 里逐条调用的 → 一页 100 条就是 100 次文件系统 stat（N+1 磁盘 IO）。

### 修复要求

1. 生成策略二选一（都要保证"缩略图真的存在"）：
   - **A**：`DataInitializer` 播种完成后调用一次 `ensureThumbs()`，让种子的 `_thumb` 一并生成；但要把产物纳入 git 或明确 gitignore（见 `05` Prompt 的 G-07）。
   - **B（更推荐）**：不做磁盘缩略图，改为**运行时按需生成**（首次请求时生成并缓存）或用 URL 参数走图片压缩——但方案 B 改动大，课程项目下建议选 A 并把 `uploads/seed/**/*_thumb.*` 讨论清楚是否入库。
2. 消除 N+1：把 `thumbUrl` 的存在性判断结果做一层**内存缓存**（`Map<String, Boolean>`，同样要有上限/清理），或在 SQL 层直接把 `thumb` 一起查出来。
3. `regen-thumbs` 的写入范围要收敛：只在 `uploads/yyyy/MM/`（用户上传目录）生成，**不要**遍历 `seed/`、`_src/`、`_query/`、`_exp/`。
4. 给管理员工具补前端入口（与 `04` Prompt 联动）：在 `frontend/src/views/admin/` 的看板页或新增一个"运维"页签里放两个按钮，调用已有的 `api.adminRegenThumbs()` / `api.adminReindex()`，并展示返回的 `generated` / `reindexed` 数字。
5. 更新 `Creator/01-交付文档/08` TC-M2-10 的"方式/预期"，使其与实际一致。

### 验收标准

- 列表接口返回的 `thumb` 字段指向的文件**确实存在**（抽样 5 条用 `curl -I` 验证 HTTP 200）。
- `POST /api/admin/regen-thumbs`（管理员）返回的 `generated` 数与磁盘新增文件数一致，且 `uploads/seed/` **不再被写入**（用 `git status` 验证无新增）。
- 前端后台能看到并触发这两个工具。

---

## B-09（P1）种子数据自相矛盾

### 现象与证据（实测）

`DataInitializer.seedRelations` 对前 3 条启事各插 3 条认领记录：

```java
String[] statuses = {"审核中", "已通过", "已拒绝"};
Long[] claimants = {c1, c2, c1};      // ← k=0 与 k=2 是同一个申请人 c1
```

问题 1：**同一个 `(item_id, claimant_id)` 出现两条**（"审核中" + "已拒绝"），直接违反 `ClaimService.apply` 自己的不变量：

```java
Long dup = claimMapper.selectCount(new QueryWrapper<ClaimRecord>()
        .eq("item_id", itemId).eq("claimant_id", claimantId).eq("status", "审核中"));
if (dup != null && dup > 0) throw new BusinessException("你已提交过认领申请，请等待发布者审核");
```

问题 2：实测 4 条待审核启事（`audit_status = 0`，id = 9/10/19/20）的 `status` 是 **"已找回" / "已认领"**——即"还没过审"却"已经团聚"。这让首页 `reunited=15` 与墙页可见的 11 条对不上（B-02 的一半原因）。

### 修复要求

1. `seedRelations` 里同一物品的 3 条认领记录必须使用**互不相同的申请人**（至少 3 个非发布人账号；账号池有 20 个，足够）。且"已通过"的那条要把物品状态置为已找回/已认领——保持现有逻辑。
2. 待审核（`audit_status = 0`）的启事，其 `status` **不能**是终态（已找回/已认领）。请在 `DataInitializer` 里加一条规范化：
   - 若 `auditStatus != 1`，则 `status` 强制为业务初始状态（`lost → 寻找中`、`found → 待认领`）；
   - 并同步修正 `uploads/seed/manifest.json` 里对应 4 条的 `status` 字段（改数据文件，不改图片）。
3. 修完后必须重新播种验证（`TRUNCATE` 六表后重启，或提供一段校验 SQL）。
4. 更新 `Creator/01-交付文档/06` 与 `项目结构.md` 里"60 启事 / 4 条待审核"的描述，补充"待审启事的业务状态一律为初始状态"。

### 验收标准

```sql
-- 1) 不存在同一 (item_id, claimant_id) 的多条记录
SELECT item_id, claimant_id, COUNT(*) c FROM claim_record GROUP BY item_id, claimant_id HAVING c > 1;   -- 期望空
-- 2) 待审核启事的 status 不是终态
SELECT id, audit_status, status FROM item WHERE audit_status <> 1;   -- 期望 status 只有 寻找中/待认领
-- 3) 六表行数符合预期
SELECT (SELECT COUNT(*) FROM item), (SELECT COUNT(*) FROM item_image), (SELECT COUNT(*) FROM claim_record);
```

---

## B-10（P1）参数校验缺口

### 现象

| 位置 | 问题 |
|---|---|
| `ItemController.dm` | 请求体是裸 `Map<String,String>`，**没有任何校验**；`ItemService.sendDm` 只做 `isBlank` 判断。而 `message.content` 是 `VARCHAR(300)`，且服务端还要拼前缀（"同学 就「标题」给你留言："）→ 长内容会撞 `DataIntegrityViolationException`，返回的是笼统的 400"提交内容超出字段长度" |
| `ClaimAuditReq` | `reply` 字段**没有 `@Size`**，而 `claim_record.reply` 是 `VARCHAR(200)`；且 `ClaimController.audit` 方法参数上**没有 `@Valid`**，即使加了注解也不会生效 |
| `ItemService.publish` | 只校验了"至少 1 张图"，**没有上限**。SRS FR2.1 写的是"图片 1–3 张"，但只有前端 `el-upload :limit="3"` 管着，后端传 20 张也照收（单张 ≤5MB、整体 ≤20MB 是唯一约束） |

### 修复要求

1. 新建 `dto/DmReq.java`（`@NotBlank` + `@Size(max = 200)`），`ItemController.dm` 改用 `@RequestBody @Valid DmReq`，并在 `sendDm` 里按"前缀 + 内容"的总长度做一次截断或校验（不超过 300）。
2. `ClaimAuditReq.reply` 加 `@Size(max = 200, message = "回复最多 200 字")`，`ClaimController.audit` 参数加 `@Valid`。
3. `ItemService.publish` 加图片数量上限校验（与 SRS 一致取 3，或改成与产品确认后的值），超出时抛 `BusinessException("最多上传 3 张图片")`，并把 `files` 数组长度检查放在读取字节之前（避免无效 IO）。
4. 顺手核对其它 DTO 的长度注解与 `schema.sql` 列宽是否一一对应（`ItemForm` 已经对齐，可作为模板）。
5. 同步更新 `Creator/01-交付文档/04-接口文档-v1.0.md`（`/items/{id}/dm` 的入参与长度约束）。

### 验收标准

```powershell
# 私信超长（>200 字）→ 400 且提示明确
# 审核回复超长 → 400
# 发布 4 张图 → 400「最多上传 3 张图片」
# 发布 3 张图 → 200
```

---

## B-11（P1）上传文件与数据库事务不一致

### 现象与根因

`ItemService.publish` 标了 `@Transactional`，但 `fileStorage.store(bytes, filename)` 在事务内**直接写磁盘**：

```java
String url = fileStorage.store(bytes, f.getOriginalFilename());
...
itemImageMapper.insert(img);   // 若此处或后续任何一步抛异常 → 事务回滚，磁盘文件残留
...
if (stored == 0) throw new BusinessException("请至少上传一张有效的物品图片");  // 回滚，文件已落盘
```

`Creator/01-交付文档/06` 明确写了"无物理外键，一致性由 Service 层 `@Transactional` 保证"——但这个论断**对文件系统不成立**。

### 修复要求

1. 在 `publish` 内做补偿：把本次事务中已落盘的 URL 收集到一个局部 `List<String>`，捕获异常时在 `catch` 中调用 `fileStorage.deleteQuietly(url)` 逐个清理，然后重新抛出。
2. 或者在 `@Transactional` 外层包一层"编排"方法：内层提交成功后再落盘（代价是 pHash 计算需要字节，可以在内存中算完再落盘 —— 现有代码已经是"读一次字节，既写盘又算哈希"，改造量不大）。
3. 同样的问题存在于 `AdminService.deleteItem`（先删库再删文件，删文件失败只记日志）——这是可接受的（宁可留孤儿文件也不能丢库记录），但请在注释里说明取舍。
4. 补一个"孤儿文件巡检"的小工具（可选）：列出 `uploads/yyyy/MM/` 下没有被 `item_image.img_url` 引用的文件，方便手工清理。

### 验收标准

- 构造一次失败发布（例如传 3 张图，其中第 2 张是假图 —— 注意当前实现是"跳过失败张"而非整体失败，请先明确语义）：
  - 若语义是"有失败张就整体失败" → 断言 `uploads/` 下无新增文件；
  - 若语义是"跳过坏图、保留好图" → 在注释与 `Creator/01-交付文档/04` 里写明，并去掉 `stored == 0` 之外的多余回滚路径。

---

## B-12（P2）JWT 拦截器对已删除用户的边界

### 现象与根因

`JwtInterceptor.preHandle`：

```java
Long uid = Long.valueOf(claims.getSubject());
User u = userMapper.selectById(uid);
if (u != null && u.getStatus() != null && u.getStatus() == 0) {
    return true;  // 封禁者等同匿名
}
Integer role = (u != null ? u.getRole() : claims.get("role", Integer.class));
UserContext.set(uid, role == null ? 0 : role);
```

用户被**删除**（`u == null`）时，仍然会把 `claims` 里的 `role` 写进上下文 → 该 token 在有效期内依然可用。若该用户曾把 role 提升为 1，后果更严重。
另外 `AuthController.me()` 直接 `userMapper.selectById(uid)` 后 `u.getId()`，用户不存在时会 NPE → 500。

虽然当前版本没有"删除用户"的接口，属潜在缺陷，但修复成本极低。

### 修复要求

1. `u == null` 时**不写上下文**（等同匿名），与封禁处理保持一致。
2. `AuthController.me()` 增加空值判断，返回 `401 请先登录`。
3. 补一条注释说明"用户被删除后 token 立即失效"的语义。

### 验收标准

- 单测或手工：直接在库里删除一个用户后，用其旧 token 访问 `/api/auth/me` → 401 而不是 500。

---

## 交付要求

1. 逐条给出：改动文件 + 关键 diff 摘要 + 验证命令 + 实际输出。
2. 全部改完后跑 `00-索引与执行顺序.md` 第六节通用验收命令；`python scripts/test_api.py` 退出码必须为 0。
3. 更新 `Creator/01-交付文档/04-接口文档-v1.0.md`（错误码表、新增 DTO 约束）、`Creator/01-交付文档/12-部署手册-v1.0.md`（`X-Forwarded-For`、`forward-headers-strategy`）、`Creator/01-交付文档/09-缺陷跟踪表`（登记 B-06~B-12）。
4. **不要**改 `uploads/seed/`、`_src/`、`_query/`、`_exp/` 的图片与 `manifest.json` 的图片路径部分（B-09 只允许改 4 条 `status` 字段）。
