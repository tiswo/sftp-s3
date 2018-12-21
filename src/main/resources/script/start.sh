#!/bin/bash
ID=${2:-default}
SHELL_PATH=$(cd $(dirname $0); pwd);
SFTPD_HOME=$(cd $SHELL_PATH/..; pwd)
SFTPD_MEM_MB=${SFTPD_MEM_MB:-2048}

MAIN_JAR="${SFTPD_HOME}/lib/s3-sftp.jar"
PIDFILE="${SFTPD_HOME}/log/app.pid"

SFTP_SPRING_ARGS="--spring.config.location=${SFTPD_HOME}/conf/application.properties"

do_run () {
  echo $SFTPD_HOME
  cd ${SFTPD_HOME}
  exec java -Xms256m -Xmx${SFTPD_MEM_MB}m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=1024m -Ddbconfigpath=/apps/dbconfig/ -jar ${MAIN_JAR} $SFTP_SPRING_ARGS
}
do_start () {
  echo $SFTPD_HOME
  cd ${SFTPD_HOME}
  nohup java -Xms256m -Xmx${SFTPD_MEM_MB}m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=1024m -Ddbconfigpath=/apps/dbconfig/ \
  -jar ${MAIN_JAR} >${SFTPD_HOME}/log/run.log 2>&1 $SFTP_SPRING_ARGS &
  PID="$!"
  #echo ${PID} >$PIDFILE
  echo "SFTPD: STARTED [${PID}]"
}
do_stop () {
  local PID="$(cat $PIDFILE 2>/dev/null)"
  ps -ax | awk '{ print $1 }' | grep -e "^${PID}$"
  if [  $? -eq 0 ]; then
    echo -n "Stopping SFTPD ${ID} PID:${PID} : "
    kill -TERM $PID
    echo " OK"
  else
    echo Stopping SFTPD ${ID} : NOT FOUND
  fi
  rm -f $PIDFILE 1>/dev/null 2>&1
}
do_status () {
  local PID="$(cat $PIDFILE 2>/dev/null)"
  echo -n "Status SFTPD ${ID} : "
  ps -ax | awk '{ print $1 }' | grep -e "^${PID}$"
  if [ $? -eq 0 ]; then
    echo "RUNNING [${PID}]"
  else
    echo "NOT RUNNING"
  fi
}
case "$1" in
  run)
    do_run
  ;;
  start)
    do_stop
    do_start
  ;;
  stop)
    do_stop
  ;;
  restart)
    do_stop
    do_start
  ;;
  status)
    do_status
  ;;
  *)
    echo "$0 <run|start|stop|restart|status> [id]"
  ;;
esac