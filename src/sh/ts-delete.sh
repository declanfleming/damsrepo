#!/bin/sh

# delete an existing object

OBJID=$1
if [ "$2" ]; then
	TS="?ts=$2"
fi

BASE=`dirname $0`
source $BASE/common.sh

curl -k -u $USER:$PASS -i -X DELETE http://localhost:8080/dams/api/objects/$OBJID$TS
if [ $? != 0 ]; then
    exit 1
fi
echo
