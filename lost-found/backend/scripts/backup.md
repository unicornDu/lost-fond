# 数据库与上传图片 备份/恢复方案（审查 2.41）

> 交付物：分工方案中"数据库及文档"角色的**备份方案**。图片存文件系统（不在库内），故备份必须"库 + uploads 目录"两件套。

## 一、备份

```bash
cd backend
bash scripts/backup.sh          # Linux/macOS/Git-Bash
```

脚本做两件事（口令自动从 `application-local.yml` 读取，无需手输）：
1. `mysqldump --single-transaction lost_found > backup/lost_found_<时间戳>.sql`
2. `tar czf backup/uploads_<时间戳>.tar.gz uploads/`

Windows 无 bash 时可手动执行等价命令：
```bat
mysqldump -u root -p lost_found > backup\lost_found_%date:~0,4%%date:~5,2%%date:~8,2%.sql
tar czf backup\uploads.tar.gz uploads\
```

**频率约定**：每周一次 + 每次演示/答辩前一次 + 每次批量造数后一次。

## 二、恢复（到空库）

```bash
# 1) 恢复表结构+数据
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS lost_found DEFAULT CHARSET utf8mb4;"
mysql -u root -p lost_found < backup/lost_found_YYYYMMDD.sql

# 2) 恢复图片（覆盖到 backend/uploads/）
tar xzf backup/uploads_YYYYMMDD.tar.gz -C .    # 在 backend/ 目录下执行

# 3) 重启后端（DataInitializer 见 item 表非空会跳过播种，不会覆盖恢复的数据）
```

## 三、注意事项

- `backup/` 与 `application-local.yml` 均含敏感/运行数据，**不要提交 git**（.gitignore 已含 application-local.yml；建议追加 `backup/`）。
- 恢复后若新增过图片而备份较旧，缺失图片前端会显示分类 emoji 兜底（`@error` 隐藏 img），不会白屏。
- 与"重置演示数据"区分：重置是 `TRUNCATE` 六表后重启（DataInitializer 重播种）；恢复是用备份覆盖，二者不要混用。
