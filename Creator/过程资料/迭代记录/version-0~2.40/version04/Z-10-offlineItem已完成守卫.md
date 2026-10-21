# Z-10（P3·后端）`offlineItem` 缺"已完成"守卫（B-03 只修了 `auditItem` 一侧）

> Z 批第 10 条（共 11 条）。执行前先读 `Deepseek_后端/索引与执行顺序.md`（JAVA_HOME 红线、验收命令）。总览与公共红线见 `索引与执行顺序.md` 及本文末尾。
> 关联：补的 AdminServiceTest 用例并入 `Z-04` 的用例名单。

## 现象与证据

```java
// AdminService.auditItem —— B-03 已加守卫
if ("已认领".equals(it.getStatus()) || "已找回".equals(it.getStatus()))
    throw new BusinessException("该启事已完成认领流程，不允许再审核修改");

// AdminService.offlineItem —— 无任何守卫
public void offlineItem(Long adminId, Long itemId) {
    Item it = itemMapper.selectById(itemId);
    if (it == null) throw new BusinessException(404, "启事不存在");
    it.setStatus("已下架");
    it.setAuditStatus(2);
    itemMapper.updateById(it);
    log(adminId, "offline", "item", itemId, "违规下架");
}
```

管理员仍可把一条**已团聚**（已认领/已找回）的启事"下架"，同时把 `audit_status` 由 1 改成 2——与 B-03"审核维度与业务维度解耦"的取向略有反复，也与 `auditItem` 的守卫不对称。

## 修复要求

二选一，并写明理由：

- **A**：与 `auditItem` 对齐——`status ∈ {已认领,已找回}` 时抛 `BusinessException`（"该启事已完成认领流程，不允许下架"）；
- **B**：允许下架，但**只改 `audit_status`、不改 `status`**（保留下架语义又不破坏业务状态），并在注释说明与 `auditItem` 的差异。

补 `AdminServiceTest` 用例（并入 Z-04 的用例名单）。

## 验收标准

```bash
# 对一条已认领/已找回启事
curl -s -X POST "http://localhost:8080/api/admin/items/{id}/offline" -H "Authorization: Bearer <admin>"
# → 方案A：code=400 且库中 status/audit_status 均不变；方案B：code=200 且 status 不变、audit_status=2
```

## 公共红线（Z 批，违者返工）

1. `JAVA_HOME` 指 JDK21；**中文路径下禁止 `mvn spring-boot:run`**（argfile 编码假错），启动验证一律 `mvn -o -B -DskipTests package` + `java -jar target/lost-found-backend.jar`。
2. `scripts/exp_*.csv`、`Creator/交付文档/11` 数据表零修改（D-01）。
3. 幂等基线：test_api.py 跑后 item=60 / item_image=120 / message 回基线；admin_log 只增不计入。
4. 不新增任何 `_*.txt`/`_*.json` 调试残留入库；git 提交由组员执行。
5. 修完在 `Qwenwork_修改/修复记录-2026-10-16.md` 追加对应小节（逐条修改+证据），格式沿用 01-05 修复记录。
