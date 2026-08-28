FROM registry.cn-hangzhou.aliyuncs.com/snow-io/eclipse-temurin:11-jre

ARG ZK_VERSION=3.9.2
ARG ZK_TGZ=apache-zookeeper-${ZK_VERSION}-bin.tar.gz
#ARG ZK_URL=https://downloads.apache.org/zookeeper/zookeeper-${ZK_VERSION}/${ZK_TGZ}

COPY ${ZK_TGZ} /tmp/${ZK_TGZ}

ENV ZK_HOME=/opt/zookeeper \
    PATH=/opt/zookeeper/bin:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates bash netcat-openbsd procps \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt && \
    #curl -fSL ${ZK_URL} -o /tmp/${ZK_TGZ} && \
    tar -xzf /tmp/${ZK_TGZ} -C /opt && \
    ln -s /opt/apache-zookeeper-${ZK_VERSION}-bin ${ZK_HOME} && \
    rm -f /tmp/${ZK_TGZ}

# 默认配置（可被 compose 覆盖）
COPY conf/zoo.cfg ${ZK_HOME}/conf/zoo.cfg
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && \
    mkdir -p /data /datalog && chmod -R 755 /data /datalog

EXPOSE 2181 2888 3888
ENTRYPOINT ["/entrypoint.sh"]
