package org.cmucreatelab.visualization;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public abstract class GeolocatedDevices
   {
   private final SortedMap<String, Device> devices = new TreeMap<String, Device>();

   public static interface Device
      {
      String getName();

      String getLatitude();

      String getLongitude();
      }

   protected final void addDevice(@Nullable final Device device)
      {
      if (device != null)
         {
         devices.put(device.getName(), device);
         }
      }

   public final boolean isEmpty()
      {
      return devices.isEmpty();
      }

   public final int size()
      {
      return devices.size();
      }

   @NotNull
   public final Iterator<Device> iterator()
      {
      return devices.values().iterator();
      }

   @Nullable
   public final Device findByName(@Nullable final String name)
      {
      return devices.get(name);
      }

   protected static final class DeviceImpl implements Device
      {
      @NotNull
      private final String name;
      @NotNull
      private final String latitude;
      @NotNull
      private final String longitude;

      public DeviceImpl(@NotNull final String name, @NotNull final String latitude, @NotNull final String longitude)
         {
         this.name = name;
         this.latitude = latitude;
         this.longitude = longitude;
         }

      @NotNull
      public String getName()
         {
         return name;
         }

      @NotNull
      public String getLatitude()
         {
         return latitude;
         }

      @NotNull
      public String getLongitude()
         {
         return longitude;
         }

      @Override
      public boolean equals(final Object o)
         {
         if (this == o)
            {
            return true;
            }
         if (o == null || getClass() != o.getClass())
            {
            return false;
            }

         final DeviceImpl that = (DeviceImpl)o;

         if (!name.equals(that.name))
            {
            return false;
            }
         if (!latitude.equals(that.latitude))
            {
            return false;
            }
         if (!longitude.equals(that.longitude))
            {
            return false;
            }

         return true;
         }

      @Override
      public int hashCode()
         {
         int result = name.hashCode();
         result = 31 * result + latitude.hashCode();
         result = 31 * result + longitude.hashCode();
         return result;
         }
      }
   }
