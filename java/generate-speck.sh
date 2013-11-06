#!/bin/bash

java -cp dist/environmental-sensor-data-processor.jar org.cmucreatelab.visualization.speck.SpeckDataProcessor ./speck_devices_12x12_test.csv ../csv/speck ../data;
