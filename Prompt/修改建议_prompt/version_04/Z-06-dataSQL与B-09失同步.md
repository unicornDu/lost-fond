# Z-06（P1·数据/文档）`data.sql` 与 B-09 失同步：README 的方案 a 会把刚修掉的脏数据带回来

> Z 批第 6 条（共 11 条，**P1**）。执行前先读 `Deepseek_后端/00-索引与执行顺序.md`（JAVA_HOME 红线、验收命令）与 `Qwenwork_修改/05-修复记录-2026-10-15.md`。总览与公共红线见 `00-索引与执行顺序.md` 及本文末尾。

## 现象与证据（实测 2026-10-16）

1. `项目运行说明/README.md` L86-88 仍把 data.sql 作为**等价安装路径**提供：

```
# 演示数据二选一：
#   a) 直接导入：  mysql -u root -p lost_found < backend/src/main/resources/db/data.sql
#   b) 自动生成（推荐）：先跑 scripts/gen_seed_images.py（离线，读 uploads/_src），
```

2. 但 `backend/src/main/resources/db/data.sql` 内的数据仍是 **2026-10-14 21:58 的旧 dump**（05 批 C-03 当时编辑过该文件删 GTID/LOCK 行，却没重新 dump），B-09 修掉的两类脏数据原样保留：

```
-- 待审核却已团聚（B-09 的目击数据，audit_status=0 而 status 是终态）
VALUES (9,'lost','丢失蓝色小熊保温杯',…,'已找回','/uploads/seed/cup-3.jpg',176,2,0,…)
VALUES (10,'found','招领：蓝色保温杯',…,'已认领','/uploads/seed/cup-4.jpg',193,20,0,…)
VALUES (19,'lost','丢了一串钥匙急寻',…,'已找回','/uploads/seed/keys-3.jpg',181,10,0,…)
VALUES (20,'found','招领：钥匙串带挂件',…,'已认领','/uploads/seed/keys-4.jpg',198,8,0,…)

-- 同一申请人重复（B-09 修的另一半：claimants 由 c1,c2,c1 改为 c1,c2,c3）
INSERT INTO `claim_record` … VALUES (1,1,2,…,'审核中',…)     -- claimant=2
INSERT INTO `claim_record` … VALUES (2,1,4,…,'已通过',…)
INSERT INTO `claim_record` … VALUES (3,1,2,…,'已拒绝',…)     -- claimant=2 重复
```

3. 当前库（重播种后）对照，说明代码侧确实是好的、只有 dump 侧是坏的：

```
SELECT id, audit_status, status FROM item WHERE id IN (9,10,19,20);
→ 9: 0/寻找中   10: 0/待认领   19: 0/寻找中   20: 0/待认领
SELECT claimant_id, status FROM claim_record WHERE item_id=1;
→ 2:审核中   3:已通过   4:已拒绝            （3 个互不相同 ✓）
```

## 根因

03/05 批改了 `DataInitializer` 的规范化逻辑与 `uploads/seed/manifest.json`，但**没有重新 dump `data.sql`**。而 `DataInitializer.run()` 是"`item` 表非空即跳过"——**导入 data.sql 后那段规范化代码永远不会执行**，脏数据就永久留在库里。

**05 批的 C-03（backup 重写）反而让这条路径更危险**：当时删掉了 `SET @@GLOBAL.GTID_PURGED` 与 `LOCK TABLES`，本意是"让普通账号也能导入"，结果是这条会带回脏数据的路径变得**更容易走通**。

另外存在文档内部矛盾：`docs/06` L190 明确写 `data.sql` "**仅作离线备份**（mysqldump 导出）"，而 README L86 把它写成与自动播种**平级**的"二选一"，启动步骤 L71 又把它列为"备选方案"。

## 修复要求（二选一，**推荐 A**）

**方案 A（推荐，最省事）**：删除 `data.sql`，README 只保留自动播种路径。
理由：`DataInitializer` 已能一键播种 60 启事 / 120 条图片引用 / 认领三态 / 消息并重算 pHash，data.sql 的唯一价值是"离线备份"，而备份用 `scripts/backup.sh` 更正确（05 批 C-03 刚重写过 backup.md）。
需同步删除/改写的引用（`grep -rn "data\.sql"` 全量命中约 **5 份文档 10 处**，执行时以实际 grep 为准；`docs/12-部署手册` 未引用 data.sql，无需改）：

| 文件 | 行 | 现内容 | 处理 |
|---|---|---|---|
| `项目运行说明/README.md` | 35、87 | 目录树 + 方案 a 命令 | 删方案 a，目录树去掉 data.sql |
| `项目运行说明/00-项目知识点与结构详解-新手版.md` | 209、315 | 目录树 + "mysqldump 导出的初始化数据" | 改为"离线备份由 scripts/backup.sh 产出" |
| `项目运行说明/启动步骤-MarkDone.md` | 71 | "备选方案：`mysql … < data.sql`" | 删除该备选，指向自动播种 |
| `项目运行说明/项目结构.md` | 72、138、147 | 文件清单 + M7 + 已完成清单 | 去掉 data.sql |
| `docs/06-数据库设计…` | 5、190 | "初始化数据：data.sql" + "仅作离线备份" | 保留"离线备份"表述，删掉"初始化数据"说法 |

**方案 B（保留 data.sql）**：重新生成，并在 README 注明"dump 日期与来源提交"，避免再次失同步。

1. `TRUNCATE` 六表 → 启动后端由新代码重新播种 → `mysqldump --no-create-info --single-transaction --set-gtid-purged=OFF --default-character-set=utf8mb4 lost_found > db/data.sql`
2. 在 `data.sql` 头部加注释：`-- 与 uploads/seed/manifest.json 同源；生成日期 2026-10-XX；种子逻辑变更后必须重新生成`
3. 验收时**必须实际导入一次并跑下文验收 SQL**（不能只看文件）。

## 验收标准

```sql
-- 方案 B（保留 data.sql）时适用：走导入路径后，两条都必须成立
SELECT id, audit_status, status FROM item WHERE audit_status <> 1;                      -- status 只能 寻找中/待认领
SELECT item_id, claimant_id, COUNT(*) c FROM claim_record GROUP BY item_id, claimant_id HAVING c > 1;   -- 必须为空
```

```
grep -rn "data.sql" 项目运行说明/ docs/     # 结果必须与"最终选择的方案"一致，不留互相矛盾的指引
```

## 公共红线（Z 批，违者返工）

1. `JAVA_HOME` 指 JDK21；**中文路径下禁止 `mvn spring-boot:run`**（argfile 编码假错），启动验证一律 `mvn -o -B -DskipTests package` + `java -jar target/lost-found-backend.jar`。
2. `scripts/exp_*.csv`、`docs/11` 数据表零修改（D-01）。
3. 所有文档数字改动必须与实测输出一致（宁可改小）。
4. 幂等基线：test_api.py 跑后 item=60 / item_image=120 / message 回基线；admin_log 只增不计入。
5. 不新增任何 `_*.txt`/`_*.json` 调试残留入库；git 提交由组员执行。
6. **`data.sql` 若保留，必须与 `uploads/seed/manifest.json` 同源同版本**；种子逻辑变更后要么重生成、要么删除，不留"半新半旧"的第二条安装路径。
7. 修完在 `Qwenwork_修改/06-修复记录-2026-10-16.md` 追加对应小节（逐条修改+证据），格式沿用 01-05 修复记录。
