#!/bin/bash

#===============================================================================
DATASTORE_HOME=$1
DATASTORE_USER_ID=$2
DATASTORE_CHANNEL_NAME=$3
OUTPUT_DIR=$4
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
   ${DATASTORE_BIN}/export --csv ${DATASTORE_KEY_VALUE_STORE} ${DATASTORE_USER_ID} ${deviceName}.${DATASTORE_CHANNEL_NAME} > ${OUTPUT_DIR}/${deviceName}.csv
done

echo Done!