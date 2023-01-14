//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.TextFile;

/**
 * Accumulate histogram and optionally store data to CSV file at regular intervals.
 */
public class Histogram {
    public final int min, max;
    @NonNull
    private final long[] histogram;
    @Nullable
    private final TextFile textFile;
    @NonNull
    private final TimeInterval updatePeriod;
    @NonNull
    private Date lastUpdateTime = new Date(0);
    private long samples = 0;
    private long countMax = 0;
    @Nullable
    private Integer mode = null;
    @Nullable
    private Integer minValue = null;
    @Nullable
    private Integer maxValue = null;

    /**
     * Accumulate histogram for value range [min, max] and auto-write histogram to CSV file at regular intervals.
     * @param min
     * @param max
     * @param updatePeriod
     * @param textFile
     */
    public Histogram(final int min, final int max, @NonNull final TimeInterval updatePeriod, @Nullable final TextFile textFile) {
        this.min = min;
        this.max = max;
        this.histogram = new long[max - min + 1];
        this.textFile = textFile;
        this.updatePeriod = updatePeriod;
        clear();
        if (textFile != null) {
            read(textFile);
        }
    }

    /**
     * Accumulate histogram for value range [min, max] in-memory only
     * @param min
     * @param max
     */
    public Histogram(final int min, final int max) {
        this(min, max, TimeInterval.minute, null);
    }

    /**
     * Get total count.
     * @return
     */
    public synchronized long count() {
        return samples;
    }

    /**
     * Get count of value.
     * @param value
     * @return
     */
    public synchronized long count(final int value) {
        if (value < min || value > max) {
            return 0;
        }
        return histogram[value - min];
    }

    @Nullable
    public synchronized Integer mode() {
        return mode;
    }

    @Nullable
    public synchronized Integer minValue() {
        return minValue;
    }

    @Nullable
    public synchronized Integer maxValue() {
        return maxValue;
    }

    /**
     * Add sample. Out of range values are discarded.
     * @param value
     * @param increment
     */
    public synchronized void add(final int value, final long increment) {
        if (value < min || value > max) {
            return;
        }
        // Increment count
        final int index = value - min;
        histogram[index] += increment;
        samples += increment;
        // Update mode
        if (histogram[index] > countMax) {
            countMax = histogram[index];
            mode = value;
        }
        // Update bounds
        if (null == minValue || value < minValue) {
            minValue = value;
        }
        if (null == maxValue || value > maxValue) {
            maxValue = value;
        }
        // Write histogram to text file at regular intervals
        if (textFile != null) {
            final Date time = new Date();
            if (lastUpdateTime.secondsSinceUnixEpoch() + updatePeriod.value < time.secondsSinceUnixEpoch()) {
                write(textFile);
                lastUpdateTime = time;
            }
        }
    }

    /**
     * Add sample. Out of range values are discarded.
     * @param value
     */
    public synchronized void add(final int value) {
        add(value, 1);
    }

    /**
     * Add all values from another histogram. Out of range values are discarded.
     * @param other Other histogram for merging into this histogram.
     */
    public synchronized void add(@NonNull final Histogram other) {
        for (int value=min; value<=max; value++) {
            final long increment = other.count(value);
            if (increment > 0) {
                // Increment count
                final int index = value - min;
                histogram[index] += increment;
                samples += increment;
                // Update mode
                if (histogram[index] > countMax) {
                    countMax = histogram[index];
                    mode = value;
                }
                // Update bounds
                if (null == minValue || value < minValue) {
                    minValue = value;
                }
                if (null == maxValue || value > maxValue) {
                    maxValue = value;
                }
            }
        }
        // Write histogram to text file at regular intervals
        if (textFile != null) {
            final Date time = new Date();
            if (lastUpdateTime.secondsSinceUnixEpoch() + updatePeriod.value < time.secondsSinceUnixEpoch()) {
                write(textFile);
                lastUpdateTime = time;
            }
        }
    }

    /**
     * Clear histogram data.
     */
    public void clear() {
        for (int i=histogram.length; i-->0;) {
            histogram[i] = 0;
        }
        samples = 0;
        countMax = 0;
        mode = null;
    }

    /**
     * Read histogram data from CSV file, this replaces existing in-memory data.
     * @param textFile
     */
    public void read(@NonNull final TextFile textFile) {
        clear();
        final String content = textFile.contentsOf();
        for (final String row : content.split("\n")) {
            final String[] cols = row.split(",", 2);
            if (cols.length != 2) {
                continue;
            }
            final int value = Integer.parseInt(cols[0]);
            final long count = Long.parseLong(cols[1]);
            if (value < min || value > max) {
                continue;
            }
            final int index = value - min;
            histogram[index] = count;
            samples += count;
            if (histogram[index] > countMax) {
                countMax = histogram[index];
                mode = value;
            }
            // Update bounds
            if (null == minValue || value < minValue) {
                minValue = value;
            }
            if (null == maxValue || value > maxValue) {
                maxValue = value;
            }
        }
    }

    /**
     * Render histogram data as CSV (value,count).
     * @return
     */
    @NonNull
    private String toCsv() {
        final StringBuilder s = new StringBuilder();
        for (int i=0; i<histogram.length; i++) {
            final int value = min + i;
            final String row = value + "," +histogram[i] + "\n";
            s.append(row);
        }
        return s.toString();
    }

    /**
     * Write histogram data to CSV file.
     * @param textFile
     */
    public void write(@NonNull final TextFile textFile) {
        final String content = toCsv();
        textFile.overwrite(content);
    }

    @NonNull
    @Override
    public String toString() {
        return "Histogram{" +
                "samples=" + samples +
                ", mode=" + mode +
                '}';
    }
}
