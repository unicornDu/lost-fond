# Prompt 01 · P0/P1 后端修复：内容泄露、统计口径、状态机、限流、删除清理

> 前置：先读同目录 `00-索引与执行顺序.md`（项目路径、JAVA_HOME 红线、验收命令）。
> 本 Prompt 只覆盖 **后端** 的 5 个问题，全部有实测证据，请逐条修复并给出验证输出。

---

## 任务总览

| ID | 严重度 | 标题 | 主要文件 |
|---|---|---|---|
| B-01 | **P0** | 待审核/已关闭启事可通过详情页与相似推荐直接看到 | `ItemService.java`、`SearchService.java`、`ItemController.java` |
| B-02 | **P0** | 首页统计与墙页总数口径不自洽 | `StatsService.java`、`ItemMapper.java`、`ItemService.java` |
| B-03 | P1 | 已驳回启事再次"通过"后仍永久隐藏 | `AdminService.java` |
| B-04 | P1 | 登录限流：锁定期过后错 1 次即再次锁定 | `AuthService.java` |
| B-05 | P1 | 删除启事只清 `claim` 消息，`dm` 私信成为悬挂数据 | `AdminService.java` |

---

## B-01（P0）待审核启事可通过详情/相似推荐直接访问

### 现象与证据（实测）

```
GET /api/items/9   → code=200  auditStatus=0  status=已找回  images=2   ← 待审核却能看到全部图片
GET /api/items/10  → code=200  auditStatus=0  status=已认领  images=2
GET /api/items/19  → code=200  auditStatus=0  status=已找回  images=2
GET /api/items/20  → code=200  auditStatus=0  status=已认领  images=2
GET /api/items/9/similar?n=3 → code=200 条数=3
```

### 根因

审核过滤只做在了两处：

- 搜图 SQL：`ItemImageMapper.searchByHash` 里有 `JOIN item ... i.audit_status = 1 AND i.status NOT IN ('已下架','已关闭')`（✅ 正确）
- 列表：`ItemService.list` 里 `qw.eq("audit_status", 1)` + `qw.notIn("status", HIDDEN)`（✅ 正确）

但**详情与相似推荐完全没有校验**：

- `ItemService.detail(Long id, Long currentUserId, String clientKey)` — 直接 `selectById` 后组装 VO，没有任何 audit/status 判断
- `SearchService.similarTo(Long itemId, int n)` — 取源启事封面图 phash 后复用 `searchByHash`，**没有校验源启事本身是否可见**；所以对一条待审核启事调用相似推荐，会通过它把库内其他启事"漏"出来

前端 `/item/:id` 路由虽需登录，但 id 可从站内消息 `refId`、私信、或直接枚举获得。

### 修复要求

1. 在 `ItemService.detail` 中，组装 VO 之前先做可见性判定：
   - `auditStatus != 1` → 抛 `BusinessException(404, "启事不存在或已被删除")`（**用 404 而不是 403**，避免通过状态码探测出"这条启事存在但未过审"，即不要产生信息泄露）。
   - `status` 属于 `已下架 / 已关闭` → 同上。
   - **例外**：发布者本人（`item.userId.equals(currentUserId)`）与管理员（`UserContext.isAdmin()`）应仍能看到自己的/待审的启事，否则个人中心"我的发布"里的待审核条目点进去会 404。请把当前用户身份与角色传进去做判断。
2. 在 `SearchService.similarTo` 中对**源启事**做同样的可见性判定；不可见时直接返回 `List.of()`（相似推荐是增强功能，不要抛错影响详情页）。
3. 顺带检查是否还有别的地方绕过了审核过滤：
   - `SearchService.searchByHash` 之后的 Java 层过滤（`isHidden` + `auditStatus != 1`）保留，作为 SQL 之外的第二道防线；
   - `ItemService.myPublishes` 保持不过滤（本人查看自己的发布，正确）。
4. 为 B-01 增加自动化回归断言（可加进 `scripts/test_api.py`，或按 `05` 的建议加 Java 测试）。

### 验收标准

```powershell
# 未登录/他人访问待审核启事 → 404
curl.exe -s "http://localhost:8080/api/items/9"                       # 期望 {"code":404,...}
curl.exe -s "http://localhost:8080/api/items/9/similar?n=3"           # 期望 data=[] 或 404
# 发布者本人访问自己的待审核启事 → 200（用 2024000X 登录后访问自己刚发布的启事）
# 已过审启事正常访问 → 200
curl.exe -s "http://localhost:8080/api/items/1"                       # 期望 {"code":200,...}
```

---

