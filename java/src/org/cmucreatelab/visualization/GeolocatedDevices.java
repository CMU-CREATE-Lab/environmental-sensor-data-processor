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
      @NotNull
      String getName();

      @Nullable
      String getPrettyName();

      @NotNull
      String getLatitude();

      @NotNull
      String getLongitude();

      @Nullable
      String getLocationDetails();
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
      @Nullable
      private final String prettyName;
      @NotNull
      private final String latitude;
      @NotNull
      private final String longitude;
      @Nullable
      private final String locationDetails;

      public DeviceImpl(@NotNull final String name,
                        @NotNull final String latitude,
                        @NotNull final String longitude)
         {
         this(name, null, latitude, longitude);
         }

      public DeviceImpl(@NotNull final String name,
                        @Nullable final String prettyName,
                        @NotNull final String latitude,
                        @NotNull final String longitude)
         {
         this(name, prettyName, latitude, longitude, null);
         }

      public DeviceImpl(@NotNull final String name,
                        @Nullable final String prettyName,
                        @NotNull final String latitude,
                        @NotNull final String longitude,
                        @Nullable final String locationDetails)
         {
         this.name = name;
         this.prettyName = prettyName;
         this.latitude = latitude;
         this.longitude = longitude;
         this.locationDetails = locationDetails;
         }

      @NotNull
      public String getName()
         {
         return name;
         }

      @Nullable
      public String getPrettyName()
         {
         return prettyName;
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

      @Nullable
      public String getLocationDetails()
         {
         return locationDetails;
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

         final DeviceImpl device = (DeviceImpl)o;

         if (!latitude.equals(device.latitude))
            {
            return false;
            }
         if (locationDetails != null ? !locationDetails.equals(device.locationDetails) : device.locationDetails != null)
            {
            return false;
            }
         if (!longitude.equals(device.longitude))
            {
            return false;
            }
         if (!name.equals(device.name))
            {
            return false;
            }
         if (prettyName != null ? !prettyName.equals(device.prettyName) : device.prettyName != null)
            {
            return false;
            }

         return true;
         }

      @Override
      public int hashCode()
         {
         int result = name.hashCode();
         result = 31 * result + (prettyName != null ? prettyName.hashCode() : 0);
         result = 31 * result + latitude.hashCode();
         result = 31 * result + longitude.hashCode();
         result = 31 * result + (locationDetails != null ? locationDetails.hashCode() : 0);
         return result;
         }
      }
   }
