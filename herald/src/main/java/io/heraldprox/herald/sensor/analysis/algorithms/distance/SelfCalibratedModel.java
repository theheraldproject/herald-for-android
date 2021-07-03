//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.DoubleValue;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

/**
 * Extension of SmoothedLinearModel to include self-calibration
 * <br>- Assume minimum and average distance between people for entire population is
 *   similar over time (e.g. weeks and months).
 * <br>- Experiments have shown advertised TX power for all test phones are similar
 *   while the measured RSSI by different phones differs at the same distance.
 * <br>- Normalisation of measured RSSI value is required to bring all receivers to
 *   a common range, and then use the minimum and median value to determine the
 *   intercept and coefficient.
 * <br>- Histogram normalisation is enabled by a long term histogram of all measured
 *   RSSI values by a device.
 * <br>- Use social norm to set minimum and mean distance between people, then set
 *   time duration within minimum and mean distance to derive percentiles for
 *   self-calibration based on observed values.
 * @param <T>
 */
public class SelfCalibratedModel<T extends DoubleValue> extends SmoothedLinearModel<T> {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "SmoothedLinearSelfCalibratedModel");
    @NonNull
    private final Distance min, mean;
    private final double maxRssiPercentile, anchorRssiPercentile;
    @NonNull
    public final RssiHistogram histogram;
    @Nullable
    private final TextFile textFile;
    private double maxRssi = -10;
    @NonNull
    private Date lastSampleTime = new Date(0);

    public SelfCalibratedModel(@NonNull final Distance min, @NonNull final Distance mean, @NonNull final TimeInterval withinMin, @NonNull final TimeInterval withinMean, @Nullable final TextFile textFile) {
        super();
        this.min = min;
        this.mean = mean;
        this.maxRssiPercentile = (TimeInterval.day.value - withinMin.value) / (double) TimeInterval.day.value;
        this.anchorRssiPercentile = (TimeInterval.day.value - withinMean.value) / (double) TimeInterval.day.value;
        this.histogram = new RssiHistogram(-99, -10, TimeInterval.minutes(10), textFile);
        this.textFile = textFile;
    }

    @Override
    public void reset() {
        super.reset();
        if (null == textFile) {
            return;
        }
        textFile.reset();
    }

    public void update() {
        histogram.update();
        // Use max RSSI percentile (e.g. 95th) value for minimum distance
        maxRssi = histogram.normalisedPercentile(maxRssiPercentile);
        // Use anchor RSSI percentile (e.g. 50th, median) value as marker for average distance.
        final double anchorRssi = histogram.normalisedPercentile(anchorRssiPercentile);
        // Use value range between mean and max as estimate for coefficient
        final double rssiRange = maxRssi - anchorRssi;
        // Estimate intercept and coefficient (default derived from SmoothedLinearModel)
        final double intercept = maxRssi;
        final double coefficient = (rssiRange > 0 ? (mean.value - min.value) / rssiRange : 0.266793);
        setParameters(intercept, coefficient);
        logger.debug("update (maxRSSI={},anchorRSSI={},intercept={},coefficient={})", maxRssi, anchorRssi, intercept, coefficient);
    }

    @Override
    public void map(@NonNull final Sample<T> value) {
        super.map(value);
        if (value.taken().secondsSinceUnixEpoch() > lastSampleTime.secondsSinceUnixEpoch()) {
            histogram.add(value.value().doubleValue());
            lastSampleTime = value.taken();
        }
    }

    @Nullable
    @Override
    public Double reduce() {
        // Update model
        update();
        final Double sampleMedian = medianOfRssi();
        if (null == sampleMedian) {
            logger.debug("reduce, sample median is null");
            return null;
        }
        final double normalisedMedian = histogram.normalise(sampleMedian);
        if (normalisedMedian < -99) {
            logger.debug("reduce, out of range (reason=tooFar,median={},minRssi={})", normalisedMedian, -99);
            return null;
        }
        if (normalisedMedian > maxRssi) {
            logger.debug("reduce, out of range (reason=tooNear,median={},maxRssi={})", normalisedMedian, maxRssi);
            return null;
        }
        final Double distanceInMetres = min.value + (intercept - normalisedMedian) * coefficient;
        if (distanceInMetres <= 0) {
            logger.debug("reduce, out of range (reason=tooNear,median={},distance={})", normalisedMedian, distanceInMetres);
            return null;
        }
        return distanceInMetres;
    }
}
