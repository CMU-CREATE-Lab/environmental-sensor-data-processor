#!/bin/bash

#===============================================================================
INPUT_DIR=./csv/gps_speck
OUTPUT_DIR=./data
#===============================================================================

OUTPUT_FILE=${OUTPUT_DIR}/gps_speck_data.js

# make sure the output directory exists
mkdir -p ${OUTPUT_DIR}

echo "// GPS Speck data" | cat > ${OUTPUT_FILE}
echo "var DATA = {};" | cat >> ${OUTPUT_FILE}
echo "" | cat >> ${OUTPUT_FILE}

# Found this at http://stackoverflow.com/a/2108296/703200
for csv_file in ${INPUT_DIR}/*
do
   csv_file=${csv_file%*/}
   deviceName=${csv_file##*/}
   speck_id=$(echo ${deviceName} | awk -F. '{print $1}')
   echo "Processing" ${speck_id}"..."${csv_file}

   echo 'DATA["'${speck_id}'"] = [' | cat >> ${OUTPUT_FILE}
   # Begin by using tail to skip the first line (the header), and then use awk
   # to check whether the GPS value is valid.  If so, print out select fields.
   tail -n +2 ${csv_file} | awk -F, '{if ($5 == "true") print "["$1","$3","$6","$7",""\""$8"\"],";}' >> ${OUTPUT_FILE}
   echo '[]];' >> ${OUTPUT_FILE}
   echo '' >> ${OUTPUT_FILE}
done