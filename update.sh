#!/bin/bash

# This script updates a deployed web app with updated source code. This script
# is not meant for deploying the web app for the first time.
# The lib directory at the path $PATH_PREDEPLOY/WEB-INF/lib/ must contain all
# external dependencies besides the ones already installed in the system.
# Run as root user.

# Exit if script is not running as root user.
if [ "$EUID" -ne 0 ]; then
  echo "Script must be run as root user."
  exit
fi

# Configure this script.
PATH_PROJ_ROOT="/home/dev/Documents/chat"
PATH_SRC="${PATH_PROJ_ROOT}/src"
PATH_PREDEPLOY="${PATH_PROJ_ROOT}/deploy"
PATH_DEPLOY="/var/lib/tomcat10/webapps/ROOT"
PATH_JAVA_LIB="/usr/share/java/tomcat10"
OWNER_NAME="tomcat10"
GROUP_NAME="tomcat10"

# Compile Java source files.
rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
javac -Xlint:unchecked -cp $PATH_JAVA_LIB/*:$PATH_PREDEPLOY/WEB-INF/lib/* -d $PATH_PREDEPLOY/WEB-INF/classes/ $PATH_SRC/chat/util/*.java $PATH_SRC/chat/state/*.java $PATH_SRC/chat/state/structs/*.java $PATH_SRC/chat/app/*.java $PATH_SRC/chat/app/structs/*.java $PATH_SRC/chat/server/*.java
if [[ $? -ne 0 ]] ; then
  rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
  exit 1
fi

# Deploy compiled bytecode.
rm -rf $PATH_DEPLOY/WEB-INF/classes/*
mv $PATH_PREDEPLOY/WEB-INF/classes/* $PATH_DEPLOY/WEB-INF/classes/

# Deploy external dependencies.
rm -rf $PATH_DEPLOY/WEB-INF/lib/*
cp $PATH_PREDEPLOY/WEB-INF/lib/* $PATH_DEPLOY/WEB-INF/lib/

# Deploy server configuration.
rm -f $PATH_DEPLOY/WEB-INF/web.xml
cp $PATH_PREDEPLOY/WEB-INF/web.xml $PATH_DEPLOY/WEB-INF/

# Deploy static resources.
rm -rf $PATH_DEPLOY/public/*
cp -r $PATH_PREDEPLOY/public/* $PATH_DEPLOY/public/

# Change ownership of all deployed resources.
chown -R $OWNER_NAME:$GROUP_NAME $PATH_DEPLOY/*