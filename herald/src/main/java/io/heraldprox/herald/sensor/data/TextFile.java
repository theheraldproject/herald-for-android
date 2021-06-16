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
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TextFile {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
    @NonNull
    private final File file;

    public TextFile(@NonNull final Context context, @NonNull final String filename) {
        final File folder = new File(getRootFolder(context), "Sensor");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                logger.fault("Make folder failed (folder={})", folder);
            }
        }
        file = new File(folder, filename);
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        }, 30, 30, TimeUnit.SECONDS);
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

    /// Get contents of file
    @NonNull
    public synchronized String contentsOf() {
        try {
            final FileInputStream fileInputStream = new FileInputStream(file);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            final StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            bufferedReader.close();
            fileInputStream.close();
            return stringBuilder.toString();
        } catch (Throwable e) {
            logger.fault("read failed (file={})", file, e);
            return "";
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

    /// Append line to new or existing file
    public synchronized void write(@NonNull final String line) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write((line + "\n").getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("write failed (file={})", file, e);
        }
    }

    /// Overwrite file content
    public synchronized void overwrite(@NonNull final String content) {
        try {
            // Write to temporary file first
            final File temporaryFile = new File(file.getParentFile(), file.getName() + ".tmp");
            final FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
            // Rename to actual file on completion
            if (!temporaryFile.renameTo(file)) {
                logger.fault("overwrite failed (file={},reason=renameFailed)", file);
            }
        } catch (Throwable e) {
            logger.fault("overwrite failed (file={})", file, e);
        }
    }

    /// Quote value for CSV output if required.
    @NonNull
    public static String csv(@NonNull final String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("'") || value.contains("’")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}
