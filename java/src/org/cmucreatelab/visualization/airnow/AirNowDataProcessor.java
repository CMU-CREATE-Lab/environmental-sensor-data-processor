package org.cmucreatelab.visualization.airnow;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.cmucreatelab.io.CsvReader;
import org.cmucreatelab.visualization.GeolocatedDevices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class AirNowDataProcessor
   {
   private static final Logger LOG = Logger.getLogger(AirNowDataProcessor.class);

   private static final String CSV_FILE_EXTENSION = ".csv";

   public static void main(final String[] args) throws IOException
      {
      if (args.length < 3)
         {
         System.err.println("ERROR: the AirNow devices file and the input and output directories must all be specified.");
         System.exit(1);
         }
      final File airNowDevicesFile = new File(args[0]);
      final File inputDirectory = new File(args[1]);
      final File outputDirectory = new File(args[2]);

      //noinspection ResultOfMethodCallIgnored
      outputDirectory.mkdirs();

      System.out.println("AirNow Devices File:   " + airNowDevicesFile.getCanonicalPath());
      System.out.println("CSV Input Directory:   " + inputDirectory.getCanonicalPath());
      System.out.println("Data Output Directory: " + outputDirectory.getCanonicalPath());
      if (!airNowDevicesFile.isFile())
         {
         System.err.println("The specified AirNow devices file is invalid.  It is either not a file, or does not exist: " + inputDirectory.getCanonicalPath());
         }

      if (!inputDirectory.isDirectory())
         {
         System.err.println("The specified input directory is invalid.  It is either not a directory, or does not exist: " + inputDirectory.getCanonicalPath());
         }

      if (!outputDirectory.isDirectory())
         {
         System.err.println("The specified output directory is invalid.  It is either not a directory, or does not exist: " + outputDirectory.getCanonicalPath());
         }

      final AirNowDataProcessor airNowDataProcessor = new AirNowDataProcessor(airNowDevicesFile, inputDirectory, outputDirectory);
      airNowDataProcessor.run();
      }

   @NotNull
   private final File airNowDevicesFile;

   @NotNull
   private final File inputDirectory;

   @NotNull
   private final File outputDirectory;

   private AirNowDataProcessor(@NotNull final File airNowDevicesFile, @NotNull final File inputDirectory, @NotNull final File outputDirectory)
      {
      this.airNowDevicesFile = airNowDevicesFile;
      this.inputDirectory = inputDirectory;
      this.outputDirectory = outputDirectory;
      }

   private void run() throws IOException
      {
      final GeolocatedDevices geolocatedDevices = new AirNowDevices(airNowDevicesFile);
      if (!geolocatedDevices.isEmpty())
         {
         final AirNowCsvDataFileEventListener airNowCsvDataFileEventListener = new AirNowCsvDataFileEventListener();
         final MetadataGenerator metadataGenerator = new MetadataGenerator(outputDirectory, geolocatedDevices);
         final BinaryGenerator binaryGenerator = new BinaryGenerator(outputDirectory);

         airNowCsvDataFileEventListener.addEventListener(metadataGenerator);
         airNowCsvDataFileEventListener.addEventListener(binaryGenerator);

         System.out.println("Processing " + geolocatedDevices.size() + " devices...");
         final Iterator<GeolocatedDevices.Device> iterator = geolocatedDevices.iterator();
         while (iterator.hasNext())
            {
            GeolocatedDevices.Device device = iterator.next();
            final File deviceFile = new File(inputDirectory, device.getName() + CSV_FILE_EXTENSION);
            if (deviceFile.isFile())
               {
               System.out.println("   " + deviceFile.getName());
               final CsvReader csvReader = new CsvReader(deviceFile, true, 2);
               csvReader.addEventListener(airNowCsvDataFileEventListener);
               csvReader.read();
               }
            }

         metadataGenerator.finish();
         binaryGenerator.finish();
         }
      }

   private static final class AirNowCsvDataFileEventListener implements CsvReader.EventListener
      {
      private static interface EventListener
         {
         void handleBegin(@NotNull String name);

         void handleLine(final int epochTimeInSeconds, final double value, final short valueAsShort);

         void handleEnd(final int numRecords);
         }

      private String name = null;
      private int numRecords = 0;

      @NotNull
      private final Set<EventListener> eventListeners = new HashSet<EventListener>();

      public void addEventListener(@Nullable final EventListener listener)
         {
         if (listener != null)
            {
            eventListeners.add(listener);
            }
         }

      @Override
      public final void handleBegin(@NotNull final File file)
         {
         final String filenameWithExtension = file.getName();
         final int dotPosition = filenameWithExtension.lastIndexOf(".");
         name = (dotPosition >= 0) ? filenameWithExtension.substring(0, dotPosition) : filenameWithExtension;
         numRecords = 0;
         for (final EventListener listener : eventListeners)
            {
            listener.handleBegin(name);
            }
         }

      @Override
      public final void handleHeader(@NotNull final String header, @NotNull final String[] fieldNames)
         {
         // nothing to do
         }

      @Override
      public final void handleLine(@NotNull final String line, @NotNull final String[] values)
         {
         try
            {
            final int epochTimeInSeconds = Integer.parseInt(values[0]);
            final double value = Double.parseDouble(values[1]);
            numRecords++;
            for (final EventListener listener : eventListeners)
               {
               // we multiple the value by 10, round it, and store as a short to save space, but provide both
               // the double version and the short version here so that the metadata can store the double version
               // and the binary can store the short version (the client will divide by 10 when processing)
               listener.handleLine(epochTimeInSeconds, value, (short)Math.round(value * 10));
               }
            }
         catch (NumberFormatException ignored)
            {
            LOG.error("NumberFormatException while parsing line [" + line + "].  Skipping.");
            }
         }

      @Override
      public final void handleEnd(@NotNull final File file, final int numLines)
         {
         for (final EventListener listener : eventListeners)
            {
            listener.handleEnd(numRecords);
            }
         }
      }

   private static final class MetadataGenerator implements AirNowCsvDataFileEventListener.EventListener
      {
      private static final int SAMPLE_INTERVAL_SECS = 3600;    // AirNow data is every hour, which is 3600 secs
      @NotNull
      private final File outputDirectory;
      @NotNull
      private final GeolocatedDevices geolocatedDevices;
      private final List<String> devicesJson;

      private String name = null;
      private int minTime = Integer.MAX_VALUE;
      private int maxTime = Integer.MIN_VALUE;
      private int minValueTime = 0;
      private int maxValueTime = 0;
      private double minValue = Double.MAX_VALUE;
      private double maxValue = Double.MIN_VALUE;
      private int recordOffset = 0;

      public MetadataGenerator(@NotNull final File outputDirectory, @NotNull final GeolocatedDevices geolocatedDevices)
         {
         this.outputDirectory = outputDirectory;
         this.geolocatedDevices = geolocatedDevices;
         devicesJson = new ArrayList<String>(geolocatedDevices.size());
         }

      @Override
      public void handleBegin(@NotNull String name)
         {
         this.name = name;
         minTime = Integer.MAX_VALUE;
         maxTime = Integer.MIN_VALUE;
         minValue = Double.MAX_VALUE;
         maxValue = Double.MIN_VALUE;
         }

      @Override
      public void handleLine(final int epochTimeInSeconds, final double value, final short valueAsShort)
         {
         minTime = Math.min(minTime, epochTimeInSeconds);
         maxTime = Math.max(maxTime, epochTimeInSeconds);
         if (Double.compare(value, minValue) <= 0)
            {
            minValue = value;
            minValueTime = epochTimeInSeconds;
            }
         if (Double.compare(value, maxValue) >= 0)
            {
            maxValue = value;
            maxValueTime = epochTimeInSeconds;
            }
         }

      @Override
      public void handleEnd(final int numRecords)
         {
         final GeolocatedDevices.Device device = geolocatedDevices.findByName(name);
         if (device != null)
            {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("\"name\":").append("\"").append(name).append("\",");
            sb.append("\"latitude\":").append(device.getLatitude()).append(",");
            sb.append("\"longitude\":").append(device.getLongitude()).append(",");
            sb.append("\"minTime\":").append(minTime).append(",");
            sb.append("\"maxTime\":").append(maxTime).append(",");
            sb.append("\"minValue\":").append(minValue).append(",");
            sb.append("\"maxValue\":").append(maxValue).append(",");
            sb.append("\"minValueTime\":").append(minValueTime).append(",");
            sb.append("\"maxValueTime\":").append(maxValueTime).append(",");
            sb.append("\"numRecords\":").append(numRecords).append(",");
            sb.append("\"recordOffset\":").append(recordOffset).append("}");
            devicesJson.add(sb.toString());

            recordOffset += numRecords;
            }
         }

      public void finish()
         {
         try
            {
            final File file = new File(outputDirectory, "airnow_metadata.json");
            final PrintWriter writer = new PrintWriter(new FileWriter(file));
            writer.println("{\"valueIntervalSecs\":" + SAMPLE_INTERVAL_SECS + ",\"devices\" : [");

            int i = 0;
            for (final String json : devicesJson)
               {
               writer.println(json + ((i == devicesJson.size() - 1) ? "" : ","));
               i++;
               }
            writer.println("]}");

            writer.close();
            }
         catch (IOException e)
            {
            LOG.error("IOException while trying to write the metadata file", e);
            }
         }
      }

   private static final class BinaryGenerator implements AirNowCsvDataFileEventListener.EventListener
      {
      @NotNull
      private final DataOutputStream outputStream;

      public BinaryGenerator(@NotNull final File outputDirectory) throws IOException
         {
         final File dataFile = new File(outputDirectory, "airnow_data.bin");
         outputStream = new DataOutputStream(new FileOutputStream(dataFile));
         }

      @Override
      public void handleBegin(@NotNull final String name)
         {
         // nothing to do
         }

      @Override
      public void handleLine(final int epochTimeInSeconds, final double value, final short valueAsShort)
         {
         try
            {
            outputStream.writeInt(epochTimeInSeconds);
            outputStream.writeShort(valueAsShort);       // we write the short version of the value to save space
            }
         catch (IOException e)
            {
            LOG.error("IOException while writing binary data", e);
            }
         }

      @Override
      public void handleEnd(final int numRecords)
         {
         // nothing to do
         }

      public void finish()
         {
         try
            {
            outputStream.close();
            }
         catch (IOException e)
            {
            LOG.error("IOException while trying to close the binary output stream", e);
            }
         }
      }
   }
