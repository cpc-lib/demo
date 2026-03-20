/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80028
 Source Host           : localhost:3306
 Source Schema         : shiro_jwt_vue_file

 Target Server Type    : MySQL
 Target Server Version : 80028
 File Encoding         : 65001

 Date: 22/04/2023 17:21:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for files
-- ----------------------------
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files`  (
                          `id` int(0) NOT NULL AUTO_INCREMENT COMMENT 'id',
                          `upload_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '分片上传uploadId',
                          `file_md5` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件md5',
                          `url` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '下载链接',
                          `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件名称',
                          `bucket_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '桶名',
                          `file_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件类型',
                          `file_size` bigint(0) NULL DEFAULT NULL COMMENT '文件大小(byte)',
                          `chunk_size` bigint(0) NULL DEFAULT NULL COMMENT '每个分片的大小（byte）',
                          `chunk_num` int(0) NULL DEFAULT NULL COMMENT '分片数量',
                          `is_delete` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除',
                          `enable` tinyint(1) NULL DEFAULT 1 COMMENT '是否禁用链接(0 禁用 1启用)',
                          `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                          `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
                          PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 304 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '文件表' ROW_FORMAT = Dynamic;