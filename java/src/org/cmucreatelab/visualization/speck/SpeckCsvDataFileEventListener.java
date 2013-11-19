package org.cmucreatelab.visualization.speck;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.cmucreatelab.io.CsvReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class SpeckCsvDataFileEventListener implements CsvReader.EventListener
   {
   private static final Logger LOG = Logger.getLogger(SpeckCsvDataFileEventListener.class);

   static interface EventListener
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
