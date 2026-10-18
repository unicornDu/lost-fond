# 数据库与上传图片 备份/恢复方案（审查 2.41）

> 交付物：分工方案中"数据库及文档"角色的**备份方案**。图片存文件系统（不在库内），故备份必须"库 + uploads 目录"两件套。
> 05 C-03 修订：文件名占位符改为脚本实际格式；恢复流程补 DROP（否则非空库必报 1050）；Windows 部分改 PowerShell；`backup/` 已进 .gitignore。

## 一、备份

```bash
cd backend
bash scripts/backup.sh          # Git-Bash / Linux / macOS
```

脚本做两件事（口令自动从 `application-local.yml` 读取；**读不到会报错退出，不会静默空口令卡死**）：
1. `mysqldump --single-transaction lost_found > backup/lost_found_<YYYYMMDD_HHMMSS>.sql`
2. `tar czf backup/uploads_<YYYYMMDD_HHMMSS>.tar.gz uploads/`

Windows 无 bash 时用 **PowerShell** 等价命令（注意文件名与脚本格式一致，含时分秒）：

```powershell
cd D:\Visual_Stdio_Code\Dai_ma\软件工程\lost-found\backend
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
& "D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64\bin\mysqldump.exe" -uroot -p --single-transaction --set-gtid-purged=OFF --default-character-set=utf8mb4 lost_found | Out-File -Encoding utf8 "backup\lost_found_$ts.sql"
tar czf "backup\uploads_$ts.tar.gz" uploads/
```

**频率约定**：每周一次 + 每次演示/答辩前一次 + 每次批量造数后一次。

## 二、恢复

> ⚠️ **破坏性操作**：第 1 步会 `DROP DATABASE`，库内现有数据全部清空。只想重置演示数据请用"TRUNCATE 八表 + 重启"（见 `Creator/01-交付文档/运行说明/01-启动步骤.md` 第六节），不要走本流程。

```bash
# 0) 确认备份文件名（脚本实际格式含时分秒，照抄下面命令时替换成真实文件名）
ls backup/
#    例：lost_found_20261015_213000.sql / uploads_20261015_213000.tar.gz

# 1) 重建空库再导入（dump 不带 DROP TABLE，恢复到非空库必然 ERROR 1050 Table already exists）
mysql -u root -p -e "DROP DATABASE IF EXISTS lost_found; CREATE DATABASE lost_found DEFAULT CHARSET utf8mb4;"
mysql -u root -p lost_found < backup/lost_found_20261015_213000.sql

# 2) 恢复图片（覆盖到 backend/uploads/）
tar xzf backup/uploads_20261015_213000.tar.gz -C .    # 在 backend/ 目录下执行

# 3) 重启后端（DataInitializer 见 item 表非空会跳过播种，不会覆盖恢复的数据）
```

PowerShell 等价：

```powershell
& "D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64\bin\mysql.exe" -uroot -p -e "DROP DATABASE IF EXISTS lost_found; CREATE DATABASE lost_found DEFAULT CHARSET utf8mb4;"
Get-Content "backup\lost_found_20261015_213000.sql" -Raw | & "D:\Visual_Stdio_Code\Claude_Code\MysqL\mysql-9.7.0-winx64\bin\mysql.exe" -uroot -p lost_found
tar xzf "backup\uploads_20261015_213000.tar.gz" -C .
```

## 三、注意事项

- `backup/` 与 `application-local.yml` 均含敏感/运行数据，**不要提交 git**：`.gitignore` 已含 `application-local.yml` 与 `backend/backup/`（05 C-01 落实）。
- 恢复后若新增过图片而备份较旧，缺失图片前端会显示分类 emoji 兜底（`@error` 隐藏 img），不会白屏。
- 与"重置演示数据"区分：重置是 `TRUNCATE` 八表后重启（DataInitializer 重播种）；恢复是用备份覆盖，二者不要混用。
- **上线前检查**（`Creator/01-交付文档/12-部署手册` 同步修订）：不能只"执行过一次 backup.sh"，必须**实际演练过一次完整恢复**（DROP→导入→起服务→抽查一条启事与一张图），否则等于没验证。
