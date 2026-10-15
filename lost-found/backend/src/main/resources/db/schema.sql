-- =============================================================
-- 拾光 · 校园失物招领平台  数据库建表脚本
-- Database: lost_found   Engine: InnoDB   Charset: utf8mb4
-- 对应分工方案 M7（成员E）：user / item / item_image / claim_record / message / admin_log
-- 核心亮点：item_image.phash 存 64 位感知哈希（pHash），以图搜图 = BIT_COUNT(phash XOR query)
-- =============================================================

CREATE DATABASE IF NOT EXISTS lost_found
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE lost_found;

-- ---------- 用户表 ----------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
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
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '启事ID',
  `type`         VARCHAR(10)  NOT NULL COMMENT '类型：lost=寻物启事 found=招领启事',
  `title`        VARCHAR(60)  NOT NULL COMMENT '物品名称/标题',
  `category`     VARCHAR(20)  NOT NULL DEFAULT '其他' COMMENT '分类：证件卡类/钥匙/电子设备/书籍资料/衣物饰品/其他',
  `location`     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '地点',
  `happen_time`  DATETIME     DEFAULT NULL COMMENT '丢失/捡到时间',
  `description`  VARCHAR(500) DEFAULT '' COMMENT '详细描述',
  `status`       VARCHAR(20)  NOT NULL DEFAULT '寻找中' COMMENT '业务状态：寻找中/待认领/认领申请中/已找回/已认领/已下架/已关闭',
  `cover_img`    VARCHAR(255) DEFAULT NULL COMMENT '封面图（首图）相对路径',
  `views`        INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `user_id`      BIGINT       NOT NULL COMMENT '发布人ID',
  `audit_status` TINYINT      NOT NULL DEFAULT 1 COMMENT '审核状态：0=待审核 1=已通过 2=已驳回',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`, `status`),
  KEY `idx_category` (`category`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
  -- 审查 2.18 取舍：关键词检索实际使用 LIKE '%k%'（QueryWrapper 不支持全文语法），
  -- 故不建 ngram 全文索引，避免"文档说了但代码没做"。数据量上来后再评估 MATCH...AGAINST。
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物/招领启事表';

-- ---------- 启事图片表（含 pHash 感知哈希） ----------
DROP TABLE IF EXISTS `item_image`;
CREATE TABLE `item_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `item_id`     BIGINT       NOT NULL COMMENT '所属启事ID',
  `img_url`     VARCHAR(255) NOT NULL COMMENT '图片相对路径',
  `phash`       BIGINT       DEFAULT NULL COMMENT '64位感知哈希（有符号BIGINT存储，汉明距离=BIT_COUNT(a XOR b)）',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '图片顺序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_phash` (`phash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='启事图片表（存 pHash 指纹，以图搜图核心）';

-- ---------- 认领申请表 ----------
DROP TABLE IF EXISTS `claim_record`;
CREATE TABLE `claim_record` (
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
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`     BIGINT       NOT NULL COMMENT '接收人ID',
  `type`        VARCHAR(20)  NOT NULL DEFAULT 'system' COMMENT '类型：claim/audit/system',
  `title`       VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题',
  `content`     VARCHAR(300) DEFAULT '' COMMENT '内容',
  `ref_id`      BIGINT       DEFAULT NULL COMMENT '关联业务ID（item/claim）',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知消息表';

-- ---------- 管理员操作日志表 ----------
DROP TABLE IF EXISTS `admin_log`;
CREATE TABLE `admin_log` (
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
