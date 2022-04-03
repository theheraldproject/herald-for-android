package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.data.Timestamp;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Histogram;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class TemporalHistogramModel {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "TemporalHistogramModel");
    // Combine measurements for the same target within each quantisation period for smoothing
    private final TimeInterval quantisationPeriod;
    // Combine quantised measurements for all targets within each histogram period for comparison
    private final TimeInterval histogramPeriod;
    // Minimum and maximum histogram bin values
    public final int minValue;
    public final int maxValue;
    // Buffer for live data (target,measurements) pending quantisation
    private final Map<String,List<Integer>> quantisationBuffer = new HashMap<>();
    @Nullable
    private Date quantisationPeriodStart = null;
    @Nullable
    private Date quantisationPeriodEnd = null;
    // Time series of histograms for comparison
    private final List<HistogramForPeriod> histogramForPeriods = new ArrayList<>();
    // Current histogram for accumulation
    @Nullable
    private HistogramForPeriod currentHistogram = null;
    // Optional text file for storing and restoring model data
    @Nullable
    private TextFile logFile = null;

    protected final static class HistogramForPeriod {
        @NonNull
        public final Date start;
        @NonNull
        public final Date end;
        @NonNull
        public final Histogram histogram;

        public HistogramForPeriod(@NonNull final Date start, @NonNull final Date end, final int min, final int max) {
            this.start = start;
            this.end = end;
            this.histogram = new Histogram(min, max);
        }

        @NonNull
        @Override
        public String toString() {
            return "HistogramForPeriod{" +
                    "start=" + start +
                    ", end=" + end +
                    ", histogram=" + histogram +
                    '}';
        }
    }

    public TemporalHistogramModel(@NonNull final TimeInterval quantisationPeriod, @NonNull final TimeInterval histogramPeriod, final int minValue, final int maxValue) {
        this(quantisationPeriod, histogramPeriod, minValue, maxValue, null);
    }

    public TemporalHistogramModel(@NonNull final TimeInterval quantisationPeriod, @NonNull final TimeInterval histogramPeriod, final int minValue, final int maxValue, @Nullable TextFile logFile) {
        this.quantisationPeriod = quantisationPeriod;
        this.histogramPeriod = histogramPeriod;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.logFile = logFile;
        read();
    }

    // MARK: - Read write log file

    private synchronized void writeHeader() {
        if (null == logFile) {
            return;
        }
        if (!logFile.empty()) {
            return;
        }
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start,end");
        for (int value=minValue; value<=maxValue; value++) {
            stringBuilder.append(',');
            stringBuilder.append(value);
        }
        logFile.writeNow(stringBuilder.toString());
    }

    private synchronized void write(@NonNull final Date start, @NonNull final Date end, @NonNull final Histogram histogram) {
        if (null == logFile) {
            return;
        }
        writeHeader();
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Timestamp.timestamp(start));
        stringBuilder.append(',');
        stringBuilder.append(Timestamp.timestamp(end));
        for (int value=minValue; value<=maxValue; value++) {
            stringBuilder.append(',');
            stringBuilder.append(histogram.count(value));
        }
        logFile.writeNow(stringBuilder.toString());
    }

    private synchronized void read() {
        if (null == logFile) {
            return;
        }
        logFile.forEachLine(new TextFile.TextFileLineConsumer() {
            private int[] values = null;
            @Override
            public void apply(@NonNull String line) {
                // Parse header
                if (line.startsWith("start,end,")) {
                    try {
                        final String[] row = line.substring("start,end,".length()).split(",");
                        values = new int[row.length];
                        for (int i=0; i<row.length; i++) {
                            values[i] = Integer.parseInt(row[i]);
                        }
                    } catch (Throwable e) {
                        logger.fault("Read failed to parse header (line={})", line);
                    }
                    return;
                }
                // Parse data after header
                if (null == values) {
                    return;
                }
                try {
                    final String[] row = line.split(",");
                    if (row.length != (values.length + 2)) {
                        logger.fault("Read failed to parse row due to length mismatch (expected={},actual={},line={})", values.length + 2, row.length, line);
                        return;
                    }
                    final Date start = Timestamp.timestamp(row[0]);
                    final Date end = Timestamp.timestamp(row[1]);
                    if (null == start || null == end) {
                        throw new Exception("Failed to parse start and end timestamps");
                    }
                    final HistogramForPeriod histogramForPeriod = new HistogramForPeriod(start, end, minValue, maxValue);
                    for (int i=0, j=2; i<values.length && j<row.length; i++, j++) {
                        // Parse errors shall reject the whole row
                        histogramForPeriod.histogram.add(values[i], Integer.parseInt(row[j]));
                    }
                    // Duplicates may occur as data maybe flushed onDestroy(), use the most recent version
                    currentHistogram = histogramForPeriod;
                    if (!histogramForPeriods.isEmpty() && histogramForPeriods.get(histogramForPeriods.size() - 1).start.equals(currentHistogram.start)) {
                        logger.debug("Read updating duplicate row (old={},new={})", histogramForPeriods.get(histogramForPeriods.size() - 1), currentHistogram);
                        histogramForPeriods.remove(histogramForPeriods.size() - 1);
                    }
                    histogramForPeriods.add(currentHistogram);
                } catch (Throwable e) {
                    logger.fault("Read failed to parse row due to content (line={})", line);
                }
            }
        });
    }

    public synchronized void flush() {
        if (null == logFile) {
            return;
        }
        flushQuantisationBuffer();
        if (null == currentHistogram) {
            return;
        }
        write(currentHistogram.start, currentHistogram.end, currentHistogram.histogram);
    }

    // MARK: - Add sample values

    public synchronized void add(@NonNull final Date timestamp, @NonNull final String identifier, final int value) {
        // Flush buffer on period end
        if (null == quantisationPeriodEnd || timestamp.afterOrEqual(quantisationPeriodEnd)) {
            flushQuantisationBuffer();
            quantisationPeriodStart = periodStart(timestamp, quantisationPeriod);
            quantisationPeriodEnd = periodEnd(timestamp, quantisationPeriod);
        }
        // Accumulate data
        List<Integer> values = quantisationBuffer.get(identifier);
        if (null == values) {
            values = new ArrayList<>(1);
            quantisationBuffer.put(identifier, values);
        }
        values.add(value);
    }

    /**
     * Get histogram for period. This combines all histogram fragments where
     * start time >= start timestamp and start time < end timestamp.
     * @param start Start timestamp (inclusive).
     * @param end End timestamp (exclusive).
     * @return Histogram covering requested period.
     */
    @NonNull
    public Histogram histogram(@NonNull final Date start, @NonNull final Date end) {
        final Histogram histogram = new Histogram(minValue, maxValue);
        for (final HistogramForPeriod histogramForPeriod : histogramForPeriods) {
            if (histogramForPeriod.start.afterOrEqual(start) && histogramForPeriod.start.before(end)) {
                histogram.add(histogramForPeriod.histogram);
            }
        }
        return histogram;
    }

    /**
     * Flush quantisation buffer.
     */
    private synchronized void flushQuantisationBuffer() {
        if (null == quantisationPeriodStart) {
            return;
        }
        final List<String> identifiers = new ArrayList<>(quantisationBuffer.keySet());
        Collections.sort(identifiers);
        for (final String identifier : identifiers) {
            final List<Integer> buffer = quantisationBuffer.get(identifier);
            if (null == buffer) {
                continue;
            }
            final Integer value = reduce(buffer);
            if (null == value) {
                continue;
            }
            addToHistogram(quantisationPeriodStart, value);
        }
        quantisationBuffer.clear();
    }

    /**
     * Quantisation function for values associated with an identifier within a quantisation period.
     * @param values Values associated with an identifier within a quantisation period.
     * @return Quantised value.
     */
    @Nullable
    private Integer reduce(@NonNull final List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        int max = values.get(0);
        for (final Integer value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Add quantised value to histogram accumulator.
     * @param timestamp Quantised timestamp for value.
     * @param value Quantised value.
     */
    private void addToHistogram(@NonNull final Date timestamp, final int value) {
        //logger.debug("addToHistogram(timestamp={},value={})", timestamp, value);
        if (null == currentHistogram || timestamp.afterOrEqual(currentHistogram.end)) {
            // Write completed period to log file
            if (null != currentHistogram) {
                //logger.debug("addToHistogram.write(currentHistogram={})", currentHistogram);
                write(currentHistogram.start, currentHistogram.end, currentHistogram.histogram);
            }
            // Create new period
            final Date start = periodStart(timestamp, histogramPeriod);
            final Date end = periodEnd(timestamp, histogramPeriod);
            currentHistogram = new HistogramForPeriod(start, end, minValue, maxValue);
            histogramForPeriods.add(currentHistogram);
        }
        currentHistogram.histogram.add(value);
    }

    private static Date periodStart(@NonNull final Date timestamp, @NonNull final TimeInterval period) {
        return new Date((timestamp.secondsSinceUnixEpoch() / period.value) * period.value);
    }

    private static Date periodEnd(@NonNull final Date timestamp, @NonNull final TimeInterval period) {
        return new Date(((timestamp.secondsSinceUnixEpoch() / period.value) + 1) * period.value);
    }
}
