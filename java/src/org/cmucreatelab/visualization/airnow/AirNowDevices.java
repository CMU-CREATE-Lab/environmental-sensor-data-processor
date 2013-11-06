package org.cmucreatelab.visualization.airnow;

import java.io.File;
import org.cmucreatelab.io.CsvReader;
import org.cmucreatelab.visualization.GeolocatedDevices;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class AirNowDevices extends GeolocatedDevices
   {
   public AirNowDevices(@NotNull final File airNowDevicesFile)
      {
      final CsvReader csvReader = new CsvReader(airNowDevicesFile, true, 6);
      csvReader.addEventListener(
            new CsvReader.EventListenerAdapter()
            {
            @Override
            public void handleLine(@NotNull final String line, @NotNull final String[] values)
               {
               final String name = values[4];
               final String latitude = values[0];
               final String longitude = values[1];
               if (name != null && latitude != null && longitude != null)
                  {
                  addDevice(new DeviceImpl(name, latitude, longitude));
                  }
               }
            });
      csvReader.read();
      }
   }
