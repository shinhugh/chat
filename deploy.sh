#!/bin/bash

# Run as root user

if [ "$EUID" -ne 0 ]; then
  echo "Script must be run as root user."
  exit
fi

PATH_PROJ_ROOT="/home/dev/Documents/chat"
PATH_SRC="${PATH_PROJ_ROOT}/src"
PATH_PREDEPLOY="${PATH_PROJ_ROOT}/deploy"
PATH_DEPLOY="/var/lib/tomcat10/webapps/ROOT"
PATH_JAVA_LIB="/usr/share/java/tomcat10"
OWNER_NAME="tomcat10"
GROUP_NAME="tomcat10"

rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
sudo -u dev javac -Xlint:unchecked -cp $PATH_JAVA_LIB/servlet-api.jar:$PATH_JAVA_LIB/websocket-api.jar:$PATH_PREDEPLOY/WEB-INF/lib/mariadb-java-client-3.0.5.jar:$PATH_PREDEPLOY/WEB-INF/lib/gson-2.9.1.jar -d $PATH_PREDEPLOY/WEB-INF/classes/ $PATH_SRC/chat/util/*.java $PATH_SRC/chat/state/*.java $PATH_SRC/chat/state/structs/*.java $PATH_SRC/chat/app/*.java $PATH_SRC/chat/app/structs/*.java $PATH_SRC/chat/server/*.java
if [[ $? -ne 0 ]] ; then
  rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
  exit 1
fi

rm -rf $PATH_DEPLOY/WEB-INF/classes/*
mv $PATH_PREDEPLOY/WEB-INF/classes/* $PATH_DEPLOY/WEB-INF/classes/

rm -f $PATH_DEPLOY/WEB-INF/web.xml
cp $PATH_PREDEPLOY/WEB-INF/web.xml $PATH_DEPLOY/WEB-INF/

rm -rf $PATH_DEPLOY/public/*
cp -r $PATH_PREDEPLOY/public/* $PATH_DEPLOY/public/

chown -R $OWNER_NAME:$GROUP_NAME $PATH_DEPLOY/*