DROP TABLE IF EXISTS t_order_1;
CREATE TABLE t_order_1(
                        `order_id` BIGINT(55) NOT NULL   COMMENT '订单id' ,
                        `price` DECIMAL(24,6)    COMMENT '价格' ,
                        `user_id` VARCHAR(50)    COMMENT '用户id' ,
                        `status` VARCHAR(11)    COMMENT '状态' ,
                        PRIMARY KEY (order_id)
)  COMMENT = '订单表1';

DROP TABLE IF EXISTS t_order_2;
CREATE TABLE t_order_2(
                          `order_id` BIGINT(55) NOT NULL   COMMENT '订单id' ,
                          `price` DECIMAL(24,6)    COMMENT '价格' ,
                          `user_id` VARCHAR(50)    COMMENT '用户id' ,
                          `status` VARCHAR(11)    COMMENT '状态' ,
                          PRIMARY KEY (order_id)
)  COMMENT = '订单表2';