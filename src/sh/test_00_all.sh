#!/bin/sh

DIR=`dirname $0`
ERRORS=0
DIVIDER="\n----------------------------------------------------------------\n"

$DIR/test_01_init.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_02_bootstrap.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_03_repositories.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_04_collections.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_05_objects.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_06_files.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_07_system.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_08_updates.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_09_solr.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER

$DIR/test_10_deletes.sh
if [ $? != 0 ]; then
	ERRORS=$(( $ERRORS + $? ))
fi
echo $DIVIDER



echo TOTAL ERRORS: $ERRORS
exit $ERRORS