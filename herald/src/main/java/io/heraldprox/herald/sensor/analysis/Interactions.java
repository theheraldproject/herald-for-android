//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorDelegateLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.datatype.Encounter;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Log of interactions for recording encounters (time, proximity, and identity).
 * This is can be used as basis for maintaining a persistent log of encounters
 * for on-device or centralised matching.
 */
public class Interactions extends SensorDelegateLogger {
    @SuppressWarnings("FieldCanBeLocal")
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Analysis.EncounterLog");
    @NonNull
    private List<Encounter> encounters = new ArrayList<>();

    public Interactions() {
        super();
    }

    public Interactions(@NonNull final Context context, @NonNull final String filename) {
        super(context, filename);
        for (String line : contentsOf().split("\n")) {
            final Encounter encounter = new Encounter(line);
            if (encounter.isValid()) {
                encounters.add(encounter);
            }
        }
        logger.debug("Loaded historic encounters (count={})", encounters.size());
    }

    @Override
    public synchronized void reset() {
        super.reset();
        encounters.clear();
    }

    private void writeHeader() {
        if (empty()) {
            write("time,proximity,unit,payload");
        }
    }

    public synchronized void append(@NonNull final Encounter encounter) {
        writeHeader();
        write(encounter.csvString());
        encounters.add(encounter);
    }

    /**
     * Get encounters from start date (inclusive) to end date (exclusive)
     * @param start Start date (inclusive)
     * @param end End date (exclusive)
     * @return Encounters in requested period.
     */
    @NonNull
    public synchronized List<Encounter> subdata(@NonNull final Date start, @NonNull final Date end) {
        final long startTime = start.getTime();
        final long endTime = end.getTime();
        final List<Encounter> subdata = new ArrayList<>();
        for (Encounter encounter : encounters) {
            if (null == encounter.timestamp) {
                continue;
            }
            final long time = encounter.timestamp.getTime();
            if (time >= startTime && time < endTime) {
                subdata.add(encounter);
            }
        }
        return subdata;
    }

    /**
     * Get all encounters from start date (inclusive)
     * @param start Start date (inclusive)
     * @return Encounters from start date to now.
     */
    @NonNull
    public synchronized List<Encounter> subdata(@NonNull final Date start) {
        final long startTime = start.getTime();
        final List<Encounter> subdata = new ArrayList<>();
        for (final Encounter encounter : encounters) {
            if (null == encounter.timestamp) {
                continue;
            }
            final long time = encounter.timestamp.getTime();
            if (time >= startTime) {
                subdata.add(encounter);
            }
        }
        return subdata;
    }

    /**
     * Remove all log records before date (exclusive). Use this function to implement
     * data retention policy.
     * @param before Cut off date (exclusive)
     */
    public synchronized void remove(@NonNull final Date before) {
        final StringBuilder content = new StringBuilder();
        content.append("time,proximity,unit,payload\n");
        final List<Encounter> subdata = subdata(before);
        for (final Encounter encounter : subdata) {
            content.append(encounter.csvString());
            content.append("\n");
        }
        overwrite(content.toString());
        encounters = subdata;
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget, @NonNull final PayloadData withPayload) {
        final Encounter encounter = new Encounter(didMeasure, withPayload);
        if (encounter.isValid()) {
            append(encounter);
        }
    }

    // MARK:- Analysis functions

    /**
     * Herald achieves > 93% continuity for 30 second windows, thus quantising encounter timestamps into 60 second
     * windows will offer a reasonable estimate of the different number of devices within detection range over time. The
     * result is a timeseries of different payloads acquired during each 60 second window, along with the proximity data
     * for each payload.
     */
    public final static class InteractionsForTime {
        public final Date time;
        @NonNull
        public final Map<PayloadData,List<Proximity>> context;

        public InteractionsForTime(@NonNull final Date time, @NonNull final Map<PayloadData,List<Proximity>> context) {
            this.time = time;
            this.context = context;
        }

        @NonNull
        @Override
        public String toString() {
            return "InteractionsForTime{" +
                    "time=" + time +
                    ", context=" + context +
                    '}';
        }
    }

    @NonNull
    public static List<InteractionsForTime> reduceByTime(@NonNull final List<Encounter> encounters) {
        return reduceByTime(encounters, TimeInterval.minute);
    }

    @NonNull
    public static List<InteractionsForTime> reduceByTime(@NonNull final List<Encounter> encounters, @NonNull final TimeInterval duration) {
        final List<InteractionsForTime> result = new ArrayList<>();
        final long divisor = duration.value * 1000;
        long currentTimeWindow = 0;
        Map<PayloadData,List<Proximity>> context = new HashMap<>();
        for (final Encounter encounter : encounters) {
            if (null == encounter.timestamp) {
                continue;
            }
            final long timeWindow = (encounter.timestamp.getTime() / divisor) * divisor;
            if (timeWindow != currentTimeWindow) {
                if (!context.isEmpty()) {
                    result.add(new InteractionsForTime(new Date(currentTimeWindow), context));
                    context = new HashMap<>();
                }
                currentTimeWindow = timeWindow;
            }
            List<Proximity> proximities = context.get(encounter.payload);
            if (null == proximities) {
                proximities = new ArrayList<>(1);
                context.put(encounter.payload, proximities);
            }
            proximities.add(encounter.proximity);
        }
        if (!context.isEmpty()) {
            result.add(new InteractionsForTime(new Date(currentTimeWindow), context));
        }
        return result;
    }

