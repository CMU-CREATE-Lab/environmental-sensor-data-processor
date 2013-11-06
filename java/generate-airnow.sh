#!/bin/bash

java -cp dist/environmental-sensor-data-processor.jar org.cmucreatelab.visualization.airnow.AirNowDataProcessor ./airnow_devices.csv ../csv ../data;
