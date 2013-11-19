package org.cmucreatelab.visualization.speck;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.cmucreatelab.io.CsvReader;
import org.cmucreatelab.visualization.GeolocatedDevices;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class SpeckDataProcessor
   {
   private static final Logger LOG = Logger.getLogger(SpeckDataProcessor.class);

   private static final String CSV_FILE_EXTENSION = ".csv";

   public static void main(final String[] args) throws IOException
      {
      if (args.length < 6)
         {
         System.err.println("Usage: <sample interval seconds> <devices file> <input directory> <output directory> <output metadata filename> <output binary filename>");
         System.exit(1);
         }
      final int sampleIntervalSecs = Integer.parseInt(args[0]);
      final File devicesFile = new File(args[1]);
      final File inputDirectory = new File(args[2]);
      final File outputDirectory = new File(args[3]);

      //noinspection ResultOfMethodCallIgnored
      outputDirectory.mkdirs();

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

      final File outputMetadataFile = new File(outputDirectory, args[4]);
      final File outputBinaryFile = new File(outputDirectory, args[5]);

      System.out.println("Sample Interval Seconds:  " + sampleIntervalSecs);
      System.out.println("Speck Devices File:       " + devicesFile.getCanonicalPath());
      System.out.println("CSV Input Directory:      " + inputDirectory.getCanonicalPath());
      System.out.println("Output Metadata File:     " + outputMetadataFile.getCanonicalPath());
      System.out.println("Output Binary File:       " + outputBinaryFile.getCanonicalPath());

      final SpeckDataProcessor dataProcessor = new SpeckDataProcessor(sampleIntervalSecs, devicesFile, inputDirectory, outputMetadataFile, outputBinaryFile);
      dataProcessor.run();
      }

   private final int sampleIntervalSecs;

   @NotNull
   private final File devicesFile;

   @NotNull
   private final File inputDirectory;

   @NotNull
   private final File outputMetadataFile;

   @NotNull
   private final File outputBinaryFile;

   SpeckDataProcessor(final int sampleIntervalSecs,
                      @NotNull final File devicesFile,
                      @NotNull final File inputDirectory,
                      @NotNull final File outputMetadataFile,
                      @NotNull final File outputBinaryFile)
      {
      this.sampleIntervalSecs = sampleIntervalSecs;
      this.devicesFile = devicesFile;
      this.inputDirectory = inputDirectory;
      this.outputMetadataFile = outputMetadataFile;
      this.outputBinaryFile = outputBinaryFile;
      }

   public final void run() throws IOException
      {
      final GeolocatedDevices geolocatedDevices = new SpeckDevices(devicesFile);
      if (!geolocatedDevices.isEmpty())
         {
         final SpeckCsvDataFileEventListener csvDataFileEventListener = new SpeckCsvDataFileEventListener();
         final MetadataGenerator metadataGenerator = new MetadataGenerator(sampleIntervalSecs, outputMetadataFile, geolocatedDevices);
         final BinaryGenerator binaryGenerator = new BinaryGenerator(outputBinaryFile);

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

   private static final class MetadataGenerator implements SpeckCsvDataFileEventListener.EventListener
      {
      private final int sampleIntervalSecs;

      @NotNull
      private final File outputFile;

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

      private MetadataGenerator(final int sampleIntervalSecs, @NotNull final File outputFile, @NotNull final GeolocatedDevices geolocatedDevices)
         {
         this.sampleIntervalSecs = sampleIntervalSecs;
         this.outputFile = outputFile;
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
            sb.append("\"latitude\":").append(device.getLatitude()).append(",");
            sb.append("\"longitude\":").append(device.getLongitude()).append(",");
            final String locationDetails = device.getLocationDetails();
            sb.append("\"locationDetails\":").append("\"").append(locationDetails == null ? "" : locationDetails).append("\",");
            sb.append("\"minTime\":").append(minTime).append(",");
            sb.append("\"maxTime\":").append(maxTime).append(",");
            sb.append("\"minValue\":").append(minValue).append(",");
            sb.append("\"maxValue\":").append(maxValue).append(",");
            sb.append("\"minValueTime\":").append(minValueTime).append(",");
            sb.append("\"maxValueTime\":").append(maxValueTime).append(",");
            sb.append("\"valueInterval\":").append(sampleIntervalSecs).append(",");
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
            final PrintWriter writer = new PrintWriter(new FileWriter(outputFile));
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

      public BinaryGenerator(@NotNull final File outputFile) throws IOException
         {
         outputStream = new DataOutputStream(new FileOutputStream(outputFile));
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
