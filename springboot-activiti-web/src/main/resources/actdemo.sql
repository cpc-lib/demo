/*
SQLyog Ultimate v12.09 (64 bit)
MySQL - 5.7.30 : Database - actdemo
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`actdemo` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `actdemo`;

/*Table structure for table `tb_evection` */

DROP TABLE IF EXISTS `tb_evection`;

CREATE TABLE `tb_evection` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `user_id` bigint(32) DEFAULT NULL COMMENT '用户id',
  `evection_name` varchar(32) DEFAULT NULL COMMENT '出差申请单名称',
  `num` double(24,6) DEFAULT NULL COMMENT '出差天数',
  `begin_date` datetime DEFAULT NULL COMMENT '预计开始时间',
  `end_date` datetime DEFAULT NULL COMMENT '预计结束时间',
  `destination` varchar(90) DEFAULT NULL COMMENT '目的地',
  `reason` varchar(90) DEFAULT NULL COMMENT '出差事由',
  `state` varchar(11) DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='tb_evection';

/*Data for the table `tb_evection` */

insert  into `tb_evection`(`id`,`user_id`,`evection_name`,`num`,`begin_date`,`end_date`,`destination`,`reason`,`state`) values (6,1,'国庆回家',8.000000,NULL,NULL,'湖南衡阳',NULL,'1');

/*Table structure for table `tb_flow_info` */

DROP TABLE IF EXISTS `tb_flow_info`;

CREATE TABLE `tb_flow_info` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flow_name` varchar(32) DEFAULT NULL COMMENT '流程名称',
  `flow_key` varchar(32) DEFAULT NULL COMMENT '流程key',
  `file_path` varchar(50) DEFAULT NULL COMMENT '流程文件位置',
  `state` int(11) DEFAULT NULL COMMENT '状态',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='tb_flow_info';

/*Data for the table `tb_flow_info` */

insert  into `tb_flow_info`(`id`,`flow_name`,`flow_key`,`file_path`,`state`,`create_time`) values (1,'出差流程','evection','bpmn/evection.bpmn',0,'2023-06-22 06:50:11');

/*Table structure for table `tb_site_message` */

DROP TABLE IF EXISTS `tb_site_message`;

CREATE TABLE `tb_site_message` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` varchar(32) DEFAULT NULL COMMENT '用户id',
  `type` varchar(11) DEFAULT NULL COMMENT '消息类型',
  `content` varchar(255) DEFAULT NULL COMMENT '消息内容',
  `is_read` varchar(11) DEFAULT NULL COMMENT '是否已读',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='tb_site_message';

/*Data for the table `tb_site_message` */

insert  into `tb_site_message`(`id`,`user_id`,`type`,`content`,`is_read`,`create_time`,`update_time`,`task_id`) values (1,'2','1',NULL,'1',NULL,NULL,'44dce67c-1282-11ee-9cd2-cc4740054736'),(2,'2','1',NULL,'1',NULL,NULL,'7c448e35-1282-11ee-9cd2-cc4740054736'),(3,'2','1',NULL,'1',NULL,NULL,'d7cf010e-1282-11ee-9cd2-cc4740054736');

/*Table structure for table `tb_user` */

DROP TABLE IF EXISTS `tb_user`;

CREATE TABLE `tb_user` (
  `id` bigint(32) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(32) DEFAULT NULL COMMENT '用户名称',
  `password` varchar(70) DEFAULT NULL COMMENT '用户密码',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `gender` int(11) DEFAULT NULL COMMENT '性别',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='tb_user';

/*Data for the table `tb_user` */

insert  into `tb_user`(`id`,`username`,`password`,`email`,`gender`,`age`) values (1,'zhangsan','$2a$10$j9Y8d4vgdktmaxU7jCCIFu04hBZ6LEwm4z.PQHIfmHhCxQPy5CAjW','260743950223@qq.com',1,23),(2,'lisi','$2a$10$CI1O69xrjm5wkV8mjWMLfeYwokPT6BK3QLipHyVOr5vAAJMIQYYpm','260743950224@qq.com',1,23),(3,'wangwu','$2a$10$GxCHU/DOKePD1xYTcPNope2dvLRg8pIYK/rtACkijBFaBwh5DtHdS','260743950226@qq.com',1,23),(4,'zhaoliu','$2a$10$hubq6vGXYR15haxGUNXvwueaSv78g8JTYXvxpbpxDZA29eOhkYEpi','260743950233@qq.com',1,23);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
