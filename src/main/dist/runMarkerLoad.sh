#!/usr/bin/env bash
#
# run the marker load module
#
. /etc/profile
APPNAME=marker-load-pipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAILLIST=llamers@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAILLIST="llamers@mcw.edu mtutaj@mcw.edu jrsmith@mcw.edu akwitek@mcw.edu motutaj@mcw.edu"
fi

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

$APPDIR/_run.sh -markerLoad "$@"

mailx -s "[$SERVER] Marker Load Pipeline Run" $EMAILLIST < $APPDIR/logs/summary.log
mailx -s "[$SERVER] Marker Load Pipeline Old Marker Expected Sizes" $EMAILLIST < $APPDIR/logs/previousAssemblyData.log
