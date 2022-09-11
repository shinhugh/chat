#!/bin/bash

# This script generates the .war file for the web app.
# The lib directory at the path $PATH_PREDEPLOY/WEB-INF/lib/ must contain all
# external dependencies besides the ones already installed in the system.

# Configure this script.
PATH_PROJ_ROOT="/home/dev/Documents/chat"
PATH_SRC="${PATH_PROJ_ROOT}/src"
PATH_PREDEPLOY="${PATH_PROJ_ROOT}/deploy"
PATH_JAVA_LIB="/usr/share/java/tomcat10"
WAR_NAME="ROOT"

# Compile Java source files.
rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
javac -Xlint:unchecked -cp $PATH_JAVA_LIB/*:$PATH_PREDEPLOY/WEB-INF/lib/* -d $PATH_PREDEPLOY/WEB-INF/classes/ $PATH_SRC/chat/util/*.java $PATH_SRC/chat/state/*.java $PATH_SRC/chat/state/structs/*.java $PATH_SRC/chat/app/*.java $PATH_SRC/chat/app/structs/*.java $PATH_SRC/chat/server/*.java
if [[ $? -ne 0 ]] ; then
  rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*
  exit 1
fi

# Create archive
rm -f $PATH_PROJ_ROOT/$WAR_NAME.war
jar -cvf $PATH_PROJ_ROOT/$WAR_NAME.war $PATH_PREDEPLOY/*

# Clean up
rm -rf $PATH_PREDEPLOY/WEB-INF/classes/*