## B-02（P0）首页统计与墙页总数口径不自洽

### 现象与证据（实测）

```
GET /api/stats                    → reunited=15  lostCount=22  foundCount=23   （lost+found = 45）
GET /api/items?type=lost&size=1   → total=28
GET /api/items?type=found&size=1  → total=28                                   （28+28 = 56）
```

三个口径互不自洽，且 `StatsService` 的类注释写着"使'寻找中'计数与墙上条数对得上"——实际对不上。

### 根因

1. `StatsService.homeStats()` 用 `notIn("status", CLOSED)`，`CLOSED = {已找回, 已认领, 已下架, 已关闭}` → 排除了已团聚启事；
   而 `ItemService.list()` 的 `HIDDEN = {已下架, 已关闭}`，**不排除已找回/已认领** → 墙页把 11 条已团聚启事也列出来了。
2. `StatsService.homeStats().reunited` 走 `ItemMapper.countReunited()`：
   `SELECT COUNT(*) FROM item WHERE status IN ('已找回','已认领')` —— **没有 `audit_status = 1` 过滤**。
   实测 reunited=15，而墙页上状态为已找回/已认领的只有 11 条，差值 4 正是那 4 条待审核启事（status 却是"已找回/已认领"，见 `05` 的 S-04）。

### 修复要求

先明确产品口径，再改代码。推荐口径（与 SRS FR4.1「列表按类型筛选」+ 首页"寻找中/待认领"语义一致）：

1. **墙页只展示"仍在流转"的启事**：`ItemService.list()` 的隐藏集合扩展为 `{已下架, 已关闭, 已找回, 已认领}`（即与 `StatsService` 对齐）。
   - 若产品上确实希望墙页也展示"已找回"作为战报，则**反向统一**：`homeStats` 不再排除已团聚，并同步改 `StatsService` 注释与 `docs/01` SRS。二选一，但必须全站一致。
2. `ItemMapper.countReunited()` 必须加 `audit_status = 1`，避免把未过审数据计入首页数字。
3. 统一后必须满足：
   ```
   stats.lostCount  == list(type=lost).total
   stats.foundCount == list(type=found).total
   stats.reunited   == (SELECT COUNT(*) FROM item WHERE audit_status=1 AND status IN ('已找回','已认领'))
   ```
4. 修改 `StatsService` 中那句与事实不符的注释。
5. 前端首页/落地页用的是 `lostCount + foundCount`（`HomeView.vue`、`WelcomeView.vue`），口径统一后不需要改前端，但请回归确认数字仍显示正常。

### 验收标准

给出修改后的实测四元组，并证明上面 3 条恒等式成立（改前后各一次对比）。

---

## B-03（P1）已驳回启事再次"通过"后仍永久隐藏

### 现象

`AdminService.auditItem(adminId, itemId, approve)`：

```java
it.setAuditStatus(approve ? 1 : 2);
if (!approve) it.setStatus("已关闭");
itemMapper.updateById(it);
```

驳回时把业务状态写成了「已关闭」，**再通过时只把 auditStatus 改回 1，不改 status** → 启事在墙页（`notIn HIDDEN`）和搜图 SQL（`status NOT IN ('已下架','已关闭')`）里永远不出现，管理员会以为"通过了但前台没有"。

当前前端「已驳回」页签只提供"删除"，所以 UI 点不到；但接口可直接调用：
`POST /api/admin/items/{id}/audit?approve=true`。

### 修复要求

1. 审核维度与业务维度必须解耦：
   - 「已关闭」不应由"驳回"写入（驳回只改 `audit_status = 2`）；或者
   - 若确实要用「已关闭」表示被驳回，则 `approve = true` 时必须把 status 恢复为业务初始状态：`"lost".equals(type) ? "寻找中" : "待认领"`。
2. 同时补一条守卫：**已认领/已找回（业务已完成）的启事不允许被审核修改**，避免管理员把已团聚的启事状态改坏。
3. 无论选哪种方案，都要保证：
   - `audit_status = 0` → 前台不可见
   - `audit_status = 2` → 前台不可见
   - `audit_status = 1` → 前台可见（除非 status 是已下架/已关闭/已完成）
4. 前端 `AuditView.vue` 的「已驳回」页签需要相应提供"重新通过"按钮（与 `04` Prompt 联动）。

### 验收标准

```powershell
# 取一条 auditStatus=0 的启事，先驳回再通过，检查最终状态
# 期望：通过后 GET /api/items/{id} 与 GET /api/items?size=100 中重新可见，status 为 寻找中/待认领
```

---

