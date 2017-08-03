#!/bin/bash
################################################################################
## Copyright:   HZGOSUN Tech. Co, BigData
## Filename:    start-consumer.sh
## Description: to start consumer
## Version:     1.0
## Author:      zhaozhe
## Created:     2017-08-03
################################################################################
set -x  ## 用于调试用，不用的时候可以注释掉

#---------------------------------------------------------------------#
#                              定义变量                                #
#---------------------------------------------------------------------#
BIN_DIR=$(cd $(dirname $0); pwd)    ## bin所在目录
DEPLOY_DIR=$(cd ..;pwd)           ## 项目根目录
CONF_DIR=${DEPLOY_DIR}/conf      ## 配置文件目录
SERVER_NAME=`sed '/ftpserver.application.name/!d;s/.*=//' conf/dubbo.properties | tr -d '\r'` #获取服务名称
SERVER_PORT=`sed '/listener-port/!d;s/.*=//' conf/dubbo.properties | tr -d '\r'` #获取服务端口号

if [ -z "$SERVER_NAME" ]; then
    SERVER_NAME=`hoatname`
fi

PIDS=`ps -f | grep java | grep "$CONF_DIR" |awk `{print $2}`
if [ -n "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

if [ -n "$SERVER_PORT" ]; then
    SERVER_PORT_COUNT=`netstat -tln | grep $SERVER_PORT | wc -l`
    if [ $SERVER_PORT_COUNT -gt 0 ]; then
        echo "ERROR: The $SERVER_NAME port $SERVER_PORT already used!"
        exit 1
    fi
fi

LIB_DIR=${DEPLOY_DIR}/lib        ## Jar 包目录
LIB_JARS=`ls $LIB_DIR | grep .jar | awk '{print "'$LIB_DIR'/"$0}' | tr "\n" ":"`;  ## jar 包位置以及第三方依赖jar包，绝对路径
LOG_DIR=${DEPLOY_DIR}/logs                       ## log 日记目录
LOG_FILE=${LOG_DIR}/start-ftpserver.log        ##  log 日记文件

#####################################################################
# 函数名: start_consumer
# 描述: 把consumer 消费组启动起来
# 参数: N/A
# 返回值: N/A
# 其他: N/A
#####################################################################
function start_consumer()
{
    if [ ! -d $LOG_DIR ]; then
        mkdir $LOG_DIR;
    fi
    nohup java -classpath $CONF_DIR:$LIB_JARS com.hzgc.ftpserver.kafka.ftp.KafkaOverFtpServer >> ${LOG_FILE} 2>&1 &
}

#####################################################################
# 函数名: main
# 描述: 脚本主要业务入口
# 参数: N/A
# 返回值: N/A
# 其他: N/A
#####################################################################
function main()
{
    start_consumer
}

## 脚本主要业务入口
main