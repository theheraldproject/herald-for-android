package org.c19x.sensor.data;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import org.c19x.AppDelegate;

import java.io.File;
import java.io.FileOutputStream;

public class TextFile {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.TextFile");
    private File file;

    public TextFile(String filename) {
        final File folder = new File(getRootFolder(AppDelegate.getContext()), "C19X");
        if (!folder.exists()) {
            folder.mkdirs();
            logger.debug("Created folder (folder={})", folder);
        }
        file = new File(folder, filename);
    }

    /**
     * Get root folder for SD card or emulated external storage.
     *
     * @param context
     * @return
     */
    private final static File getRootFolder(final Context context) {
        // Get SD card or emulated external storage. By convention (really!?)
        // SD card is reported after emulated storage, so select the last folder
        final File[] externalMediaDirs = context.getExternalMediaDirs();
        if (externalMediaDirs.length > 0) {
            return externalMediaDirs[externalMediaDirs.length - 1];
        } else {
            return Environment.getExternalStorageDirectory();
        }
    }

    public boolean empty() {
        return file.exists();
    }

    /// Append line to new or existing file
    public void write(String line) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            MediaScannerConnection.scanFile(AppDelegate.getContext(), new String[]{file.getAbsolutePath()}, null, null);
            fileOutputStream.write((line + "\n").getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("write failed (file={})", file, e);
        }
    }

    /// Overwrite file content
    public void overwrite(String content) {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            MediaScannerConnection.scanFile(AppDelegate.getContext(), new String[]{file.getAbsolutePath()}, null, null);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Throwable e) {
            logger.fault("overwrite failed (file={})", file, e);
        }
    }

}
