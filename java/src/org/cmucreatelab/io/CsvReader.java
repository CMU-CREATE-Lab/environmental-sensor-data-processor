package org.cmucreatelab.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class CsvReader
   {
   private static final Logger LOG = Logger.getLogger(CsvReader.class);
   public static final String COMMA_DELIMITER = ",";

   public static interface EventListener
      {
      void handleBegin(@NotNull final File file);

      void handleHeader(@NotNull final String header, @NotNull String[] fieldNames);

      void handleLine(@NotNull final String line, @NotNull String[] values);

      void handleEnd(@NotNull final File file, final int numLines);
      }

   public static class EventListenerAdapter implements EventListener
      {
      @Override
      public void handleBegin(@NotNull final File file)
         {
         // does nothing by default
         }

      @Override
      public void handleHeader(@NotNull final String header, @NotNull final String[] fieldNames)
         {
         // does nothing by default
         }

      @Override
      public void handleLine(@NotNull final String line, @NotNull final String[] values)
         {
         // does nothing by default
         }

      @Override
      public void handleEnd(@NotNull final File file, final int numLines)
         {
         // does nothing by default
         }
      }

   @NotNull
   private final File file;
   private final boolean hasHeader;
   private final int numFields;
   @NotNull
   private final Set<EventListener> eventListeners = new HashSet<EventListener>();

   public CsvReader(@NotNull final File file, final boolean hasHeader, final int numFields)
      {
      this.file = file;
      this.hasHeader = hasHeader;
      this.numFields = numFields;
      }

   public void addEventListener(@Nullable final EventListener listener)
      {
      if (listener != null)
         {
         eventListeners.add(listener);
         }
      }

   public void read()
      {
      try
         {
         final Scanner lineScanner = new Scanner(new BufferedReader(new FileReader(file)));

         // Tell listeners about the beginning of the file
         for (final EventListener listener : eventListeners)
            {
            listener.handleBegin(file);
            }

         // Tell listeners about the header, if any
         if (hasHeader && lineScanner.hasNextLine())
            {
            final String header = lineScanner.nextLine();
            final String[] fieldNames = header.split(COMMA_DELIMITER);
            for (final EventListener listener : eventListeners)
               {
               listener.handleHeader(header, fieldNames);
               }
            }

         int numLines = 0;
         while (lineScanner.hasNextLine())
            {
            final String line = lineScanner.nextLine().trim();

            // ignore empty lines
            if (line.length() > 0)
               {
               final String[] fields = line.split(COMMA_DELIMITER);

               // ignore lines with too few fields
               if (fields.length >= numFields)
                  {
                  for (final EventListener listener : eventListeners)
                     {
                     listener.handleLine(line, fields);
                     }
                  numLines++;
                  }
               }
            }

         for (final EventListener listener : eventListeners)
            {
            listener.handleEnd(file, numLines);
            }
         }
      catch (FileNotFoundException e)
         {
         LOG.error("FileNotFoundException while trying to read file [" + file + "].  Read aborted", e);
         }
      }
   }
