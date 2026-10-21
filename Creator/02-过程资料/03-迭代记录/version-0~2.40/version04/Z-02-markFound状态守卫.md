# Z-02（P3·后端）markFound 缺状态守卫：悬挂在审申请 + 已下架/已关闭可被翻回终态

> Z 批第 2 条（共 11 条）。执行前先读 `Deepseek_后端/索引与执行顺序.md`（JAVA_HOME 红线、验收命令）与 `Qwenwork_修改/05-修复记录-2026-10-15.md`（文档数字口径）。总览与公共红线见 `索引与执行顺序.md` 及本文末尾。

## 现象与证据

`ItemService.markFound`（约 196-202 行）只有归属校验，无状态守卫、无事务，直接写终态：

```java
public void markFound(Long id, Long userId) {
    Item item = itemMapper.selectById(id);
    if (item == null) throw new BusinessException(404, "启事不存在");
    if (!item.getUserId().equals(userId)) throw new BusinessException(403, "只能操作自己发布的启事");
    item.setStatus("lost".equals(item.getType()) ? "已找回" : "已认领");   // ← 直接改终态，无状态守卫
    itemMapper.updateById(item);
}
```

由此产生两类问题（2026-10-16 二次复核后收窄立论，修正 A）：

1. **悬挂申请（核心问题）**：启事处于"认领申请中"时本人直接 mark-found → status 变终态，但该启事下其余"审核中"的认领申请**永远停在审核中**——`ClaimService.audit` 的"已完成"守卫对通过/拒绝两分支都生效，这些申请再也无人能处理，申请人视角状态永不落地。
2. **已下架/已关闭可被翻回终态**：本人对"已下架/已关闭"启事调 mark-found 仍会成功，把管理端的处置结果翻回"已找回/已认领"，与 `auditItem` 的 B-03 守卫不对称。
   （注：**"待审核"不在本条范围内**——待审启事对他人本就不可见（B-01），本人标记完成无外部影响，过审后按"已完成 + audit=1"正常展示；详见修复要求 1 的说明。）

## 修复要求

1. 增加状态白名单（与 05 批 #23 修复后的认领白名单一致）：`{寻找中, 待认领, 认领申请中}`；白名单外（`已下架/已关闭/已找回/已认领` 等）抛 `BusinessException(400, "该启事当前状态不允许标记完成")`。
   > ⚠️ 注意：待审核启事的 status 恰落在白名单内（`publish` 设 `寻找中/待认领` + audit=0），所以"待审启事本人标记完成"**会成功——这是有意的**，不要额外加 `auditStatus == 1` 判断（修正 A 已论证无外部影响）。
2. `status == 认领申请中` 时标记成功前，**批量关闭该启事下所有"审核中"申请**（置为拒绝/已关闭语义，与 `ClaimService.apply` 通过时的自动关闭逻辑对齐）并给申请人发站内通知——推送写法参考 `ClaimService` 中"通过时自动关闭其余申请并通知"（消息 refId=itemId 口径，勿破坏 B-05 的幂等基线）。
3. 事务：状态更新 + 申请批量关闭 + 消息写入放同一 `@Transactional`（现状 markFound 连 `@Transactional` 都没有）。
4. 回归测试：
   - `ItemServiceTest` 新增用例（Mockito 风格随现有 2 条）：本人+寻找中 → 成功；非本人 → 403；已找回 → 400；已下架 → 400；认领申请中 → 成功且 verify 申请批量关闭与通知写入。
   - 有余力再在 `scripts/test_api.py` 补 2 条断言（终态再标记 400、竞争悬挂场景）。
5. **文档同步（修正 C，勿漏）**：
   - `Creator/01-交付文档/02-UML建模全套-v1.0.md` L177-178 状态图补边：`认领申请中 --> 已找回/已认领: 失主标记完成（自动关闭其余在审申请并通知）`；
   - `Creator/01-交付文档/04-接口文档-v1.0.md` L29 mark-found 行补"仅 `寻找中/待认领/认领申请中` 可标记，白名单外 400"；
   - 若本条登记为缺陷，Creator/01-交付文档/09 按 #24 起编号并更新统计段。

## 验收标准

```bash
cd backend && mvn -o -B test        # 全绿，用例数 35 → ≥40
# 手工：java -jar 启动后
#   对已找回启事 POST /api/items/{id}/mark-found → code=400
#   对寻找中本人启事 → 200；认领申请中本人启事 → 200 且在审申请全部关闭+通知
```

## 公共红线（Z 批，违者返工）

1. `JAVA_HOME` 指 JDK21；**中文路径下禁止 `mvn spring-boot:run`**（argfile 编码假错），启动验证一律 `mvn -o -B -DskipTests package` + `java -jar target/lost-found-backend.jar`。
2. `scripts/exp_*.csv`、`Creator/01-交付文档/11` 数据表零修改（D-01）。
3. 所有文档数字改动必须与实测输出一致（宁可改小）。
4. 幂等基线：test_api.py 跑后 item=60 / item_image=120 / message 回基线；admin_log 只增不计入。
5. 不新增任何 `_*.txt`/`_*.json` 调试残留入库；git 提交由组员执行。
6. 修完在 `Qwenwork_修改/修复记录-2026-10-16.md` 追加对应小节（逐条修改+证据），格式沿用 01-05 修复记录。
