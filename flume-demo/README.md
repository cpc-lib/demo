### 测试interceptor

在实际的开发中，一台服务器产生的日志类型可能有很多种，不同类型的日志可能需要
发送到不同的分析系统。此时会用到 Flume 拓扑结构中的 Multiplexing 结构，Multiplexing
的原理是，根据 event 中 Header 的某个 key 的值，将不同的 event 发送到不同的 Channel中，
所以我们需要自定义一个 Interceptor，为不同类型的 event 的 Header 中的 key 赋予 不同的值。
在该案例中，我们以端口数据模拟日志，以是否包含”atguigu”模拟不同类型的日志，
我们需要自定义 interceptor 区分数据中是否包含”atguigu”，将其分别发往不同的分析
系统（Channel）。


测试效果为端口数据不同,进入到不同的flume中流转

在hadoop102,hadoop103,hadoop104 创建/opt/module/flume/job/group文件夹

hadoop102 的/opt/module/flume/job/group下创建flume1.conf
```
# Name the components on this agent
a1.sources = r1
a1.sinks = k1 k2
a1.channels = c1 c2
# Describe/configure the source
a1.sources.r1.type = netcat
a1.sources.r1.bind = localhost
a1.sources.r1.port = 44444
a1.sources.r1.interceptors = i1
a1.sources.r1.interceptors.i1.type = \
cc.ivera.interceptor.CustomInterceptor$Builder
a1.sources.r1.selector.type = multiplexing
a1.sources.r1.selector.header = type
a1.sources.r1.selector.mapping.first = c1
a1.sources.r1.selector.mapping.second = c2
# Describe the sink
a1.sinks.k1.type = avro
a1.sinks.k1.hostname = hadoop103
a1.sinks.k1.port = 4141
a1.sinks.k2.type=avro
a1.sinks.k2.hostname = hadoop104
a1.sinks.k2.port = 4242
# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
# Use a channel which buffers events in memory
a1.channels.c2.type = memory
a1.channels.c2.capacity = 1000
a1.channels.c2.transactionCapacity = 100
# Bind the source and sink to the channel
a1.sources.r1.channels = c1 c2
a1.sinks.k1.channel = c1
a1.sinks.k2.channel = c2
```
hadoop103 的/opt/module/flume/job/group下创建flume4.conf
```
a1.sources = r1
a1.sinks = k1
a1.channels = c1
a1.sources.r1.type = avro
a1.sources.r1.bind = hadoop103
a1.sources.r1.port = 4141
a1.sinks.k1.type = logger
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
a1.sinks.k1.channel = c1
a1.sources.r1.channels = c1
```
hadoop104 的/opt/module/flume/job/group下创建flume3.conf
```
a1.sources = r1
a1.sinks = k1
a1.channels = c1
a1.sources.r1.type = avro
a1.sources.r1.bind = hadoop104
a1.sources.r1.port = 4242
a1.sinks.k1.type = logger
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
a1.sinks.k1.channel = c1
a1.sources.r1.channels = c1
```
                        ┌────────────────────────────┐
                        │        hadoop102            │
                        │      (Flume Agent)          │
                        │                             │
                        │  Source: r1 (netcat 44444)  │
                        │        │                    │
                        │        ▼                    │
                        │  Interceptor (Custom)       │
                        │        │                    │
                        │        ▼                    │
                        │  Channel Selector           │
                        │  (multiplexing by header)   │
                        │        │                    │
                        │   ┌────┴────┐               │
                        │   │         │               │
                        │   ▼         ▼               │
                        │ Channel c1  Channel c2      │
                        │ (memory)    (memory)        │
                        │   │         │               │
                        │   ▼         ▼               │
                        │ Sink k1     Sink k2          │
                        │ (avro)      (avro)           │
                        │   │         │               │
                        └───│─────────│───────────────┘
                            │         │
               Avro RPC 4141 │         │ Avro RPC 4242
                            │         │
        ┌───────────────────▼─┐     ┌─▼───────────────────┐
        │      hadoop103       │     │      hadoop104       │
        │   (Flume Agent)      │     │   (Flume Agent)      │
        │                      │     │                      │
        │ Source r1 (avro)     │     │ Source r1 (avro)     │
        │ port = 4141          │     │ port = 4242          │
        │        │             │     │        │             │
        │        ▼             │     │        ▼             │
        │ Channel c1 (memory)  │     │ Channel c1 (memory)  │
        │        │             │     │        │             │
        │        ▼             │     │        ▼             │
        │ Sink k1 (logger)     │     │ Sink k1 (logger)     │
        └──────────────────────┘     └──────────────────────┘





首先启动hadoop104,hadoop103,然后启动hadoop102


hadoop102
bin/flume-ng agent --conf conf/ --name  a1 --conf-file job/group/flume1.conf - Dflume.root.logger=INFO,console

hadoop103
bin/flume-ng agent --conf conf/ --name  a1 --conf-file job/group/flume4.conf - Dflume.root.logger=INFO,console

hadoop104
bin/flume-ng agent --conf conf/ --name  a1 --conf-file job/group/flume3.conf - Dflume.root.logger=INFO,console


### 测试source
使用 flume 接收数据，并给每条数据添加前缀，输出到控制台。前缀可从 flume 配置文
件中配置。

测试效果为source信息自动添加了信息

在hadoop102 创建/opt/module/job/source/predefined-source-flume.conf
```
# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1
# Describe/configure the source
a1.sources.r1.type = cc.ivera.MySource
a1.sources.r1.delay = 1000
#a1.sources.r1.field = atguigu
# Describe the sink
a1.sinks.k1.type = logger
# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```
启动hadoop102

bin/flume-ng agent --conf conf/ --name  a1 --conf-file job/source/predefined-source-flume.conf - Dflume.root.logger=INFO,console


### 测试sink
使用 flume 接收数据，并在 Sink 端给每条数据添加前缀和后缀，输出到控制台。前后
缀可在 flume 任务配置文件中配置。

在hadoop102 创建/opt/module/job/sink/predefined-sink-flume.conf
```
# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1
# Describe/configure the source
a1.sources.r1.type = netcat
a1.sources.r1.bind = localhost
a1.sources.r1.port = 44444
# Describe the sink
a1.sinks.k1.type = cc.ivera.MySink
#a1.sinks.k1.prefix = atguigu:
a1.sinks.k1.suffix = :atguigu
# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100
# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

测试效果为输入test 自动添加了前缀hello:,后缀加了:atguigu


