-- MySQL dump 10.13  Distrib 9.7.0, for Win64 (x86_64)
--
-- Host: localhost    Database: lost_found
-- ------------------------------------------------------
-- Server version	9.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--


--
-- Dumping data for table `admin_log`
--

/*!40000 ALTER TABLE `admin_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_log` ENABLE KEYS */;

--
-- Dumping data for table `claim_record`
--

/*!40000 ALTER TABLE `claim_record` DISABLE KEYS */;
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (1,1,2,'演示凭证1：物品内侧有我的姓名贴/划痕特征','13800000000','审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (2,1,4,'演示凭证2：物品内侧有我的姓名贴/划痕特征','13800000001','已通过','确认是失主，已通过','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (3,1,2,'演示凭证3：物品内侧有我的姓名贴/划痕特征','13800000002','已拒绝','特征不符，已拒绝','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (4,2,2,'演示凭证1：物品内侧有我的姓名贴/划痕特征','13800000000','审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (5,2,4,'演示凭证2：物品内侧有我的姓名贴/划痕特征','13800000001','已通过','确认是失主，已通过','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (6,2,2,'演示凭证3：物品内侧有我的姓名贴/划痕特征','13800000002','已拒绝','特征不符，已拒绝','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (7,3,2,'演示凭证1：物品内侧有我的姓名贴/划痕特征','13800000000','审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (8,3,4,'演示凭证2：物品内侧有我的姓名贴/划痕特征','13800000001','已通过','确认是失主，已通过','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (9,3,2,'演示凭证3：物品内侧有我的姓名贴/划痕特征','13800000002','已拒绝','特征不符，已拒绝','2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (10,4,2,'演示凭证：只有失主知道的细节特征',NULL,'审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (11,5,2,'演示凭证：只有失主知道的细节特征',NULL,'审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `claim_record` (`id`, `item_id`, `claimant_id`, `proof`, `contact`, `status`, `reply`, `create_time`, `update_time`) VALUES (12,6,2,'演示凭证：只有失主知道的细节特征',NULL,'审核中',NULL,'2026-10-14 21:58:45','2026-10-14 21:58:45');
/*!40000 ALTER TABLE `claim_record` ENABLE KEYS */;

--
-- Dumping data for table `item`
--

/*!40000 ALTER TABLE `item` DISABLE KEYS */;
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (1,'lost','蓝色保温杯，杯身有小熊贴纸','其他','第一教学楼','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/cup-0.jpg',40,9,1,'2026-10-14 21:58:43','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (2,'found','捡到蓝色保温杯（小熊贴纸）','其他','图书馆','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/cup-1.jpg',57,7,1,'2026-10-14 21:58:43','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (3,'lost','丢失米色盖保温杯','其他','第二食堂','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/cup-2.jpg',74,5,1,'2026-10-14 21:58:43','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (4,'found','蓝色小熊贴纸保温杯','其他','操场','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','认领申请中','/uploads/seed/cup-3.jpg',91,18,1,'2026-10-14 21:58:43','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (5,'lost','保温杯（杯盖米色）遗落','其他','宿舍楼','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','认领申请中','/uploads/seed/cup-4.jpg',108,16,1,'2026-10-14 21:58:43','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (6,'found','捡到蓝色保温杯一只','其他','实验楼','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','认领申请中','/uploads/seed/cup-0.jpg',125,14,1,'2026-10-14 21:58:44','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (7,'lost','寻：蓝色保温杯','其他','校车站','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/cup-1.jpg',142,12,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (8,'found','蓝色保温杯待认领','其他','体育馆','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/cup-2.jpg',159,3,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (9,'lost','丢失蓝色小熊保温杯','其他','第一教学楼','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/cup-3.jpg',176,2,0,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (10,'found','招领：蓝色保温杯','其他','图书馆','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/cup-4.jpg',193,20,0,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (11,'lost','一串钥匙带小熊挂件','钥匙','图书馆','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/keys-0.jpg',45,18,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (12,'found','丢失一串钥匙（小熊挂件）','钥匙','第二食堂','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/keys-1.jpg',62,16,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (13,'lost','捡到四把钥匙+挂件','钥匙','操场','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/keys-2.jpg',79,14,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (14,'found','钥匙串（小熊挂件）待认领','钥匙','宿舍楼','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/keys-3.jpg',96,12,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (15,'lost','寻：宿舍楼下丢的钥匙','钥匙','实验楼','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/keys-4.jpg',113,3,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (16,'found','捡到钥匙一串','钥匙','校车站','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/keys-0.jpg',130,2,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (17,'lost','丢失钥匙带小熊挂件','钥匙','体育馆','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/keys-1.jpg',147,20,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (18,'found','钥匙串待失主认领','钥匙','第一教学楼','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/keys-2.jpg',164,11,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (19,'lost','丢了一串钥匙急寻','钥匙','图书馆','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/keys-3.jpg',181,10,0,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (20,'found','招领：钥匙串带挂件','钥匙','第二食堂','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/keys-4.jpg',198,8,0,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (21,'lost','白色无线耳机一副','电子设备','第二食堂','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/earbuds-0.jpg',50,12,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (22,'found','捡到白色无线耳机','电子设备','操场','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/earbuds-1.jpg',67,3,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (23,'lost','丢失白色耳机+充电盒','电子设备','宿舍楼','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/earbuds-2.jpg',84,2,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (24,'found','白色耳机充电盒有划痕','电子设备','实验楼','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/earbuds-3.jpg',101,20,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (25,'lost','寻：白色无线耳机','电子设备','校车站','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/earbuds-4.jpg',118,11,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (26,'found','捡到耳机及充电盒','电子设备','体育馆','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/earbuds-0.jpg',135,10,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (27,'lost','丢失白色耳机一副','电子设备','第一教学楼','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/earbuds-1.jpg',152,8,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (28,'found','白色无线耳机待认领','电子设备','图书馆','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/earbuds-2.jpg',169,6,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (29,'lost','耳机充电盒遗落操场','电子设备','第二食堂','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/earbuds-3.jpg',186,19,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (30,'found','招领：白色无线耳机','电子设备','操场','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/earbuds-4.jpg',203,17,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (31,'lost','学生证一张（透明卡套）','证件卡类','操场','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/card-0.jpg',55,20,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (32,'found','捡到学生证（带挂绳）','证件卡类','宿舍楼','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/card-1.jpg',72,11,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (33,'lost','丢失学生证急寻','证件卡类','实验楼','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/card-2.jpg',89,10,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (34,'found','学生证待失主认领','证件卡类','校车站','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/card-3.jpg',106,8,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (35,'lost','寻：透明卡套学生证','证件卡类','体育馆','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/card-4.jpg',123,6,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (36,'found','捡到学生证已交班委','证件卡类','第一教学楼','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/card-0.jpg',140,19,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (37,'lost','丢失学生证一张','证件卡类','图书馆','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/card-1.jpg',157,17,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (38,'found','学生证（挂绳款）待认领','证件卡类','第二食堂','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/card-2.jpg',174,15,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (39,'lost','校车站捡到学生证','证件卡类','操场','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/card-3.jpg',191,13,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (40,'found','招领：学生证带卡套','证件卡类','宿舍楼','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/card-4.jpg',208,4,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (41,'lost','粉色雨伞，伞柄有蝴蝶结','其他','宿舍楼','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/umbrella-0.jpg',60,8,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (42,'found','捡到粉色蝴蝶结雨伞','其他','实验楼','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/umbrella-1.jpg',77,6,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (43,'lost','丢失粉色雨伞一把','其他','校车站','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/umbrella-2.jpg',94,19,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (44,'found','粉色雨伞待认领','其他','体育馆','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/umbrella-3.jpg',111,17,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (45,'lost','寻：蝴蝶结柄粉伞','其他','第一教学楼','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/umbrella-4.jpg',128,15,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (46,'found','捡到粉色雨伞','其他','图书馆','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/umbrella-0.jpg',145,13,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (47,'lost','丢失粉色蝴蝶结雨伞','其他','第二食堂','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/umbrella-1.jpg',162,4,1,'2026-10-14 21:58:44','2026-10-14 21:58:44');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (48,'found','粉伞（伞骨微弯）待认领','其他','操场','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/umbrella-2.jpg',179,21,1,'2026-10-14 21:58:44','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (49,'lost','图书馆遗落粉色雨伞','其他','宿舍楼','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/umbrella-3.jpg',196,9,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (50,'found','招领：粉色雨伞','其他','实验楼','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/umbrella-4.jpg',213,7,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (51,'lost','灰色格纹流苏围巾','衣物饰品','实验楼','2026-09-20 08:00:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/scarf-0.jpg',65,17,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (52,'found','捡到灰色格纹围巾','衣物饰品','校车站','2026-09-21 09:07:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/scarf-1.jpg',82,15,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (53,'lost','丢失灰色围巾一条','衣物饰品','体育馆','2026-09-22 10:14:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/scarf-2.jpg',99,13,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (54,'found','灰围巾（流苏款）待认领','衣物饰品','第一教学楼','2026-09-23 11:21:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/scarf-3.jpg',116,4,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (55,'lost','寻：灰色格纹围巾','衣物饰品','图书馆','2026-09-24 12:28:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/scarf-4.jpg',133,21,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (56,'found','捡到灰色围巾','衣物饰品','第二食堂','2026-09-25 13:35:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/scarf-0.jpg',150,9,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (57,'lost','丢失灰色流苏围巾','衣物饰品','操场','2026-09-26 14:42:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','寻找中','/uploads/seed/scarf-1.jpg',167,7,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (58,'found','灰色围巾待失主','衣物饰品','宿舍楼','2026-09-27 15:49:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','待认领','/uploads/seed/scarf-2.jpg',184,5,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (59,'lost','操场主席台捡到灰围巾','衣物饰品','实验楼','2026-09-20 16:56:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已找回','/uploads/seed/scarf-3.jpg',201,18,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
INSERT INTO `item` (`id`, `type`, `title`, `category`, `location`, `happen_time`, `description`, `status`, `cover_img`, `views`, `user_id`, `audit_status`, `create_time`, `update_time`) VALUES (60,'found','招领：灰色格纹围巾','衣物饰品','校车站','2026-09-21 17:03:00','物品特征详见照片；如有线索请在详情页申请认领或私信联系，感谢！（演示数据）','已认领','/uploads/seed/scarf-4.jpg',218,16,1,'2026-10-14 21:58:45','2026-10-14 21:58:45');
/*!40000 ALTER TABLE `item` ENABLE KEYS */;

--
-- Dumping data for table `item_image`
--

/*!40000 ALTER TABLE `item_image` DISABLE KEYS */;
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (1,1,'/uploads/seed/cup-0.jpg',-1176191252217113454,0,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (2,1,'/uploads/seed/cup-1.jpg',-5782780244652083046,1,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (3,2,'/uploads/seed/cup-1.jpg',-5782780244652083046,0,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (4,2,'/uploads/seed/cup-2.jpg',-1176191252217113454,1,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (5,3,'/uploads/seed/cup-2.jpg',-1176191252217113454,0,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (6,3,'/uploads/seed/cup-3.jpg',-1176191252217113454,1,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (7,4,'/uploads/seed/cup-3.jpg',-1176191252217113454,0,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (8,4,'/uploads/seed/cup-4.jpg',-1216692862677323630,1,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (9,5,'/uploads/seed/cup-4.jpg',-1216692862677323630,0,'2026-10-14 21:58:43');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (10,5,'/uploads/seed/cup-0.jpg',-1176191252217113454,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (11,6,'/uploads/seed/cup-0.jpg',-1176191252217113454,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (12,6,'/uploads/seed/cup-1.jpg',-5782780244652083046,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (13,7,'/uploads/seed/cup-1.jpg',-5782780244652083046,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (14,7,'/uploads/seed/cup-2.jpg',-1176191252217113454,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (15,8,'/uploads/seed/cup-2.jpg',-1176191252217113454,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (16,8,'/uploads/seed/cup-3.jpg',-1176191252217113454,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (17,9,'/uploads/seed/cup-3.jpg',-1176191252217113454,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (18,9,'/uploads/seed/cup-4.jpg',-1216692862677323630,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (19,10,'/uploads/seed/cup-4.jpg',-1216692862677323630,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (20,10,'/uploads/seed/cup-0.jpg',-1176191252217113454,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (21,11,'/uploads/seed/keys-0.jpg',2032086357544385782,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (22,11,'/uploads/seed/keys-1.jpg',6445752557674019926,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (23,12,'/uploads/seed/keys-1.jpg',6445752557674019926,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (24,12,'/uploads/seed/keys-2.jpg',2032086357544385782,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (25,13,'/uploads/seed/keys-2.jpg',2032086357544385782,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (26,13,'/uploads/seed/keys-3.jpg',2032086357561162998,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (27,14,'/uploads/seed/keys-3.jpg',2032086357561162998,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (28,14,'/uploads/seed/keys-4.jpg',1762002344923995254,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (29,15,'/uploads/seed/keys-4.jpg',1762002344923995254,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (30,15,'/uploads/seed/keys-0.jpg',2032086357544385782,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (31,16,'/uploads/seed/keys-0.jpg',2032086357544385782,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (32,16,'/uploads/seed/keys-1.jpg',6445752557674019926,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (33,17,'/uploads/seed/keys-1.jpg',6445752557674019926,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (34,17,'/uploads/seed/keys-2.jpg',2032086357544385782,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (35,18,'/uploads/seed/keys-2.jpg',2032086357544385782,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (36,18,'/uploads/seed/keys-3.jpg',2032086357561162998,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (37,19,'/uploads/seed/keys-3.jpg',2032086357561162998,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (38,19,'/uploads/seed/keys-4.jpg',1762002344923995254,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (39,20,'/uploads/seed/keys-4.jpg',1762002344923995254,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (40,20,'/uploads/seed/keys-0.jpg',2032086357544385782,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (41,21,'/uploads/seed/earbuds-0.jpg',3294059981336635025,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (42,21,'/uploads/seed/earbuds-1.jpg',-6226410542619793765,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (43,22,'/uploads/seed/earbuds-1.jpg',-6226410542619793765,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (44,22,'/uploads/seed/earbuds-2.jpg',2424865253254129297,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (45,23,'/uploads/seed/earbuds-2.jpg',2424865253254129297,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (46,23,'/uploads/seed/earbuds-3.jpg',3294059981336635025,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (47,24,'/uploads/seed/earbuds-3.jpg',3294059981336635025,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (48,24,'/uploads/seed/earbuds-4.jpg',3222284137220427409,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (49,25,'/uploads/seed/earbuds-4.jpg',3222284137220427409,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (50,25,'/uploads/seed/earbuds-0.jpg',3294059981336635025,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (51,26,'/uploads/seed/earbuds-0.jpg',3294059981336635025,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (52,26,'/uploads/seed/earbuds-1.jpg',-6226410542619793765,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (53,27,'/uploads/seed/earbuds-1.jpg',-6226410542619793765,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (54,27,'/uploads/seed/earbuds-2.jpg',2424865253254129297,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (55,28,'/uploads/seed/earbuds-2.jpg',2424865253254129297,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (56,28,'/uploads/seed/earbuds-3.jpg',3294059981336635025,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (57,29,'/uploads/seed/earbuds-3.jpg',3294059981336635025,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (58,29,'/uploads/seed/earbuds-4.jpg',3222284137220427409,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (59,30,'/uploads/seed/earbuds-4.jpg',3222284137220427409,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (60,30,'/uploads/seed/earbuds-0.jpg',3294059981336635025,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (61,31,'/uploads/seed/card-0.jpg',1928444304030776067,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (62,31,'/uploads/seed/card-1.jpg',-4450747801847907457,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (63,32,'/uploads/seed/card-1.jpg',-4450747801847907457,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (64,32,'/uploads/seed/card-2.jpg',1928444304030767939,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (65,33,'/uploads/seed/card-2.jpg',1928444304030767939,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (66,33,'/uploads/seed/card-3.jpg',1928444304030710531,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (67,34,'/uploads/seed/card-3.jpg',1928444304030710531,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (68,34,'/uploads/seed/card-4.jpg',-1003207565506430201,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (69,35,'/uploads/seed/card-4.jpg',-1003207565506430201,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (70,35,'/uploads/seed/card-0.jpg',1928444304030776067,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (71,36,'/uploads/seed/card-0.jpg',1928444304030776067,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (72,36,'/uploads/seed/card-1.jpg',-4450747801847907457,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (73,37,'/uploads/seed/card-1.jpg',-4450747801847907457,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (74,37,'/uploads/seed/card-2.jpg',1928444304030767939,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (75,38,'/uploads/seed/card-2.jpg',1928444304030767939,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (76,38,'/uploads/seed/card-3.jpg',1928444304030710531,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (77,39,'/uploads/seed/card-3.jpg',1928444304030710531,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (78,39,'/uploads/seed/card-4.jpg',-1003207565506430201,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (79,40,'/uploads/seed/card-4.jpg',-1003207565506430201,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (80,40,'/uploads/seed/card-0.jpg',1928444304030776067,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (81,41,'/uploads/seed/umbrella-0.jpg',-229679526834859921,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (82,41,'/uploads/seed/umbrella-1.jpg',6634061666135896495,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (83,42,'/uploads/seed/umbrella-1.jpg',6634061666135896495,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (84,42,'/uploads/seed/umbrella-2.jpg',-229679526835056529,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (85,43,'/uploads/seed/umbrella-2.jpg',-229679526835056529,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (86,43,'/uploads/seed/umbrella-3.jpg',-229679526835122065,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (87,44,'/uploads/seed/umbrella-3.jpg',-229679526835122065,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (88,44,'/uploads/seed/umbrella-4.jpg',-283546525365136977,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (89,45,'/uploads/seed/umbrella-4.jpg',-283546525365136977,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (90,45,'/uploads/seed/umbrella-0.jpg',-229679526834859921,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (91,46,'/uploads/seed/umbrella-0.jpg',-229679526834859921,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (92,46,'/uploads/seed/umbrella-1.jpg',6634061666135896495,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (93,47,'/uploads/seed/umbrella-1.jpg',6634061666135896495,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (94,47,'/uploads/seed/umbrella-2.jpg',-229679526835056529,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (95,48,'/uploads/seed/umbrella-2.jpg',-229679526835056529,0,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (96,48,'/uploads/seed/umbrella-3.jpg',-229679526835122065,1,'2026-10-14 21:58:44');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (97,49,'/uploads/seed/umbrella-3.jpg',-229679526835122065,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (98,49,'/uploads/seed/umbrella-4.jpg',-283546525365136977,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (99,50,'/uploads/seed/umbrella-4.jpg',-283546525365136977,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (100,50,'/uploads/seed/umbrella-0.jpg',-229679526834859921,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (101,51,'/uploads/seed/scarf-0.jpg',-4139411156097305509,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (102,51,'/uploads/seed/scarf-1.jpg',6844761021274899067,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (103,52,'/uploads/seed/scarf-1.jpg',6844761021274899067,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (104,52,'/uploads/seed/scarf-2.jpg',-4139411156097305477,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (105,53,'/uploads/seed/scarf-2.jpg',-4139411156097305477,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (106,53,'/uploads/seed/scarf-3.jpg',-4139411156097305477,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (107,54,'/uploads/seed/scarf-3.jpg',-4139411156097305477,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (108,54,'/uploads/seed/scarf-4.jpg',-3567561875551432581,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (109,55,'/uploads/seed/scarf-4.jpg',-3567561875551432581,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (110,55,'/uploads/seed/scarf-0.jpg',-4139411156097305509,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (111,56,'/uploads/seed/scarf-0.jpg',-4139411156097305509,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (112,56,'/uploads/seed/scarf-1.jpg',6844761021274899067,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (113,57,'/uploads/seed/scarf-1.jpg',6844761021274899067,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (114,57,'/uploads/seed/scarf-2.jpg',-4139411156097305477,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (115,58,'/uploads/seed/scarf-2.jpg',-4139411156097305477,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (116,58,'/uploads/seed/scarf-3.jpg',-4139411156097305477,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (117,59,'/uploads/seed/scarf-3.jpg',-4139411156097305477,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (118,59,'/uploads/seed/scarf-4.jpg',-3567561875551432581,1,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (119,60,'/uploads/seed/scarf-4.jpg',-3567561875551432581,0,'2026-10-14 21:58:45');
INSERT INTO `item_image` (`id`, `item_id`, `img_url`, `phash`, `sort`, `create_time`) VALUES (120,60,'/uploads/seed/scarf-0.jpg',-4139411156097305509,1,'2026-10-14 21:58:45');
/*!40000 ALTER TABLE `item_image` ENABLE KEYS */;

--
-- Dumping data for table `message`
--

/*!40000 ALTER TABLE `message` DISABLE KEYS */;
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (1,9,'claim','收到新的认领申请','有人申请认领「蓝色保温杯，杯身有小熊贴纸」，请查看凭证并审核。',1,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (2,9,'claim','收到新的认领申请','有人申请认领「蓝色保温杯，杯身有小熊贴纸」，请查看凭证并审核。',1,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (3,9,'claim','收到新的认领申请','有人申请认领「蓝色保温杯，杯身有小熊贴纸」，请查看凭证并审核。',1,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (4,7,'claim','收到新的认领申请','有人申请认领「捡到蓝色保温杯（小熊贴纸）」，请查看凭证并审核。',2,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (5,7,'claim','收到新的认领申请','有人申请认领「捡到蓝色保温杯（小熊贴纸）」，请查看凭证并审核。',2,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (6,7,'claim','收到新的认领申请','有人申请认领「捡到蓝色保温杯（小熊贴纸）」，请查看凭证并审核。',2,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (7,5,'claim','收到新的认领申请','有人申请认领「丢失米色盖保温杯」，请查看凭证并审核。',3,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (8,5,'claim','收到新的认领申请','有人申请认领「丢失米色盖保温杯」，请查看凭证并审核。',3,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (9,5,'claim','收到新的认领申请','有人申请认领「丢失米色盖保温杯」，请查看凭证并审核。',3,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (10,18,'claim','收到新的认领申请','有人申请认领「蓝色小熊贴纸保温杯」，请查看凭证并审核。',4,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (11,16,'claim','收到新的认领申请','有人申请认领「保温杯（杯盖米色）遗落」，请查看凭证并审核。',5,0,'2026-10-14 21:58:45');
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `ref_id`, `is_read`, `create_time`) VALUES (12,14,'claim','收到新的认领申请','有人申请认领「捡到蓝色保温杯一只」，请查看凭证并审核。',6,0,'2026-10-14 21:58:45');
/*!40000 ALTER TABLE `message` ENABLE KEYS */;

--
-- Dumping data for table `user`
--

/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (1,'10000001','$2a$10$6/U25DCi/0YyJs04il4bxuXxwnwK9Wrv0a9j1oLpBUSDi5DMR0Z6G','管理员','🛡️',NULL,1,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (2,'20240009','$2a$10$7O.KnaTqteBkGo6Eip/87O/fIBhvYvvJmPjxA/8sqeuDjGED78j7m','郑同学','🐸',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (3,'20240008','$2a$10$ArODhvneKtNJFhtjQeaJ2uF0DJbDO6DZLKNvuDxuv2LOvSz9.6652','吴同学','🐰',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (4,'20240019','$2a$10$J01KYfrRPDxuBCCBDMdwdu0VIz6IQsTMGrz339pK8SEDxr08hZDhW','秦同学','🐰',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (5,'20240003','$2a$10$QSjviY2NhML4ma5NdDLERO8Oz/qJ6bmZcLRJotES9B.FS3sHV0cH2','王同学','🦁',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (6,'20240014','$2a$10$d2jyWDOqfPE8MP.cwfNaXuZ7wZBJ4UfY3ubREbGKojiCdPMjpDTKi','蒋同学','🐯',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (7,'20240002','$2a$10$bX53TolLamUtmC/hHB4pQuUtNvVkdt01z7Ppge4kg5MJOa8ANpxSu','陈同学','🐵',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (8,'20240013','$2a$10$JlhMO8xMk3d2i5jXOU8xDOQgjG1xCEnwmdnAamGxYJLc9H48ltdpC','卫同学','🐸',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (9,'20240001','$2a$10$47eONP0AnGcAwyvlIchkZu31beussTAGEoc8Wq0RUUJVc9R8SlRJO','林同学','🐵',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (10,'20240012','$2a$10$OezW8mZg50BxcOFPs/mV2uLlZncqrAtsvxxbqyuxaOB3bkUs0DJFW','褚同学','🦊',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (11,'20240011','$2a$10$mV2jKy..aZY4qa5yafjhuOjxKqR7kIDadsCzwFw9xkuu5JvKzEG6a','冯同学','🐰',NULL,0,1,'2026-10-14 21:58:42','2026-10-14 21:58:42');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (12,'20240007','$2a$10$ii0YI1bHAUrEV4fzGel.b.2OBkiebTB86A7pBULZTN/qBUN5tCHU6','周同学','🦄',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (13,'20240018','$2a$10$G32SmOlGmhvoko40skbGmOTIox0ql2Qq/8XXcj6N09Pc30Xkfzo6m','朱同学','🐵',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (14,'20240006','$2a$10$SxOmEVbpLWHNU568hOexlu9Jg0IVc2LU6n4C9408CHD/vb0AuHjhS','孙同学','🦁',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (15,'20240017','$2a$10$6JO53DT8VkfcoIoEBTvQJeGQaOKnyLQNqHESSPyRYbfq/57Wb0WrS','杨同学','🐵',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (16,'20240005','$2a$10$kKZEwEHj3xkLtRE0y48cvuYAdnakfWGi8j.3lXkd6OFILtMuMkena','刘同学','🐨',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (17,'20240016','$2a$10$ASWrYdnoVIYXE/ggvltQVutpqzacAV4SRYwHwXr6C1wFj14dzzgxK','韩同学','🐼',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (18,'20240004','$2a$10$VU7dtAgPx4T3anxSl7pYl.bo90NNVI0KsfLz0HpWVi9e6XmTWUfp.','赵同学','🐼',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (19,'20240015','$2a$10$Hx/h36rWxyRCeLpSF1f/D.jCGdqCvQFTzAwAGTkvVc40wCEQDW2OS','沈同学','🐨',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (20,'20240010','$2a$10$Hds1vAXazOJSNJL/pqQprOsR8YWiBikhfqtwEb4shRNRl3iKHyxnC','钱同学','🐰',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
INSERT INTO `user` (`id`, `account`, `password`, `nickname`, `avatar`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES (21,'20240020','$2a$10$Q0HhuEN4LpTiFQ2VVOQ7QuoZ.DXSWdP6oDvX4GMzUXhTVYuAYyhnO','许同学','🦁',NULL,0,1,'2026-10-14 21:58:43','2026-10-14 21:58:43');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-14 21:58:57
