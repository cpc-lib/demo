FROM registry.cn-hangzhou.aliyuncs.com/snow-io/eclipse-temurin:11-jre

ARG HBASE_VERSION=2.5.13
ARG HBASE_TGZ=hbase-${HBASE_VERSION}-bin.tar.gz
#ARG HBASE_URL=https://downloads.apache.org/hbase/${HBASE_VERSION}/${HBASE_TGZ}

COPY ${HBASE_TGZ} /tmp/${HBASE_TGZ}


ENV HBASE_HOME=/opt/hbase \
    HBASE_CONF_DIR=/opt/hbase/conf \
    PATH=/opt/hbase/bin:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates bash netcat-openbsd procps \
    && rm -rf /var/lib/apt/lists/*


#RUN mkdir -p /opt && \
#    tar -xzf /tmp/${HBASE_TGZ} -C /opt && \
#    ln -s /opt/hbase-${HBASE_VERSION} ${HBASE_HOME} && \
#    rm -f /tmp/${HBASE_TGZ}

RUN mkdir -p /opt && \
    #curl -fSL ${HBASE_URL} -o /tmp/${HBASE_TGZ} && \
    tar -xzf /tmp/${HBASE_TGZ} -C /opt && \
    ln -s /opt/hbase-${HBASE_VERSION} ${HBASE_HOME} && \
    rm -f /tmp/${HBASE_TGZ}

# 默认配置（可被 compose 覆盖）
COPY conf/hbase-site.xml ${HBASE_CONF_DIR}/hbase-site.xml
COPY conf/hbase-env.sh  ${HBASE_CONF_DIR}/hbase-env.sh

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && \
    mkdir -p /data/hbase && chmod -R 777 /data && \
    mkdir -p /wal && chmod -R 777 /wal

EXPOSE 16010 16000 16020 16030
ENTRYPOINT ["/entrypoint.sh"]
