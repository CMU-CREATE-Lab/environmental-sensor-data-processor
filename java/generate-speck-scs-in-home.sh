#!/bin/bash

java -cp dist/environmental-sensor-data-processor.jar org.cmucreatelab.visualization.speck.SpeckDataProcessor 1 ./speck_scs_in_home_devices.csv ../csv/speck_scs_in_home ../data speck_scs_in_home_metadata.json speck_scs_in_home_data.bin;