    /**
     * Get all target devices, duration and proximity distribution. The result is a table of payload data
     * and summary information, including last seen at time, total duration of exposure, and distribution
     * of proximity (RSSI) values.
     */
    public final static class InteractionsForTarget {
        @NonNull
        public final Date lastSeenAt;
        @NonNull
        public final TimeInterval duration;
        @NonNull
        public final Distribution proximity;

        public InteractionsForTarget(@NonNull final Date lastSeenAt, @NonNull final TimeInterval duration, @NonNull final Distribution proximity) {
            this.lastSeenAt = lastSeenAt;
            this.duration = duration;
            this.proximity = proximity;
        }

        @NonNull
        @Override
        public String toString() {
            return "InteractionsForTarget{" +
                    "lastSeenAt=" + lastSeenAt +
                    ", duration=" + duration +
                    ", proximity=" + proximity +
                    '}';
        }
    }

    @NonNull
    public static Map<PayloadData, InteractionsForTarget> reduceByTarget(@NonNull final List<Encounter> encounters) {
        final Map<PayloadData, InteractionsForTarget> targets = new HashMap<>();
        for (final Encounter encounter : encounters) {
            if (null == encounter.timestamp || null == encounter.proximity || null == encounter.payload) {
                continue;
            }
            if (encounter.proximity.unit != ProximityMeasurementUnit.RSSI) {
                continue;
            }
            final InteractionsForTarget triple = targets.get(encounter.payload);
            if (null == triple) {
                // One encounter is assumed to be at least 1 second minimum
                final Distribution proximity = new Distribution(encounter.proximity.value, 1);
                targets.put(encounter.payload, new InteractionsForTarget(encounter.timestamp, new TimeInterval(1), proximity));
                continue;
            }
            final TimeInterval elapsed = new TimeInterval(triple.lastSeenAt, encounter.timestamp);
            if (elapsed.value > 30) {
                // Two encounters separated by > 30 seconds is assumed to be disjointed
                targets.put(encounter.payload, new InteractionsForTarget(encounter.timestamp, triple.duration, triple.proximity));
                continue;
            }
            // Two encounters within 30 seconds is assumed to be continuous
            // Proximity for every second of the most recent period of encounter
            // is assumed to be the most recent measurement
            triple.proximity.add(encounter.proximity.value, elapsed.value);
            targets.put(encounter.payload, new InteractionsForTarget(encounter.timestamp, new TimeInterval(triple.duration.value + elapsed.value), triple.proximity));
        }
        return targets;
    }

    /**
     * Histogram of exposure offers an estimate of exposure, while avoiding resolution of actual payload identity.
     * @param encounters List of encounters.
     * @return Histogram of proximity (time duration at each RSSI value)
     */
    @NonNull
    public static Map<Double,TimeInterval> reduceByProximity(@NonNull final List<Encounter> encounters) {
        return reduceByProximity(encounters, ProximityMeasurementUnit.RSSI, 1d);
    }

    @NonNull
    public static Map<Double,TimeInterval> reduceByProximity(@NonNull final List<Encounter> encounters, @NonNull final ProximityMeasurementUnit unit, @NonNull final Double bin) {
        final Map<PayloadData,Date> targets = new HashMap<>();
        final Map<Double,TimeInterval> histogram = new HashMap<>();
        for (final Encounter encounter : encounters) {
            if (null == encounter.timestamp || null == encounter.proximity || null == encounter.payload) {
                continue;
            }
            if (encounter.proximity.unit != unit) {
                continue;
            }
            final Double value = Math.round(encounter.proximity.value / bin) * bin;
            final Date lastSeenAt = targets.get(encounter.payload);
            if (null == lastSeenAt) {
                // One encounter is assumed to be at least 1 second minimum
                final TimeInterval timeInterval = histogram.get(value);
                histogram.put(value, new TimeInterval(1 + (null == timeInterval ? 0 : timeInterval.value)));
                targets.put(encounter.payload, encounter.timestamp);
                continue;
            }
            final TimeInterval elapsed = new TimeInterval(lastSeenAt, encounter.timestamp);
            if (elapsed.value > 30) {
                // Two encounters separated by > 30 seconds is assumed to be disjointed
                targets.put(encounter.payload, encounter.timestamp);
                continue;
            }
            // Two encounters within 30 seconds is assumed to be continuous
            // Proximity for every second of the most recent period of encounter
            // is assumed to be the most recent measurement
            final TimeInterval timeInterval = histogram.get(value);
            histogram.put(value, new TimeInterval(elapsed.value + (null == timeInterval ? 0 : timeInterval.value)));
            targets.put(encounter.payload, encounter.timestamp);
        }
        return histogram;
    }
}
