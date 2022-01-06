package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.heraldprox.herald.sensor.datatype.CalibrationMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Histogram;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.datatype.Tuple;

public class RssiLog extends SensorDelegateLogger {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "RssiLog");
    private final List<PointMeasurement> pointMeasurements = new ArrayList<>();

    /**
     * RSSI measurement taken at a point in time for a specific target.
     */
    protected final static class PointMeasurement {
        @NonNull
        public final Date timestamp;
        @NonNull
        public final TargetIdentifier target;
        @NonNull
        public final Double rssi;
        @Nullable
        public final Double txPower;

        public PointMeasurement(@NonNull final Date timestamp, @NonNull final TargetIdentifier target, @NonNull final Double rssi, @Nullable final Double txPower) {
            this.timestamp = timestamp;
            this.target = target;
            this.rssi = rssi;
            this.txPower = txPower;
        }

        public PointMeasurement(@NonNull final TargetIdentifier target, @NonNull final Proximity proximity) {
            this(new Date(), target, proximity.value, null != proximity.calibration && proximity.calibration.unit == CalibrationMeasurementUnit.BLETransmitPower && null != proximity.calibration.value ? proximity.calibration.value : null);
        }
    }

    public RssiLog(@NonNull Context context, @NonNull String filename) {
        super(context, filename);
    }

    @Override
    protected String header() {
        return "time,id,rssi,txpower";
    }

    @Override
    public synchronized void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        // Guard for RSSI measurements only
        //noinspection ConstantConditions
        if (didMeasure.unit != ProximityMeasurementUnit.RSSI || null == didMeasure.value || null == fromTarget.value) {
            return;
        }
        append(new PointMeasurement(fromTarget, didMeasure));
    }

    // MARK: - Accumulate measurements

    protected synchronized void append(@NonNull final PointMeasurement pointMeasurement) {
        // Add to memory
        pointMeasurements.add(pointMeasurement);
        // Add to CSV log file
        final String timestamp = timestamp(pointMeasurement.timestamp);
        final String target = pointMeasurement.target.toString();
        final String rssi = pointMeasurement.rssi.toString();
        final String txPower = (null != pointMeasurement.txPower ? pointMeasurement.txPower.toString() : "");
        final String line = writeCsv(timestamp, target, rssi, txPower);
        // Add to logger
        logger.debug("Append (sizeInMemory={},line={})", pointMeasurements.size(), line);
    }

    // MARK: - Process measurements

    /**
     * Quantise point measurements to fixed time windows, where the maximum RSSI and TxPower value for each
     * target is taken as the time window value. This is necessary for cleaning the raw data that can be
     * sampled at different intervals, where the multiple readings can be taken in a short time window.
     * Quantisation provides the basis for estimating RSSI-time units, i.e. x seconds at y proximity.
     * @param pointMeasurements Point measurements for quantisation, assumes sorted in time order.
     * @param duration Time unit for quantisation, e.g. 10 second windows.
     * @return Quantised point measurements.
     */
    @NonNull
    protected final static List<PointMeasurement> quantise(@NonNull final List<PointMeasurement> pointMeasurements, @NonNull final TimeInterval duration) {
        final List<PointMeasurement> quantisedPointMeasurements = new ArrayList<>(pointMeasurements.size());
        // Quantise point measurements by time duration and target identifier to compact the data
        long currentTimeWindow = -1;
        final Map<TargetIdentifier, Tuple<Double, Double>> quantisedData = new HashMap<>();
        // Assumes point measurements are sorted by time
        for (final PointMeasurement pointMeasurement : pointMeasurements) {
            // Get quantised time window of point measurement, i.e. time rounded by duration
            final long timeWindow = (pointMeasurement.timestamp.secondsSinceUnixEpoch() / duration.value) * duration.value;
            // Flush quantised data if this is a new time window
            // Output is sorted by time then target identifier
            if (currentTimeWindow != timeWindow) {
                final Date timestamp = new Date(currentTimeWindow);
                final List<TargetIdentifier> keys = new ArrayList<>(quantisedData.keySet());
                Collections.sort(keys);
                for (final TargetIdentifier key : keys) {
                    final Tuple<Double, Double> value = quantisedData.get(key);
                    if (null == value.a) {
                        continue;
                    }
                    final PointMeasurement quantisedPointMeasurement = new PointMeasurement(timestamp, key, value.a, value.b);
                    quantisedPointMeasurements.add(quantisedPointMeasurement);
                }
                quantisedData.clear();
            }
            currentTimeWindow = timeWindow;
            // Accumulate data for time window by establishing max RSSI and TxPower for each target
            // in each time window as the quantised value. This is necessary as multiple readings
            // may be taken for one target in a short period of time.
            Tuple<Double, Double> quantisedDataForTarget = quantisedData.get(pointMeasurement.target);
            if (quantisedDataForTarget == null) {
                quantisedData.put(pointMeasurement.target, new Tuple<>(pointMeasurement.rssi, pointMeasurement.txPower));
            } else {
                // Update quantised RSSI
                if (null != pointMeasurement.rssi && (null == quantisedDataForTarget.a || pointMeasurement.rssi > quantisedDataForTarget.a)) {
                    quantisedDataForTarget = new Tuple<>(pointMeasurement.rssi, quantisedDataForTarget.b);
                    quantisedData.put(pointMeasurement.target, quantisedDataForTarget);
                }
                // Update quantised TxPower
                if (null != pointMeasurement.txPower && (null == quantisedDataForTarget.b || pointMeasurement.txPower > quantisedDataForTarget.b)) {
                    quantisedDataForTarget = new Tuple<>(quantisedDataForTarget.a, pointMeasurement.txPower);
                    quantisedData.put(pointMeasurement.target, quantisedDataForTarget);
                }
            }
        }
        // Flush last time window
        if (!quantisedData.isEmpty()) {
            final Date timestamp = new Date(currentTimeWindow);
            final List<TargetIdentifier> keys = new ArrayList<>(quantisedData.keySet());
            Collections.sort(keys);
            for (final TargetIdentifier key : keys) {
                final Tuple<Double, Double> value = quantisedData.get(key);
                if (null == value.a) {
                    continue;
                }
                final PointMeasurement quantisedPointMeasurement = new PointMeasurement(timestamp, key, value.a, value.b);
                quantisedPointMeasurements.add(quantisedPointMeasurement);
            }
            quantisedData.clear();
        }
        return quantisedPointMeasurements;
    }

    /**
     * Select a subset of point measurements based on start (inclusive) and end (exclusive) timestamps.
     * @param pointMeasurements Point measurements for selection, assumes sorted in time order.
     * @param start Start timestamp (inclusive)
     * @param end End timestamp (exclusive)
     * @return Point measurements at or after start timestamp and before end timestamp.
     */
    @NonNull
    protected final static List<PointMeasurement> subdata(@NonNull final List<PointMeasurement> pointMeasurements, @NonNull final Date start, @NonNull final Date end) {
        final List<PointMeasurement> subdata = new ArrayList<>(pointMeasurements.size());
        for (final PointMeasurement pointMeasurement : pointMeasurements) {
            if (pointMeasurement.timestamp.compareTo(start) >= 0) {
                if (pointMeasurement.timestamp.compareTo(end) < 0) {
                    subdata.add(pointMeasurement);
                } else {
                    break;
                }
            }
        }
        return subdata;
    }

    /**
     * Filter point measurements based on min (inclusive) and max (exclusive) values.
     * @param pointMeasurements Point measurements for filtering, assumes sorted in time order.
     * @param min Minimum RSSI value (inclusive)
     * @param max Maximum RSSI value (exclusive)
     * @return Point measurements containing RSSI values >= min and < max.
     */
    @NonNull
    protected final static List<PointMeasurement> filterByRssi(@NonNull final List<PointMeasurement> pointMeasurements, @NonNull final Double min, @NonNull final Double max) {
        final List<PointMeasurement> subdata = new ArrayList<>(pointMeasurements.size());
        for (final PointMeasurement pointMeasurement : pointMeasurements) {
            if (null == pointMeasurement.rssi) {
                continue;
            }
            if (pointMeasurement.rssi >= min && pointMeasurement.rssi < max) {
                subdata.add(pointMeasurement);
            }
        }
        return subdata;
    }

    /**
     * Build histogram of RSSI values for point measurements in range [min,max].
     * @param pointMeasurements Point measurements for building histogram.
     * @param min Minimum value (inclusive).
     * @param max Maximum value (exclusive).
     * @return Histogram of integer RSSI values from the point measurements.
     */
    @NonNull
    protected final static Histogram histogramOfRssi(@NonNull final List<PointMeasurement> pointMeasurements, @NonNull final Double min, @NonNull final Double max) {
        final int intMin = (int) Math.floor(min);
        final int intMax = (int) Math.ceil(max);
        final Histogram histogram = new Histogram(intMin, intMax);
        for (final PointMeasurement pointMeasurement : filterByRssi(pointMeasurements, min, max)) {
            histogram.add((int) Math.round(pointMeasurement.rssi));
        }
        return histogram;
    }

    @NonNull
    protected final static Histogram smoothAcrossBins(@NonNull final Histogram histogram, final int window) {
        final Histogram smoothed = new Histogram(histogram.min, histogram.max);
        final int halfWindow = window / 2;
        for (int value=histogram.min; value<=histogram.max; value++) {
            final int windowStart = value - halfWindow;
            final int windowEnd = value + halfWindow;
            long sumOfWindowValue = 0;
            for (int windowValue=windowStart; windowValue<=windowEnd; windowValue++) {
                sumOfWindowValue += histogram.count(windowValue);
            }
            smoothed.add(value, sumOfWindowValue / (1 + halfWindow * 2));
        }
        return smoothed;
    }
}
