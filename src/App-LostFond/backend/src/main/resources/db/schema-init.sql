-- =============================================================
-- 拾光 · 校园失物招领平台  数据库初始化脚本【安全版：不清数据】
-- Database: lost_found   Engine: InnoDB   Charset: utf8mb4
-- 2026-10-18 审查修复新增：与 schema.sql 的区别是「无 DROP TABLE、全部 CREATE TABLE IF NOT EXISTS」，
-- 可重复执行：已有表保持不变、缺表补齐，适合首次初始化与已有库升级。
-- 需要彻底重置（⚠️ 会清空数据）时使用同目录 schema.sql，执行前先跑 backend/scripts/backup.sh。
-- =============================================================

CREATE DATABASE IF NOT EXISTS lost_found
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE lost_found;

-- ---------- 用户表 ----------
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `account`     VARCHAR(20)  NOT NULL COMMENT '学号（登录账号，6-12位数字）',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `nickname`    VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar`      VARCHAR(16)  NOT NULL DEFAULT '🦊' COMMENT '头像（emoji）',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话（选填）',
  `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0=普通用户 1=管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=封禁',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 启事（物品）表 ----------
CREATE TABLE IF NOT EXISTS `item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '启事ID',
  `type`         VARCHAR(10)  NOT NULL COMMENT '类型：lost=寻物启事 found=招领启事',
  `title`        VARCHAR(60)  NOT NULL COMMENT '物品名称/标题',
  `category`     VARCHAR(20)  NOT NULL DEFAULT '其他' COMMENT '分类：证件卡类/钥匙/电子设备/书籍资料/衣物饰品/其他',
  `location`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '地点',
  `latitude`     DECIMAL(10,6) DEFAULT NULL COMMENT '纬度（GCJ-02，发布页地图选点，可空）',
  `longitude`    DECIMAL(10,6) DEFAULT NULL COMMENT '经度（GCJ-02，发布页地图选点，可空）',
  `happen_time`  DATETIME     DEFAULT NULL COMMENT '丢失/捡到时间',
  `description`  VARCHAR(500) DEFAULT '' COMMENT '详细描述',
  `status`       VARCHAR(20)  NOT NULL DEFAULT '寻找中' COMMENT '业务状态：寻找中/待认领/认领申请中/已找回/已认领/已下架/已关闭',
  `cover_img`    VARCHAR(255) DEFAULT NULL COMMENT '封面图（首图）相对路径',
  `views`        INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `user_id`      BIGINT       NOT NULL COMMENT '发布人ID',
  `audit_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=已通过 2=已驳回（默认 0 与代码口径一致，防止 SQL 直插绕过审核）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`, `status`),
  KEY `idx_category` (`category`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物/招领启事表';

-- ---------- 启事图片表（含 pHash 感知哈希） ----------
CREATE TABLE IF NOT EXISTS `item_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `item_id`     BIGINT       NOT NULL COMMENT '所属启事ID',
  `img_url`     VARCHAR(255) NOT NULL COMMENT '图片相对路径',
  `phash`       BIGINT       DEFAULT NULL COMMENT '64位感知哈希（有符号BIGINT存储，汉明距离=BIT_COUNT(a XOR b)）',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '图片顺序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='启事图片表（存 pHash 指纹，以图搜图核心）';

