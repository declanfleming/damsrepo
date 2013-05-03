#!/bin/sh

# bulk delete records from solr
BASE=`dirname $0`
source $BASE/common.sh

curl -u $USER:$PASS -X DELETE -F id=bb01010101 http://localhost:8080/dams/api/index
if [ $? != 0 ]; then
    exit 1
fi
echo
