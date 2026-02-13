修改my.cnf
server-id=1
log-bin=mysql-bin
binlog_format=row
binlog-do-db=test
binlog-do-db=test_route
binlog-do-db=cdc_test

重启mysql服务
systemctl restart mysqld



建表语句
CREATE TABLE `user_info` (
`id` varchar(32) NOT NULL COMMENT '主键ID',
`name` varchar(32) DEFAULT NULL,
`sex` varchar(32) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

insert into user_info values ("1002","test2","male");
update user_info set name="test" where id="1002"
delete from  user_info where id="1002"


bin/flink run -c cc.ivera.FlinkCDC atguigu-flink-cdc-1.0-SNAPSHOT.jar