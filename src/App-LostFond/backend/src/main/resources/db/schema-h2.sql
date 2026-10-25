-- =============================================================
-- 拾光 App 独立版 · H2 建表脚本（MySQL 兼容模式，可重复执行）
-- 与 db/schema-init.sql 表结构一致；去掉了 MySQL 专有的 ENGINE/CHARSET/内联 KEY，
-- 索引只保留业务唯一约束（演示库规模小，非唯一索引无收益）。
-- JDBC URL 必须带 NON_KEYWORDS=USER,VALUE,TYPE,STATUS,ACTION,CYCLE,KEY，
-- 否则保留字会与 user/status/cycle 等表名或列名冲突。
-- =============================================================

CREATE TABLE IF NOT EXISTS user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  account     VARCHAR(20)  NOT NULL,
  password    VARCHAR(100) NOT NULL,
  nickname    VARCHAR(30)  NOT NULL DEFAULT '',
  avatar      VARCHAR(16)  NOT NULL DEFAULT '🦊',
  phone       VARCHAR(20)  DEFAULT NULL,
  role        TINYINT      NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_account UNIQUE (account)
);

CREATE TABLE IF NOT EXISTS item (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  type         VARCHAR(10)  NOT NULL,
  title        VARCHAR(60)  NOT NULL,
  category     VARCHAR(20)  NOT NULL DEFAULT '其他',
  location     VARCHAR(50)  NOT NULL DEFAULT '',
  latitude     DECIMAL(10,6) DEFAULT NULL,
  longitude    DECIMAL(10,6) DEFAULT NULL,
  happen_time  DATETIME     DEFAULT NULL,
  description  VARCHAR(500) DEFAULT '',
  status       VARCHAR(20)  NOT NULL DEFAULT '寻找中',
  cover_img    VARCHAR(255) DEFAULT NULL,
  views        INT          NOT NULL DEFAULT 0,
  user_id      BIGINT       NOT NULL,
  audit_status TINYINT      NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS item_image (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  item_id     BIGINT       NOT NULL,
  img_url     VARCHAR(255) NOT NULL,
  phash       BIGINT       DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS claim_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  item_id     BIGINT       NOT NULL,
  claimant_id BIGINT       NOT NULL,
  proof       VARCHAR(300) NOT NULL DEFAULT '',
  contact     VARCHAR(50)  DEFAULT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT '审核中',
  reply       VARCHAR(200) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS message (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL,
  from_user_id BIGINT       DEFAULT NULL,
  type         VARCHAR(20)  NOT NULL DEFAULT 'system',
  title        VARCHAR(100) NOT NULL DEFAULT '',
  content      VARCHAR(300) DEFAULT '',
  ref_id       BIGINT       DEFAULT NULL,
  is_read      TINYINT      NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  admin_id    BIGINT       NOT NULL,
  action      VARCHAR(50)  NOT NULL,
  target_type VARCHAR(20)  NOT NULL DEFAULT '',
  target_id   BIGINT       DEFAULT NULL,
  detail      VARCHAR(300) DEFAULT '',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  item_id     BIGINT   NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_favorite_user_item UNIQUE (user_id, item_id)
);

CREATE TABLE IF NOT EXISTS delivery (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  biz_type    VARCHAR(32)  NOT NULL,
  biz_id      BIGINT       DEFAULT NULL,
  title       VARCHAR(128) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  file_path   VARCHAR(512) NOT NULL,
  file_name   VARCHAR(255) NOT NULL,
  mime_type   VARCHAR(128) NOT NULL,
  file_size   BIGINT       NOT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS watch_subscription (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  plan_code   VARCHAR(20) NOT NULL DEFAULT 'light',
  cycle       VARCHAR(12) NOT NULL DEFAULT 'month',
  status      TINYINT     NOT NULL DEFAULT 1,
  start_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_time DATETIME    DEFAULT NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_watch_subscription_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS watch_target (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  phash            BIGINT       NOT NULL,
  label            VARCHAR(60)  NOT NULL DEFAULT '',
  img_url          VARCHAR(255) DEFAULT NULL,
  source_item_id   BIGINT       DEFAULT NULL,
  status           TINYINT      NOT NULL DEFAULT 1,
  hit_count        INT          NOT NULL DEFAULT 0,
  last_hit_item_id BIGINT       DEFAULT NULL,
  expire_time      DATETIME     DEFAULT NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
