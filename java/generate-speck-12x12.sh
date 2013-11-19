#!/bin/bash

java -cp dist/environmental-sensor-data-processor.jar org.cmucreatelab.visualization.speck.SpeckDataProcessor 1 ./speck_12x12_devices.csv ../csv/speck_12x12 ../data speck_12x12_metadata.json speck_12x12_data.bin;
