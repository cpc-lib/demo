### flume集成kafka

#### 1.解压flume

生产环境中，可以设置flume的堆内存为4G或以上。
修改/opt/module/flume/conf/flume-env.sh文件，配置如下参数（虚拟机环境暂不配置）
```
# 修改JVM配置
export JAVA_OPTS="-Xms4096m -Xmx4096m -Dcom.sun.management.jmxremote"
```


#### 2.配置job,编辑文件file_to_kafka.conf
```
# 定义组件
a1.sources = r1
a1.channels = c1

# 配置source
a1.sources.r1.type = TAILDIR
a1.sources.r1.filegroups = f1
# 日志（数据）文件
a1.sources.r1.filegroups.f1 = /opt/module/flume/data/test.log
a1.sources.r1.positionFile = /opt/module/flume/taildir_position.json

# 配置channel
# 采用Kafka Channel，省去了Sink，提高了效率
a1.channels.c1.type = org.apache.flume.channel.kafka.KafkaChannel
a1.channels.c1.kafka.bootstrap.servers = hadoop102:9092,hadoop103:9092,hadoop104:9092
a1.channels.c1.kafka.topic = test
a1.channels.c1.parseAsFlumeEvent = false

# 组装 
a1.sources.r1.channels = c1
```


#### 3.启动job
```
bin/flume-ng agent -n a1 -c conf/ -f job/file_to_kafka.conf
```