## B-04（P1）登录限流：锁定期过后错 1 次即再次锁定

### 现象与根因

`AuthService.login` 的限流状态是一个 `ConcurrentHashMap<String, long[]>`（`[失败次数, 锁定截止时间]`）：

```java
long[] st = loginFail.get(acc);
if (st != null && st[1] > System.currentTimeMillis())
    throw new BusinessException("错误次数过多，账号已临时锁定，请 10 分钟后再试");
...
long[] cur = loginFail.computeIfAbsent(acc, k -> new long[]{0, 0});
cur[0]++;
if (cur[0] >= MAX_FAIL) cur[1] = System.currentTimeMillis() + LOCK_MS;
```

失败计数 `cur[0]` **只在登录成功时 `loginFail.remove(acc)` 才清零**。10 分钟锁定期一过，`st[1] <= now` 检查通过，但只要再输错一次，`cur[0]` 就从 6 变 7 再次 `>= MAX_FAIL` → **立刻又锁 10 分钟**。等于账号被无限期锁死，每次只需攻击者错一次。

### 修复要求

1. 锁定期结束后必须重置计数：在判定"未锁定"的分支里，若 `st != null && st[1] != 0 && st[1] <= now`，则把 `st[0] = 0; st[1] = 0;`（或直接 `loginFail.remove(acc)`）。
2. 密码正确时保持现有的 `loginFail.remove(acc)`。
3. 顺手加固（可选但建议）：
   - 给 map 加一个上限或定期清理，避免被大量随机学号刷爆内存（与 B-06/`02` 的同类问题一起考虑）；
   - 锁定时长的提示文案保留现有中文风格。
4. **不要**为此引入 Redis（项目约束）。

### 验收标准

为了不把演示账号锁死，请用 `AuthServiceTest`（Mockito 单元测试，不连库）覆盖以下场景并全部通过：

- 连续 5 次错误 → 第 6 次提示"已临时锁定"；
- 模拟时间越过锁定期（可注入时钟，或直接操作 `loginFail` 状态 / 用可测的私有方法）→ 再错 1 次**应为普通"学号或密码错误"，而不是立刻再次锁定**；
- 期间任一次登录成功 → 计数清零。

---

## B-05（P1）删除启事只清 `claim` 消息，`dm` 私信成为悬挂数据

### 现象与根因

`AdminService.deleteItem`：

```java
messageMapper.delete(new QueryWrapper<Message>().eq("ref_id", itemId).eq("type", "claim"));
```

而 `ItemService.sendDm` 写入的是 `type = "dm"`、`ref_id = itemId`：

```java
m.setType("dm");
m.setRefId(itemId);
```

→ 删除启事后，私信仍留在收件人消息中心，点击"查看启事"跳到 404。

**这条同时是测试脚本不幂等的根因**：`scripts/test_api.py` 每次运行都会造 1 条 dm，而服务端清理不掉，所以 `message` 表行数永远回不到基线（实测库里 `message=14`，而 `DataInitializer` 只播 12 条）。

### 修复要求

1. 删除启事时一并清理所有指向该启事的消息：`messageMapper.delete(new QueryWrapper<Message>().eq("ref_id", itemId))`（不再限制 `type`）。
   - 需要评估：`ref_id` 也被别的业务类型用过吗？当前只有 `claim` 与 `dm` 用 itemId 作为 refId，确认后可以无条件按 `ref_id` 删。
2. 更稳妥的做法是**软删除**：`message` 表加 `deleted` 字段或把内容改写为"关联启事已删除"，前端点击时不再跳转。二选一，但要保证前端不会跳到不存在的启事。
3. 修好后，`scripts/test_api.py` 的幂等自检才有意义（配合 `05` Prompt 一起改）。

### 验收标准

```powershell
# 删除一条有私信的启事后
SELECT COUNT(*) FROM message WHERE ref_id = <已删除的itemId>;   -- 期望 0
# 个人中心消息列表不再出现指向已删除启事的条目
```

---

## 交付要求

1. 逐条给出：**改动文件 + 关键 diff 摘要 + 验证命令 + 实际输出**。
2. 全部改完后跑一遍 `00-索引与执行顺序.md` 第六节的通用验收命令，确认：编译通过、单元测试全绿、`python scripts/test_api.py` 退出码 0。
3. 更新 `lost-found/docs/09-缺陷跟踪表-v1.0.md`，把 B-01~B-05 登记为新的缺陷条目（编号接在 #9 之后），并写明修复方案与回归方式。
4. **不要**顺手重排/格式化无关代码，不要改 `uploads/`、`scripts/exp_*.csv`。
