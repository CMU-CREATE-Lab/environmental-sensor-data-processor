package org.cmucreatelab.visualization.speck;

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
public class SpeckDataProcessor
   {
   private static final Logger LOG = Logger.getLogger(SpeckDataProcessor.class);

   private static final String CSV_FILE_EXTENSION = ".csv";

   public static void main(final String[] args) throws IOException
      {
      if (args.length < 3)
         {
         System.err.println("ERROR: the Speck devices file and the input and output directories must all be specified.");
         System.exit(1);
         }
      final File devicesFile = new File(args[0]);
      final File inputDirectory = new File(args[1]);
      final File outputDirectory = new File(args[2]);

      //noinspection ResultOfMethodCallIgnored
      outputDirectory.mkdirs();

      System.out.println("Speck Devices File:    " + devicesFile.getCanonicalPath());
      System.out.println("CSV Input Directory:   " + inputDirectory.getCanonicalPath());
      System.out.println("Data Output Directory: " + outputDirectory.getCanonicalPath());
      if (!devicesFile.isFile())
         {
         System.err.println("The specified Speck devices file is invalid.  It is either not a file, or does not exist: " + inputDirectory.getCanonicalPath());
         }

      if (!inputDirectory.isDirectory())
         {
         System.err.println("The specified input directory is invalid.  It is either not a directory, or does not exist: " + inputDirectory.getCanonicalPath());
         }

      if (!outputDirectory.isDirectory())
         {
         System.err.println("The specified output directory is invalid.  It is either not a directory, or does not exist: " + outputDirectory.getCanonicalPath());
         }

      final SpeckDataProcessor dataProcessor = new SpeckDataProcessor(devicesFile, inputDirectory, outputDirectory);
      dataProcessor.run();
      }

   @NotNull
   private final File devicesFile;

   @NotNull
   private final File inputDirectory;

   @NotNull
   private final File outputDirectory;

   private SpeckDataProcessor(@NotNull final File devicesFile, @NotNull final File inputDirectory, @NotNull final File outputDirectory)
      {
      this.devicesFile = devicesFile;
      this.inputDirectory = inputDirectory;
      this.outputDirectory = outputDirectory;
      }

   private void run() throws IOException
      {
      final GeolocatedDevices geolocatedDevices = new SpeckDevices(devicesFile);
      if (!geolocatedDevices.isEmpty())
         {
         final SpeckCsvDataFileEventListener csvDataFileEventListener = new SpeckCsvDataFileEventListener();
         final MetadataGenerator metadataGenerator = new MetadataGenerator(outputDirectory, geolocatedDevices);
         final BinaryGenerator binaryGenerator = new BinaryGenerator(outputDirectory);

         csvDataFileEventListener.addEventListener(metadataGenerator);
         csvDataFileEventListener.addEventListener(binaryGenerator);

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
               csvReader.addEventListener(csvDataFileEventListener);
               csvReader.read();
               }
            }

         metadataGenerator.finish();
         binaryGenerator.finish();
         }
      }

   private static final class SpeckCsvDataFileEventListener implements CsvReader.EventListener
      {
      private static interface EventListener
         {
         void handleBegin(@NotNull String name);

         void handleLine(final int epochTimeInSeconds, final int value);

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
            final int value = Integer.parseInt(values[1]);
            numRecords++;
            for (final EventListener listener : eventListeners)
               {
               listener.handleLine(epochTimeInSeconds, value);
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

   private static final class MetadataGenerator implements SpeckCsvDataFileEventListener.EventListener
      {
      private static final int SAMPLE_INTERVAL_SECS = 1;    // Speck data is every second
      @NotNull
      private final File outputDirectory;
      @NotNull
      private final GeolocatedDevices geolocatedDevices;
      @NotNull
      private final List<String> devicesJson;

      private String name = null;
      private int minTime = Integer.MAX_VALUE;
      private int maxTime = Integer.MIN_VALUE;
      private int minValueTime = 0;
      private int maxValueTime = 0;
      private int minValue = Integer.MAX_VALUE;
      private int maxValue = Integer.MIN_VALUE;
      private int recordOffset = 0;

      public MetadataGenerator(@NotNull final File outputDirectory, @NotNull final GeolocatedDevices geolocatedDevices)
         {
         this.outputDirectory = outputDirectory;
         this.geolocatedDevices = geolocatedDevices;
         devicesJson = new ArrayList<String>();
         }

      @Override
      public void handleBegin(@NotNull String name)
         {
         this.name = name;
         minTime = Integer.MAX_VALUE;
         maxTime = Integer.MIN_VALUE;
         minValue = Integer.MAX_VALUE;
         maxValue = Integer.MIN_VALUE;
         }

      @Override
      public void handleLine(final int epochTimeInSeconds, final int value)
         {
         minTime = Math.min(minTime, epochTimeInSeconds);
         maxTime = Math.max(maxTime, epochTimeInSeconds);
         if (value <= minValue)
            {
            minValue = value;
            minValueTime = epochTimeInSeconds;
            }
         if (value >= maxValue)
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
            sb.append("\"prettyName\":").append("\"").append(device.getPrettyName()).append("\",");
            sb.append("\"row\":").append(device.getLatitude()).append(",");
            sb.append("\"col\":").append(device.getLongitude()).append(",");
            sb.append("\"minTime\":").append(minTime).append(",");
            sb.append("\"maxTime\":").append(maxTime).append(",");
            sb.append("\"minValue\":").append(minValue).append(",");
            sb.append("\"maxValue\":").append(maxValue).append(",");
            sb.append("\"minValueTime\":").append(minValueTime).append(",");
            sb.append("\"maxValueTime\":").append(maxValueTime).append(",");
            sb.append("\"valueInterval\":").append(SAMPLE_INTERVAL_SECS).append(",");
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
            final File file = new File(outputDirectory, "speck_12x12_metadata.json");
            final PrintWriter writer = new PrintWriter(new FileWriter(file));
            writer.println("{\"devices\" : [");

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

   private static final class BinaryGenerator implements SpeckCsvDataFileEventListener.EventListener
      {
      @NotNull
      private final DataOutputStream outputStream;

      public BinaryGenerator(@NotNull final File outputDirectory) throws IOException
         {
         final File dataFile = new File(outputDirectory, "speck_12x12_data.bin");
         outputStream = new DataOutputStream(new FileOutputStream(dataFile));
         }

      @Override
      public void handleBegin(@NotNull final String name)
         {
         // nothing to do
         }

      @Override
      public void handleLine(final int epochTimeInSeconds, final int value)
         {
         try
            {
            outputStream.writeInt(epochTimeInSeconds);
            outputStream.writeInt(value);
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
