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
    protected final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
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
        if (empty()) {
            writeNow(header());
        }
    }

    /**
     * Override this method to provide optional file header row.
     * @return
     */
    protected String header() {
        return "";
    }

    @Override
    public synchronized void reset() {
        if (null == textFile) {
            return;
        }
        textFile.reset();
        writeNow(header());
    }

    /**
     * Get given time as formatted timestamp "yyyy-MM-dd HH:mm:ss"
     * @return Formatted timestamp for given time.
     */
    @NonNull
    protected String timestamp(@NonNull Date date) {
        return TextFile.csv(dateFormatter.format(date));
    }

    /**
     * Get current time as formatted timestamp "yyyy-MM-dd HH:mm:ss"
     * @return Formatted timestamp for current time.
     */
    @NonNull
    protected String timestamp() {
        return timestamp(new Date());
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
     * Write line immediately. Same as write() but the data is flushed to file immediately.
     * @param line Line to write.
     */
    protected void writeNow(@NonNull final String line) {
        if (null == textFile) {
            return;
        }
        textFile.writeNow(line);
    }

    /**
     * Write list of values as CSV row. This function will wrap individual values in quotes if necessary.
     * Null values will be outputted as empty string.
     * @param values
     */
    @NonNull
    protected String writeCsv(@NonNull final String... values) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<values.length; i++) {
            if (i > 0) {
                stringBuilder.append(',');
            }
            if (null != values[i]) {
                stringBuilder.append(TextFile.csv(values[i]));
            }
        }
        final String line = stringBuilder.toString();
        write(line);
        return line;
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
