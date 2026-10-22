-- ============================================================================
-- v2.41.1 迁移：item 表补精确坐标（已在本机执行过，重复执行会报重复列，属正常）
-- 用途：位置卡显示"真实位置"（发布页地图选点 / 浏览器定位写入，GCJ-02）
-- 新库不用跑：schema-init.sql / schema.sql 已包含这两列
-- 执行：mysql -uroot -p lost_found < backend/scripts/migrate-v2.41.1-location.sql
-- ============================================================================

ALTER TABLE `item`
  ADD COLUMN `latitude`  DECIMAL(10,6) DEFAULT NULL COMMENT '纬度（GCJ-02，发布页地图选点，可空）' AFTER `location`,
  ADD COLUMN `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度（GCJ-02，发布页地图选点，可空）' AFTER `latitude`;

-- 可选：给 8 个预设地点补"楼栋近似坐标"（演示用，非精确位置）。
-- 不跑这段时，旧启事在位置卡里显示"未标注精确位置 · 校园示意图"，不会假装精确。
-- 使用时把下面的坐标换成你们校区的真实中心点，并按楼栋相对位置微调。
-- UPDATE item SET latitude = 30.535900, longitude = 114.361000
--   WHERE location = '图书馆' AND latitude IS NULL;
