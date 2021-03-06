#!/bin/sh

# update a metadata record
BASE=`dirname $0`
. $BASE/common.sh

OBJID=bb52572546
DISS=$BASE/../sample/object/diss.rdf.xml

curl -u $USER:$PASS -X PUT -F file=@$DISS $URL/api/objects/$OBJID
if [ $? != 0 ]; then
    exit 1
fi
echo
