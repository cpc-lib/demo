#!/bin/bash
# 主机列表（存储变量值而非变量名）
ip1="192.168.220.201"
HOST_LIST=("$ip1")  # 使用变量值，并用空格分隔

# 声明一个关联数组
declare -A host_port

host_port["$ip1"]=22

DEFAULT_PORT=22

#远程服务其上面的java路径
REMOTE_JAVA_EXECUTE_PATH=/www/java/jdk17/bin/java

#时间
DATE=$(date +%Y%m%d%H%M)

BASE_PATH=/ruoyi/test

# 服务名称。同时约定部署服务的 jar 包名字也为它。
SERVER_NAME=ruoyi-admin.jar

#目标target路径
TARGET_PATH=$WORKSPACE/ruoyi-admin/target

# java源码中的application-dev.yml配置
PROFILES_ACTIVE=druid

#JVM参数
JVM_OPTS="-Xms1024m -Xmx2048m"


function stop(){
    #用于接收参数host
     local host=$1
     local port=$2
     pid=$(ssh -p $port root@$host ps -ef | grep $BASE_PATH/$SERVER_NAME | grep -v "grep" | awk '{print $2}')
     if [ -n "${pid}" ]; then
        # 正常关闭
        echo "[stop] ruoyi-admin 运行中，开始 kill [${pid}]"
        ssh -p $port root@$host "kill -15 ${pid}"
        # 等待最大 120 秒，直到关闭完成。
        for ((i = 0; i < 120; i++))
            do
                ssh -p $port root@$host sleep 1
                pid=$(ssh -p $port root@$host ps -ef | grep $BASE_PATH/$SERVER_NAME | grep -v "grep" | awk '{print $2}')
                if [ -n "$pid" ]; then
                    echo -e ".\c"
                else
                    echo "[stop] 停止 $BASE_PATH/$SERVER_NAME 成功"
                    break
                fi
		    done

        # 如果正常关闭失败，那么进行强制 kill -9 进行关闭
        if [ -n "$pid" ]; then
            echo "[stop] $BASE_PATH/$SERVER_NAME 失败，强制 kill -9 ${pid}"
            ssh -p $port root@$host kill -9 $pid
        fi
    # 如果 Java 服务未启动，则无需关闭
    else
        echo "[stop] $BASE_PATH/$SERVER_NAME 未启动，无需停止"
    fi
    echo ${pid}
}


for host in "${HOST_LIST[@]}"; do
    # 获取该主机对应的端口（默认为22如果未指定）
    port=${host_port[$host]:-$DEFAULT_PORT}
    # 在目标主机找到目标程序的进程号`
    stop $host $port
    #备份文件
    ssh -p $port root@$host "mv $BASE_PATH/$SERVER_NAME $BASE_PATH/$SERVER_NAME.$DATE"
    #新建目录
    ssh -p $port root@$host "mkdir -p $BASE_PATH"
    rsync -avz -e "ssh -p ${port}" --progress "${TARGET_PATH}/${SERVER_NAME}" "root@${host}:${BASE_PATH}/${SERVER_NAME}"
    ssh -p ${port} root@${host} "nohup $REMOTE_JAVA_EXECUTE_PATH -jar ${BASE_PATH}/${SERVER_NAME} ${JVM_OPTS} --spring.profiles.active=${PROFILES_ACTIVE} > ${BASE_PATH}/startup.log 2>&1 &"
    echo "[start] 启动 $BASE_PATH/$SERVER_NAME.jar 完成"
done
