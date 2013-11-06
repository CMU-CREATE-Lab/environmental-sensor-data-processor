package org.cmucreatelab.visualization.speck;

import java.io.File;
import org.cmucreatelab.io.CsvReader;
import org.cmucreatelab.visualization.GeolocatedDevices;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class SpeckDevices extends GeolocatedDevices
   {
   public SpeckDevices(@NotNull final File devicesFile)
      {
      final CsvReader csvReader = new CsvReader(devicesFile, true, 4);
      csvReader.addEventListener(
            new CsvReader.EventListenerAdapter()
            {
            @Override
            public void handleLine(@NotNull final String line, @NotNull final String[] values)
               {
               final String name = values[0];
               final String prettyName = values[1];
               final String row = values[2];
               final String col = values[3];
               if (name != null && row != null && col != null)
                  {
                  addDevice(new DeviceImpl(name, prettyName, row, col));
                  }
               }
            });
      csvReader.read();
      }
   }
