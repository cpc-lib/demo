/*
 Navicat Premium Data Transfer

 Source Server         : 114.132.210.77-腾讯云-3312
 Source Server Type    : MySQL
 Source Server Version : 50744
 Source Host           : 114.132.210.77:3312
 Source Schema         : test

 Target Server Type    : MySQL
 Target Server Version : 50744
 File Encoding         : 65001

 Date: 10/12/2023 21:03:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_location
-- ----------------------------
DROP TABLE IF EXISTS `tb_location`;
CREATE TABLE `tb_location`  (
  `id` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `gis` geometry NOT NULL COMMENT '空间位置信息',
  `geohash` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci GENERATED ALWAYS AS (st_geohash(`gis`,8)) VIRTUAL COMMENT 'geohash' NOT NULL,
  `longitude` decimal(22, 7) NOT NULL,
  `latitude` decimal(22, 7) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  SPATIAL INDEX `idx_gis`(`gis`),
  INDEX `idx_geohash`(`geohash`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_location
-- ----------------------------
INSERT INTO `tb_location` VALUES ('1c54181734894da4bf11bd801c97b0ba', '牛首山森林公园', ST_GeomFromText('POINT(118.7440731 31.9139955)'), DEFAULT, 118.7440731, 31.9139955);
INSERT INTO `tb_location` VALUES ('21d1de0c490145589c068443061f3a1a', '中山陵', ST_GeomFromText('POINT(118.8507685 32.0621805)'), DEFAULT, 118.8507685, 32.0621805);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606300', '侵华日军南京大屠杀遇难同胞纪念馆', ST_GeomFromText('POINT(118.729422 32.0618311)'), DEFAULT, 118.7294220, 32.0618311);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606301', '夫子庙', ST_GeomFromText('POINT(118.7834511 32.0169232)'), DEFAULT, 118.7834511, 32.0169232);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606500', '南京禄口客运站', ST_GeomFromText('POINT(118.7196143 31.770287)'), DEFAULT, 118.7196143, 31.7702870);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606510', '南京南站', ST_GeomFromText('POINT(118.7211177 31.968887)'), DEFAULT, 118.7211177, 31.9688870);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606511', '南京大学', ST_GeomFromText('POINT(118.7510613 32.083129)'), DEFAULT, 118.7510613, 32.0831290);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606512', '南京北站', ST_GeomFromText('POINT(118.4460376 32.0667569)'), DEFAULT, 118.4460376, 32.0667569);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606520', '新街口', ST_GeomFromText('POINT(118.7856726 32.0395687)'), DEFAULT, 118.7856726, 32.0395687);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606521', '栖霞寺', ST_GeomFromText('POINT(118.952774 32.1505693)'), DEFAULT, 118.9527740, 32.1505693);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606530', '老城南菜馆', ST_GeomFromText('POINT(118.7863882 32.0166159)'), DEFAULT, 118.7863882, 32.0166159);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606531', '南京城墙', ST_GeomFromText('POINT(118.8043137 32.0196684)'), DEFAULT, 118.8043137, 32.0196684);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606537', '莫愁湖东路北', ST_GeomFromText('POINT(118.7648799 32.0315439)'), DEFAULT, 118.7648799, 32.0315439);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606539', '南京博物院', ST_GeomFromText('POINT(118.7756682 32.0313765)'), DEFAULT, 118.7756682, 32.0313765);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606540', '朝天宫', ST_GeomFromText('POINT(118.7761011 32.0309915)'), DEFAULT, 118.7761011, 32.0309915);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606541', '老门东', ST_GeomFromText('POINT(118.7867381 32.0108295)'), DEFAULT, 118.7867381, 32.0108295);
INSERT INTO `tb_location` VALUES ('312cf42ee19e4ddebabc2c5e41606548', '中华门', ST_GeomFromText('POINT(118.7842873 32.0127349)'), DEFAULT, 118.7842873, 32.0127349);
INSERT INTO `tb_location` VALUES ('65fa74d2fe0d4a38b76147dffa05b3f7', '阅江楼', ST_GeomFromText('POINT(118.7426856 32.0937971)'), DEFAULT, 118.7426856, 32.0937971);
INSERT INTO `tb_location` VALUES ('c642357784af4ba5ab1b3b5ad83f95c3', '南京总统府', ST_GeomFromText('POINT(118.7947451 32.0442655)'), DEFAULT, 118.7947451, 32.0442655);
INSERT INTO `tb_location` VALUES ('f585e2188c7245209748badb61733ef6', '南京长江大桥', ST_GeomFromText('POINT(118.7438451 32.1121645)'), DEFAULT, 118.7438451, 32.1121645);
INSERT INTO `tb_location` VALUES ('fc6af35a1dcd41d29b747d57f6ba808a', '红山森林动物园', ST_GeomFromText('POINT(118.8031595 32.0919398)'), DEFAULT, 118.8031595, 32.0919398);

SET FOREIGN_KEY_CHECKS = 1;
