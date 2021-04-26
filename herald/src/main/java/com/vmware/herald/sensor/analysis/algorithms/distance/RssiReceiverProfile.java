package com.vmware.herald.sensor.analysis.algorithms.distance;

import com.vmware.herald.sensor.DefaultSensorDelegate;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.ProximityMeasurementUnit;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Accumulate histogram of all RSSI measurements to build
/// a profile of the receiver for normalisation
public class RssiReceiverProfile extends DefaultSensorDelegate {
    private final int min, max;
    private final long[] histogram;
    private final TextFile textFile;
    private final TimeInterval autoWritePeriod;
    private boolean isComplete = false;
    private long lastWrittenAt = 0;

    /// Accumulate a RSSI profile for value range [min, max] and auto-write profile to storage at regular intervals
    public RssiReceiverProfile(final int min, final int max, final TextFile textFile, final TimeInterval autoWritePeriod) {
        this.min = min;
        this.max = max;
        this.histogram = new long[max - min + 1];
        this.textFile = textFile;
        this.autoWritePeriod = autoWritePeriod;
        if (textFile != null) {
            read(textFile);
        }
    }

    /// Accumulate a RSSI profile for value range [min, max] in-memory only
    public RssiReceiverProfile(final int min, final int max) {
        this(min, max, null, null);
    }

    @Override
    public synchronized void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        // Guard for data collection until histogram reaches maximum count
        if (isComplete) {
            return;
        }
        // Guard for RSSI measurements only
        if (didMeasure.unit != ProximityMeasurementUnit.RSSI || didMeasure.value == null) {
            return;
        }
        // Guard for RSSI range
        final int rssi = (int) Math.round(didMeasure.value);
        if (rssi < min || rssi > max) {
            return;
        }
        // Guard for reaching maximum count
        final int index = rssi - min;
        if (histogram[index] == Long.MAX_VALUE) {
            isComplete = true;
            return;
        }
        // Increment count
        histogram[index]++;
        // Auto write to file at regular intervals
        if (textFile != null && autoWritePeriod != null && lastWrittenAt + autoWritePeriod.value < System.currentTimeMillis()) {
            write(textFile);
        }
    }

    /// Read profile data from storage, this replaces existing in-memory profile
    public void read(TextFile textFile) {
        final String content = textFile.contentsOf();
        for (final String row : content.split("\n")) {
            final String[] cols = row.split(",", 2);
            if (cols.length != 2) {
                continue;
            }
            final int rssi = Integer.parseInt(cols[0]);
            final long count = Long.parseLong(cols[1]);
            if (rssi < min || rssi > max) {
                continue;
            }
            final int index = rssi - min;
            histogram[index] = count;
            if (count == Long.MAX_VALUE) {
                isComplete = true;
            }
        }
    }

    /// Render profile data as CSV (RSSI,count)
    private String toCsv() {
        final StringBuilder s = new StringBuilder();
        for (int i=0; i<histogram.length; i++) {
            final int rssi = min + i;
            final String row = rssi + "," +histogram[i] + "\n";
            s.append(row);
        }
        return s.toString();
    }

    /// Write profile data to storage
    public void write(TextFile textFile) {
        final String content = toCsv();
        textFile.overwrite(content);
    }

    // MARK: - Histogram equalisation

    /// Compute cumulative distribution function (CDF) of histogram
    private static void cdf(final long[] histogram, final long[] cdf) {
        long sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            cdf[i] = (sum += histogram[i]);
        }
    }

    /// Compute transformation table for equalising histogram to target CDF
    private static void equalisation(final long[] histogram, final long[] cdf, final int[] transform) {
        final double sum = cdf[cdf.length - 1];
        final long max = cdf.length - 1;
        if (sum > 0) {
            int j = 0;
            for (int i = 0; i < histogram.length; i++) {
                for (; j < cdf.length; j++) {
                    if (histogram[i] <= cdf[j]) {
                        transform[i] = j;
                        break;
                    }
                }
            }
        }
    }

    /// Compute transformation table for normalising histogram to maximise its dynamic range
    private static void normalisation(final long[] cdf, final int[] transform) {
        final double sum = cdf[cdf.length - 1];
        final long max = cdf.length - 1;
        if (sum > 0) {
            for (int i = cdf.length; i-- > 0; ) {
                transform[i] = (int) Math.round(max * cdf[i] / sum);
            }
        }
    }

}
