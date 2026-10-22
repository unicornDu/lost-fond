#!/usr/bin/env bash
# 数据库 + 上传图片 备份脚本（审查 2.41 / 分工方案"备份方案"交付物）
# 用法：bash scripts/backup.sh     （口令从 backend/application-local.yml 读取，无需手输）
# 恢复：见 scripts/backup.md
set -euo pipefail
cd "$(dirname "$0")/.."          # 进入 backend/（固定工作目录，审查 2.42②）

TS="$(date +%Y%m%d_%H%M%S)"
mkdir -p backup

# 从 application-local.yml 读取数据库账号/口令（避免在脚本里硬编码）
LOCAL="application-local.yml"
DB_USER="$(sed -nE 's/^[[:space:]]*username:[[:space:]]*//p' "$LOCAL" | head -1)"
DB_PASS="$(sed -nE 's/^[[:space:]]*password:[[:space:]]*//p' "$LOCAL" | head -1)"
DB_USER="${DB_USER:-root}"
# 修复 05 C-03：取不到口令必须报错退出——静默空口令会让 mysqldump 交互式卡住（无人值守表现为挂死）
if [ -z "$DB_PASS" ]; then
  echo "错误：无法从 $LOCAL 读取数据库口令（spring.datasource.password）。请确认文件存在且格式正确。" >&2
  exit 1
fi

echo ">> 导出数据库 lost_found -> backup/lost_found_${TS}.sql"
mysqldump -u "$DB_USER" -p"$DB_PASS" --single-transaction --set-gtid-purged=OFF \
          --default-character-set=utf8mb4 lost_found > "backup/lost_found_${TS}.sql"

echo ">> 打包上传图片 -> backup/uploads_${TS}.tar.gz"
tar czf "backup/uploads_${TS}.tar.gz" uploads/

echo ">> 完成："
ls -lh backup/ | tail -3
echo "提示：演示前与每周各跑一次；backup/ 已进 .gitignore（05 C-01），不会进 git。"