-- ---------- 认领申请表 ----------
CREATE TABLE IF NOT EXISTS `claim_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '认领记录ID',
  `item_id`     BIGINT       NOT NULL COMMENT '启事ID',
  `claimant_id` BIGINT       NOT NULL COMMENT '认领申请人ID',
  `proof`       VARCHAR(300) NOT NULL DEFAULT '' COMMENT '认领凭证（只有失主知道的特征描述）',
  `contact`     VARCHAR(50)  DEFAULT NULL COMMENT '联系方式',
  `status`      VARCHAR(20)  NOT NULL DEFAULT '审核中' COMMENT '状态：审核中/已通过/已拒绝',
  `reply`       VARCHAR(200) DEFAULT NULL COMMENT '发布者回复',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_claimant` (`claimant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认领申请与审核表';

-- ---------- 站内消息表 ----------
CREATE TABLE IF NOT EXISTS `message` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`      BIGINT       NOT NULL COMMENT '接收人ID',
  `from_user_id` BIGINT       DEFAULT NULL COMMENT '发送者ID（NULL=系统消息，dm 恒有值，支持双向回复）',
  `type`         VARCHAR(20)  NOT NULL DEFAULT 'system' COMMENT '类型：claim/audit/dm/system',
  `title`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题',
  `content`      VARCHAR(300) DEFAULT '' COMMENT '内容',
  `ref_id`       BIGINT       DEFAULT NULL COMMENT '关联业务ID（item/claim）',
  `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知消息表';

-- ---------- 管理员操作日志表 ----------
CREATE TABLE IF NOT EXISTS `admin_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `admin_id`    BIGINT       NOT NULL COMMENT '管理员ID',
  `action`      VARCHAR(50)  NOT NULL COMMENT '操作类型：audit/offline/ban/delete',
  `target_type` VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '目标类型：item/user',
  `target_id`   BIGINT       DEFAULT NULL COMMENT '目标ID',
  `detail`      VARCHAR(300) DEFAULT '' COMMENT '操作详情',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- ---------- 启事收藏表 ----------
CREATE TABLE IF NOT EXISTS `favorite` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`     BIGINT      NOT NULL COMMENT '收藏人ID',
  `item_id`     BIGINT      NOT NULL COMMENT '启事ID',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='启事收藏表';

-- ---------- 交付文件表 ----------
CREATE TABLE IF NOT EXISTS `delivery` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '交付ID',
  `user_id`     BIGINT       NOT NULL COMMENT '接收人ID（下载/打开仅限本人，管理员豁免）',
  `biz_type`    VARCHAR(32)  NOT NULL COMMENT '业务类型：CLAIM_CERT认领证明/REPORT_CSV统计报表/MATCH_REPORT匹配报告',
  `biz_id`      BIGINT       DEFAULT NULL COMMENT '关联业务ID（item/claim；启事删除时级联清理）',
  `title`       VARCHAR(128) NOT NULL COMMENT '卡片标题：认领证明 #12',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '卡片副文案',
  `file_path`   VARCHAR(512) NOT NULL COMMENT '相对 uploads 根的路径：deliveries/yyyy/MM/uuid.pdf',
  `file_name`   VARCHAR(255) NOT NULL COMMENT '下载展示文件名',
  `mime_type`   VARCHAR(128) NOT NULL COMMENT 'MIME（白名单：application/pdf、text/csv、application/zip）',
  `file_size`   BIGINT       NOT NULL COMMENT '字节数',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0失效',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `create_time`),
  KEY `idx_biz` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交付文件表';

-- ---------- 守望订阅（v2.20） ----------
-- 课程演示：不接真实支付，开通只记录方案权益（守望位 / 阈值 / 保留天数）。
CREATE TABLE IF NOT EXISTS `watch_subscription` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
  `user_id`     BIGINT      NOT NULL COMMENT '用户ID（一人一条）',
  `plan_code`   VARCHAR(20) NOT NULL DEFAULT 'light' COMMENT '方案：light轻守 / steady长守 / crowd众守',
  `cycle`       VARCHAR(12) NOT NULL DEFAULT 'month' COMMENT '周期：month按月 / semester按学期',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1生效 0已取消',
  `start_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本次开通时间',
  `expire_time` DATETIME    DEFAULT NULL COMMENT '到期时间（NULL=长期有效）',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='守望订阅表（v2.20，课程演示不接支付）';

-- ---------- 守望位（v2.20） ----------
CREATE TABLE IF NOT EXISTS `watch_target` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '守望ID',
  `user_id`          BIGINT       NOT NULL COMMENT '守望人ID',
  `phash`            BIGINT       NOT NULL COMMENT '64位感知哈希（与 item_image.phash 同口径）',
  `label`            VARCHAR(60)  NOT NULL DEFAULT '' COMMENT '守望照片备注',
  `img_url`          VARCHAR(255) DEFAULT NULL COMMENT '守望照片相对路径（列表缩略图）',
  `source_item_id`   BIGINT       DEFAULT NULL COMMENT '来源启事ID（从详情页开启守望时记录）',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '1守望中 0已结束',
  `hit_count`        INT          NOT NULL DEFAULT 0 COMMENT '命中次数',
  `last_hit_item_id` BIGINT       DEFAULT NULL COMMENT '最近一次命中的启事ID',
  `expire_time`      DATETIME     DEFAULT NULL COMMENT '守望到期时间（NULL=不过期）',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_phash` (`phash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='守望位表（v2.20：新招领通过审核时反查本表）';
