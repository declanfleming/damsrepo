#!/bin/sh

# update a metadata record
BASE=`dirname $0`
. $BASE/common.sh

OBJID=bb01010101
JSON='[{"subject":"bb01010101","predicate":"dams:note","object":"node1"},{"subject":"node1","predicate":"dams:type","object":"abstract"},{"subject":"node1","predicate":"rdf:value","object":"test"}]'

curl -u $USER:$PASS -X PUT -F adds=$JSON $URL/api/objects/$OBJID
if [ $? != 0 ]; then
    exit 1
fi
echo
