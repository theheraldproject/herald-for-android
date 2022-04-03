//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TextFile implements Resettable {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    @NonNull
    private final File file;
    private final ConcurrentLinkedQueue<String> writeBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger writeBufferSize = new AtomicInteger();

    /**
     * Line consumer for forEachLine().
     */
    public interface TextFileLineConsumer {
        void apply(@NonNull final String line);
    }

    /**
     * Text file with 30 second write delay on write() calls.
     * @param file File within a folder.
     */
    public TextFile(@NonNull final File file) {
        this(file, 30);
    }

    /**
     * Text file with configurable write delay on write() calls.
     * @param file File within a folder.
     * @param writeDelay Time delay in seconds between flushing buffered data to storage.
     */
    public TextFile(@NonNull final File file, final int writeDelay) {
        this.file = file;
        final File folder = file.getParentFile();
        if (null == folder) {
            logger.fault("File must exist in a folder");
        }
        if (null != folder && !folder.exists() && !folder.mkdirs()) {
            logger.fault("Make folder failed (folder={})", folder);
        }
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                flush();
            }
        }, writeDelay, writeDelay, TimeUnit.SECONDS);
    }

    /**
     * Text file with 30 second write delay on write() calls.
     * @param context Application context.
     * @param filename File name of file to be stored within "Sensor" folder in application context.
     */
    public TextFile(@NonNull final Context context, @NonNull final String filename) {
        this(context, filename, 30);
    }

    /**
     * Text file with configurable write delay on write() calls.
     * @param context Application context.
     * @param filename File name of file to be stored within "Sensor" folder in application context.
     * @param writeDelay Time delay in seconds between flushing buffered data to storage
     */
    public TextFile(@NonNull final Context context, @NonNull final String filename, final int writeDelay) {
        this(new File(new File(getRootFolder(context), "Sensor"), filename));
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!flush()) {
                    return;
                }
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        }, writeDelay, writeDelay, TimeUnit.SECONDS);
    }

    // MARK: - Resettable

    @Override
    public synchronized void reset() {
        clearBuffer();
        overwrite("");
    }

    /**
     * Discard pending writes
     */
    protected synchronized void clearBuffer() {
        writeBuffer.clear();
    }

    // MARK: - I/O functions

    /**
     * Remove all text files in context.
     * @param context Application context
     * @return True if successful, false otherwise.
     */
    public final static boolean removeAll(@NonNull final Context context) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
        final File folder = new File(getRootFolder(context), "Sensor");
        if (!folder.exists()) {
            return true;
        }
        boolean success = true;
        for (final File file : folder.listFiles()) {
            if (file.delete()) {
                logger.debug("Remove file successful (folder={},file={})", folder, file.getName());
            } else {
                logger.fault("Remove file failed (folder={},file={})", folder, file.getName());
                success = false;
            }
        }
        return success;
    }

    /**
     * List all text files in context.
     * @param context
     * @return List of all text files in context.
     */
    public final static Collection<TextFile> listAll(@NonNull final Context context) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
        final File folder = new File(getRootFolder(context), "Sensor");
        final List<TextFile> textFiles = new ArrayList<>();
        if (!folder.exists()) {
            return textFiles;
        }
        for (final File file : folder.listFiles()) {
            textFiles.add(new TextFile(file));
        }
        return textFiles;
    }

    /**
     * Get input stream associated with file.
     * @param context Application context.
     * @param filename File.
     * @return Input stream, or null on failure.
     */
    @Nullable
    public final static InputStream inputStream(@NonNull final Context context, @NonNull final String filename) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
        final File folder = new File(getRootFolder(context), "Sensor");
        if (!folder.exists()) {
            logger.fault("inputStream, folder does not exist (folder={})", folder);
            return null;
        }
        final File file = new File(folder, filename);
        if (!file.exists()) {
            logger.fault("inputStream, file does not exist (file={})", file);
            return null;
        }
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            if (null == fileInputStream) {
                logger.fault("inputStream, cannot open file input stream (file={})", file);
                return null;
            }
            return fileInputStream;
        } catch (Throwable e) {
            logger.fault("inputStream, failed due to exception", e);
            return null;
        }
    }

    /**
     * Get contents of file.
     * @return File content
     */
    @NonNull
    public synchronized String contentsOf() {
        final StringBuilder stringBuilder = new StringBuilder();
        forEachLine(new TextFileLineConsumer() {
            @Override
            public void apply(@NonNull String line) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        });
        return stringBuilder.toString();
    }

    /**
     * Get contents of file as individual lines.
     * @return File content as individual lines.
     */
    @NonNull
    public synchronized List<String> lines() {
        final List<String> lines = new ArrayList<>();
        forEachLine(new TextFileLineConsumer() {
            @Override
            public void apply(@NonNull String line) {
                lines.add(line);
            }
        });
        return lines;
    }

    /**
     * Apply function to each line of the text file.
     * @param consumer
     */
    public synchronized void forEachLine(@NonNull TextFileLineConsumer consumer) {
        try {
            if (!file.exists()) {
                return;
            }
            final FileInputStream fileInputStream = new FileInputStream(file);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                consumer.apply(line);
            }
            bufferedReader.close();
            fileInputStream.close();
        } catch (Throwable e) {
            logger.fault("read failed (file={})", file, e);
        }
    }

    /**
     * Get root folder for SD card or emulated external storage.
     *
     * @param context Application context.
     * @return Root folder.
     */
    private static File getRootFolder(@NonNull final Context context) {
        // Get SD card or emulated external storage. By convention (really!?)
        // SD card is reported after emulated storage, so select the last folder
        final File[] externalMediaDirs = context.getExternalMediaDirs();
        if (externalMediaDirs.length > 0) {
            return externalMediaDirs[externalMediaDirs.length - 1];
        } else {
            return Environment.getExternalStorageDirectory();
        }
    }

    public synchronized boolean empty() {
        return !file.exists() || 0 == file.length();
    }

    /**
     * Append line to new or existing file. The line is added to the
     * write buffer and automatically flushed once every 30 seconds
     * or when the buffer contains > 256k of text.
     */
    public synchronized void write(@NonNull final String line) {
        writeBuffer.add(line);
        if (writeBufferSize.addAndGet(line.length()) > 262144) {
            flush();
        }
    }

    /**
     * Flush write buffer if it is not empty.
     * @return True if data was written, false otherwise.
     */
    public synchronized boolean flush() {
        if (writeBuffer.isEmpty()) {
            return false;
        }
        final List<String> lines = new ArrayList<>(writeBuffer);
        final StringBuilder content = new StringBuilder(writeBufferSize.get() + lines.size());
        // Newline character is added to the end by writeNow()
        for (int i=0; i<lines.size(); i++) {
            if (i > 0) {
                content.append('\n');
            }
            content.append(lines.get(i));
        }
        writeNow(content.toString());
        clearBuffer();
        return true;
    }

    /**
     * Append line to new or existing file immediately.
     * @param line Line of text
     */
    public synchronized void writeNow(@NonNull final String line) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((line + "\n").getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("write failed (file={})", file, e);
        }
    }

    /**
     * Overwrite file content.
     * @param content Text content
     */
    public synchronized void overwrite(@NonNull final String content) {
        clearBuffer();
        try {
            // No longer writing to temporary file first as this caused the test to fail on latest Android version
            final FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("overwrite failed (file={})", file, e);
        }
    }

    /**
     * Quote value for CSV output if required.
     * @param value
     * @return
     */
    @NonNull
    public static String csv(@NonNull final String value) {
        if (null == value) {
            return "NULL";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("'") || value.contains("’")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}
