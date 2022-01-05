//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.heraldprox.herald.sensor.DefaultSensorDelegate;

/**
 * Default sensor delegate with convenient functions for writing data to log file.
 */
public class SensorDelegateLogger extends DefaultSensorDelegate implements Resettable {
    protected final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.UK);
    @Nullable
    protected final Context context;
    @Nullable
    private final TextFile textFile;

    public SensorDelegateLogger() {
        context = null;
        textFile = null;
    }

    public SensorDelegateLogger(@NonNull final Context context, @NonNull final String filename) {
        this.context = context;
        this.textFile = new TextFile(context, filename);
        this.dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public synchronized void reset() {
        if (null == textFile) {
            return;
        }
        textFile.reset();
    }

    /**
     * Get current time as formatted timestamp "yyyy-MM-dd HH:mm:ss"
     * @return Formatted timestamp for current time.
     */
    @NonNull
    protected String timestamp() {
        return TextFile.csv(dateFormatter.format(new Date()));
    }

    /**
     * Wrap value as CSV format value.
     * @param value Any text value.
     * @return Wrapped value.
     */
    @NonNull
    protected String csv(@NonNull final String value) {
        return TextFile.csv(value);
    }

    /**
     * Write line. This function will add newline character to end of line.
     * @param line Line to write.
     */
    protected void write(@NonNull final String line) {
        if (null == textFile) {
            return;
        }
        textFile.write(line);
    }

    /**
     * Overwrite file content.
     * @param content Content to write.
     */
    protected void overwrite(@NonNull final String content) {
        if (null == textFile) {
            return;
        }
        textFile.overwrite(content);
    }

    /**
     * Test if the file is empty.
     * @return True if empty, false otherwise.
     */
    protected boolean empty() {
        if (null == textFile) {
            return false;
        }
        return textFile.empty();
    }

    /**
     * Return contents of file.
     * @return File content or "" for empty or non-existent file.
     */
    @NonNull
    protected String contentsOf() {
        if (null == textFile) {
            return "";
        }
        return textFile.contentsOf();
    }
}
