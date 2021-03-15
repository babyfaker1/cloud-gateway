#!/bin/bash
#author:mlzhang@easipass.com
JAVA_HOME=/u02/jdk1.8.0_121
JAVA_OPTIONS_INITIAL=-Xms2048M
JAVA_OPTIONS_MAX=-Xmx2048M
JAVA_OPTIONS_PERMSIZE=-XX:PermSize=256m
JAVA_OPTIONS_MAX_PERMSIZE=-XX:MaxPermSize=512m
_JAR_KEYWORDS=nacosgateway.jar
APP_NAME=nacosgateway
PID=$(ps aux | grep ${_JAR_KEYWORDS} | grep -v grep | awk '{print $2}' )

check_if_process_is_running(){
 if [ "$PID" = "" ]; then
   return 1
 fi
 ps -p $PID | grep "java"
 return $?
}


case "$1" in
  status)
    if check_if_process_is_running
    then
      echo -e "\033[32m $APP_NAME is running \033[0m"
    else
      echo -e "\033[32m $APP_NAME not running \033[0m"
    fi
    ;;
  stop)
    if ! check_if_process_is_running
    then
      echo  -e "\033[32m $APP_NAME  already stopped \033[0m"
      exit 0
    fi
    kill -9 $PID
    echo -e "\033[32m Waiting for process to stop \033[0m"
    NOT_KILLED=1
    for i in {1..20}; do
      if check_if_process_is_running
      then
        echo -ne "\033[32m . \033[0m"
        sleep 1
      else
        NOT_KILLED=0
      fi
    done
    echo
    if [ $NOT_KILLED = 1 ]
    then
      echo -e "\033[32m Cannot kill process \033[0m"
      exit 1
    fi
    echo  -e "\033[32m $APP_NAME already stopped \033[0m"
    ;;
  start)
    if [ "$PID" != "" ] && check_if_process_is_running
    then
      echo -e "\033[32m $APP_NAME already running \033[0m"
      exit 1
    fi
   nohup $JAVA_HOME/bin/java -Dcsp.sentinel.dashboard.server=192.168.130.121:8080 -Dcsp.sentinel.api.port=9003 -jar -server $JAVA_OPTIONS_INITIAL $JAVA_OPTIONS_MAX $JAVA_OPTIONS_PERMSIZE $JAVA_OPTIONS_MAX_PERMSIZE $_JAR_KEYWORDS > output 2>&1 &
   echo -ne "\033[32m Starting \033[0m"
    for i in {1..20}; do
        echo -ne "\033[32m.\033[0m"
        sleep 1
    done
    if check_if_process_is_running
     then
       echo  -e "\033[32m $APP_NAME fail \033[0m"
    else
       echo  -e "\033[32m $APP_NAME started \033[0m"
    fi
    ;;
  restart)
    $0 stop
    if [ $? = 1 ]
    then
      exit 1
    fi
    $0 start
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    exit 1
esac

exit 0
