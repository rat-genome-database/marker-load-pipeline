#!/usr/bin/env bash
#
# generic runner -- passes any command-line parameters straight through to the pipeline jar
# (does not enforce any module; e.g. the caller supplies -markerLoad or -snpMigrate).
# If no valid module parameter is given, the pipeline prints usage and aborts.
#
. /etc/profile
APPNAME=marker-load-pipeline

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" > run.log 2>&1
