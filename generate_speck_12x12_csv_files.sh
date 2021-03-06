#!/bin/bash

#===============================================================================
DATASTORE_HOME=./datastore
DATASTORE_USER_ID=43
DATASTORE_CHANNEL_NAME=particles
OUTPUT_DIR=./csv/speck_12x12
START_TIME=1383316440
END_TIME=1383320580
#===============================================================================

DATASTORE_BIN=${DATASTORE_HOME}/datastore
DATASTORE_KEY_VALUE_STORE=${DATASTORE_HOME}/db/dev.kvs

# make sure the output directory exists
mkdir -p ${OUTPUT_DIR}

echo Exporting CSVs...

# Found this at http://stackoverflow.com/a/2108296/703200
for dir in ${DATASTORE_KEY_VALUE_STORE}/${DATASTORE_USER_ID}/*/
do
   dir=${dir%*/}
   deviceName=${dir##*/}
   echo '   '${deviceName}
   ${DATASTORE_BIN}/export --start ${START_TIME} --end ${END_TIME} --csv ${DATASTORE_KEY_VALUE_STORE} ${DATASTORE_USER_ID} ${deviceName}.${DATASTORE_CHANNEL_NAME} > ${OUTPUT_DIR}/${deviceName}.csv
done

echo Done!