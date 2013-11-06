#!/bin/bash

#===============================================================================
DATASTORE_HOME=./datastore
DATASTORE_USER_ID=42
DATASTORE_CHANNEL_NAME=PM2_5_mass
OUTPUT_DIR=./csv
#===============================================================================

./export_csv_files_from_datastore.sh ${DATASTORE_HOME} ${DATASTORE_USER_ID} ${DATASTORE_CHANNEL_NAME} ${OUTPUT_DIR}