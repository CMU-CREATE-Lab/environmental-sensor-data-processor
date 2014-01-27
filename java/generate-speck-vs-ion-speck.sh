#!/bin/bash

java -cp dist/environmental-sensor-data-processor.jar org.cmucreatelab.visualization.speck.SpeckDataProcessor 1 ./speck_vs_ion_speck_devices.csv ../csv/speck_vs_ion_speck ../data speck_vs_ion_speck_metadata.json speck_vs_ion_speck_data.bin;
