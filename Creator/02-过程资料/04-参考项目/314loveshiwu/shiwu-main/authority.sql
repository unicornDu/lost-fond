/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3307
 Source Server Type    : MySQL
 Source Server Version : 50528
 Source Host           : localhost:3307
 Source Schema         : authority

 Target Server Type    : MySQL
 Target Server Version : 50528
 File Encoding         : 65001

 Date: 23/07/2023 12:11:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for goods
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '序列号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失物名称',
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失物图片',
  `place` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拾取地点',
  `des` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '失物详细说明',
  `date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拾取日期',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拾取物品管理' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of goods
-- ----------------------------

-- ----------------------------
-- Table structure for help
-- ----------------------------
DROP TABLE IF EXISTS `help`;
CREATE TABLE `help`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '序列号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '丢失物品名称',
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物品图片',
  `date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '丢失日期',
  `place` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '丢失地点',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '丢失物品管理' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of help
-- ----------------------------
INSERT INTO `help` VALUES (1, 'xx', 'http://localhost:8888/files/1690084185343800.jpg', '2023-07-31', 'ss', '19890905753', 'sds');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父菜单ID，一级菜单为0',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单URL',
  `perms` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '授权(多个用逗号分隔，如：user:list,user:create)',
  `component` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `type` int(5) NOT NULL DEFAULT 0 COMMENT '类型     0：目录   1：菜单   2：按钮',
  `icon` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `orderNum` int(11) NULL DEFAULT NULL COMMENT '排序',
  `created` datetime NOT NULL,
  `updated` datetime NULL DEFAULT NULL,
  `statu` int(5) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 128 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (2, 0, '用户管理', '/sys/users', 'sys:user:list', 'sys/User', 1, 'el-icon-s-custom', 2, '2023-07-23 19:03:45', '2023-07-23 11:41:11', 1);
INSERT INTO `sys_menu` VALUES (3, 0, '角色管理', '/sys/roles', 'sys:role:list', 'sys/Role', 1, 'el-icon-rank', 3, '2023-07-23 19:03:45', '2023-07-23 11:40:37', 1);
INSERT INTO `sys_menu` VALUES (4, 0, '菜单管理', '/sys/menus', 'sys:menu:list', 'sys/Menu', 1, 'el-icon-menu', 4, '2023-07-23 19:03:45', '2023-07-23 11:41:28', 0);
INSERT INTO `sys_menu` VALUES (24, 0, '个人信息', '/sys/persons', 'sys:person:list', 'sys/Person', 1, 'el-icon-user-solid', 1, '2023-07-23 12:28:13', '2023-07-23 11:41:19', 1);
INSERT INTO `sys_menu` VALUES (126, 0, '拾取物品管理', '/sys/goods', 'sys:goods:list', 'Goods', 1, 'el-icon-s-claim', 5, '2023-07-23 11:00:36', '2023-07-23 11:41:35', 1);
INSERT INTO `sys_menu` VALUES (127, 0, '丢失物品管理', '/sys/help', 'sys:help:list', 'Help', 1, 'el-icon-s-release', 6, '2023-07-23 11:11:29', '2023-07-23 11:41:41', 1);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `remark` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created` datetime NULL DEFAULT NULL,
  `updated` datetime NULL DEFAULT NULL,
  `statu` int(5) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE,
  UNIQUE INDEX `code`(`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (3, '普通用户', 'normal', '只有基本功能', '2021-01-04 10:09:14', '2021-01-30 08:19:52', 1);
INSERT INTO `sys_role` VALUES (6, '超级管理员', 'admin', '系统默认最高权限', '2021-01-16 13:29:03', '2023-01-10 19:05:46', 1);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL,
  `menu_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 289 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (276, 6, 1);
INSERT INTO `sys_role_menu` VALUES (277, 6, 2);
INSERT INTO `sys_role_menu` VALUES (278, 6, 3);
INSERT INTO `sys_role_menu` VALUES (279, 6, 4);
INSERT INTO `sys_role_menu` VALUES (280, 6, 114);
INSERT INTO `sys_role_menu` VALUES (281, 6, 24);
INSERT INTO `sys_role_menu` VALUES (282, 6, 125);
INSERT INTO `sys_role_menu` VALUES (283, 6, 126);
INSERT INTO `sys_role_menu` VALUES (284, 6, 127);
INSERT INTO `sys_role_menu` VALUES (285, 3, 24);
INSERT INTO `sys_role_menu` VALUES (286, 3, 2);
INSERT INTO `sys_role_menu` VALUES (287, 3, 126);
INSERT INTO `sys_role_menu` VALUES (288, 3, 127);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `email` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `city` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `created` datetime NULL DEFAULT NULL,
  `updated` datetime NULL DEFAULT NULL,
  `last_login` datetime NULL DEFAULT NULL,
  `statu` int(5) NOT NULL,
  `phone` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_USERNAME`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$bLtt8fceWbCNvw37kv2lUuTKXBXJonCliOZCRLTFHe7CIR0he89W6', 'http://localhost:8888/files/1674204129286151.png', '123@qq.com', '广州市', '2021-01-12 22:13:53', '2023-07-23 11:42:16', '2020-12-30 08:38:37', 1, 18237613715);
INSERT INTO `sys_user` VALUES (15, 'dyy123', '$2a$10$nTZ2Z.wwNgK2.j8BG4JYJ.WAyHVxfoynntbnBgdHXQ/iGor65OakG', 'http://localhost:8888/files/1673529559138083.jpeg', '234567@qq.com', NULL, NULL, '2023-01-20 16:21:27', NULL, 0, 2345678);
INSERT INTO `sys_user` VALUES (20, 'wwwww', '$2a$10$FOVXwCGe72WgDPGSbjEQlOAFxEyhic.DHilcyx.sycpq1/d2aVe0K', 'http://localhost:8888/files/1673529559138083.jpeg', NULL, NULL, NULL, NULL, NULL, 0, NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (20, 3, 3);
INSERT INTO `sys_user_role` VALUES (21, 2, 3);
INSERT INTO `sys_user_role` VALUES (23, 1, 3);
INSERT INTO `sys_user_role` VALUES (24, 1, 6);
INSERT INTO `sys_user_role` VALUES (25, 12, 3);
INSERT INTO `sys_user_role` VALUES (26, 13, 3);
INSERT INTO `sys_user_role` VALUES (27, 14, 3);
INSERT INTO `sys_user_role` VALUES (28, 15, 3);
INSERT INTO `sys_user_role` VALUES (29, 20, 3);

SET FOREIGN_KEY_CHECKS = 1;
