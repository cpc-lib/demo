1.启动nameserver
>start mqnamesrv.cmd

2.启动broker
>start mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable = true -c ../conf/broker.conf

3.修改runbroker.cmd jvm参数
>-Xms2g -Xmx2g > -Xms256m -Xmx256m -Xmn512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m