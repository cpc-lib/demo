canal.deployer-1.1.7.tar.gz 使用root账户进行启动canal
修改canal.properties
##################################################
######### 		     Kafka 		     #############
##################################################
kafka.bootstrap.servers = hadoop102:9092,hadoop103:9092,hadoop104:9092

修改instance.properties
canal.instance.master.address=hadoop102:3306
# mq config
canal.mq.topic=canal_test
canal.mq.partition=0

#设置为kafka模式,11111端口无法启动



启动kafka消费者
bin/kafka-console-consumer.sh --bootstrap-server hadoop102:9092 --topic canal_test

#插入数据到user_info中
CREATE TABLE `user_info` (
`id` varchar(32) NOT NULL COMMENT '主键ID',
`name` varchar(32) DEFAULT NULL,
`sex` varchar(32) DEFAULT NULL,
PRIMARY KEY (`id`)
)
INSERT INTO user_info VALUES('1001','zhangsan','male'),('1002','lisi','female');



