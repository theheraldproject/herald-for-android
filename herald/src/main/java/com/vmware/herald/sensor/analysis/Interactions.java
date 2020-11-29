//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis;

import android.content.Context;

import com.vmware.herald.sensor.DefaultSensorDelegate;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Encounter;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.ProximityMeasurementUnit;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;
import com.vmware.herald.sensor.datatype.Triple;
import com.vmware.herald.sensor.datatype.Tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Log of interactions for recording encounters (time, proximity, and identity).
/// This is can be used as basis for maintaining a persistent log
/// of encounters for on-device or centralised matching.
public class Interactions extends DefaultSensorDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Analysis.EncounterLog");
    private final TextFile textFile;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private List<Encounter> encounters = new ArrayList<>();

    public Interactions() {
        textFile = null;
    }

    public Interactions(final Context context, final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,proximity,unit,payload");
        } else {
            final String content = textFile.contentsOf();
            for (String line : content.split("\n")) {
                final Encounter encounter = new Encounter(line);
                if (encounter.isValid()) {
                    encounters.add(encounter);
                }
            }
            logger.debug("Loaded historic encounters (count={})", encounters.size());
        }
    }

    public synchronized void append(Encounter encounter) {
        if (textFile != null) {
            textFile.write(encounter.csvString());
        }
        encounters.add(encounter);
    }

    /// Get encounters from start date (inclusive) to end date (exclusive)
    public synchronized List<Encounter> subdata(Date start, Date end) {
        final long startTime = start.getTime();
        final long endTime = end.getTime();
        final List<Encounter> subdata = new ArrayList<>();
        for (Encounter encounter : encounters) {
            if (encounter.timestamp == null) {
                continue;
            }
            final long time = encounter.timestamp.getTime();
            if (time >= startTime && time < endTime) {
                subdata.add(encounter);
            }
        }
        return subdata;
    }

    /// Get all encounters from start date (inclusive)
    public synchronized List<Encounter> subdata(Date start) {
        final long startTime = start.getTime();
        final List<Encounter> subdata = new ArrayList<>();
        for (Encounter encounter : encounters) {
            if (encounter.timestamp == null) {
                continue;
            }
            final long time = encounter.timestamp.getTime();
            if (time >= startTime) {
                subdata.add(encounter);
            }
        }
        return subdata;
    }

    /// Remove all log records before date (exclusive). Use this function to implement data retention policy.
    public synchronized void remove(Date before) {
        final StringBuilder content = new StringBuilder();
        final List<Encounter> subdata = subdata(before);
        for (Encounter encounter : subdata) {
            content.append(encounter.csvString());
            content.append("\n");
        }
        if (textFile != null) {
            textFile.overwrite(content.toString());
        }
        encounters = subdata;
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        final Encounter encounter = new Encounter(didMeasure, withPayload);
        if (encounter.isValid()) {
            append(encounter);
        }
    }

    // MARK:- Analysis functions

    /// Herald achieves > 93% continuity for 30 second windows, thus quantising encounter timestamps into 60 second
    /// windows will offer a reasonable estimate of the different number of devices within detection range over time. The
    /// result is a timeseries of different payloads acquired during each 60 second window, along with the proximity data
    /// for each payload.
    public final static class InteractionsForTime {
        public final Date time;
        public final Map<PayloadData,List<Proximity>> context;
        public InteractionsForTime(Date time, Map<PayloadData,List<Proximity>> context) {
            this.time = time;
            this.context = context;
        }
        @Override
        public String toString() {
            return "InteractionsForTime{" +
                    "time=" + time +
                    ", context=" + context +
                    '}';
        }
    }
    public final static List<InteractionsForTime> reduceByTime(List<Encounter> encounters) {
        return reduceByTime(encounters, TimeInterval.minute);
    }
    public final static List<InteractionsForTime> reduceByTime(List<Encounter> encounters, TimeInterval duration) {
        final List<InteractionsForTime> result = new ArrayList<>();
        final long divisor = duration.value * 1000;
        long currentTimeWindow = 0;
        Map<PayloadData,List<Proximity>> context = new HashMap<>();
        for (Encounter encounter : encounters) {
            final long timeWindow = (encounter.timestamp.getTime() / divisor) * divisor;
            if (timeWindow != currentTimeWindow) {
                if (!context.isEmpty()) {
                    result.add(new InteractionsForTime(new Date(currentTimeWindow), context));
                    context = new HashMap<>();
                }
                currentTimeWindow = timeWindow;
            }
            List<Proximity> proximities = context.get(encounter.payload);
            if (proximities == null) {
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

    /// Get all target devices, duration and proximity distribution. The result is a table of payload data
    /// and summary information, including last seen at time, total duration of exposure, and distribution
    /// of proximity (RSSI) values.
    public final static class InteractionsForTarget {
        public final Date lastSeenAt;
        public final TimeInterval duration;
        public final Sample proximity;
        public InteractionsForTarget(Date lastSeenAt, TimeInterval duration, Sample proximity) {
            this.lastSeenAt = lastSeenAt;
            this.duration = duration;
            this.proximity = proximity;
        }
        @Override
        public String toString() {
            return "InteractionsForTarget{" +
                    "lastSeenAt=" + lastSeenAt +
                    ", duration=" + duration +
                    ", proximity=" + proximity +
                    '}';
        }
    }
    public final static Map<PayloadData, InteractionsForTarget> reduceByTarget(List<Encounter> encounters) {
        final Map<PayloadData, InteractionsForTarget> targets = new HashMap<>();
        for (Encounter encounter : encounters) {
            if (encounter.proximity.unit != ProximityMeasurementUnit.RSSI) {
                continue;
            }
            InteractionsForTarget triple = targets.get(encounter.payload);
            if (triple == null) {
                // One encounter is assumed to be at least 1 second minimum
                final Sample proximity = new Sample(encounter.proximity.value, 1);
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

    /// Histogram of exposure offers an esimate of exposure, while avoiding resolution of actual payload identity.
    public final static Map<Double,TimeInterval> reduceByProximity(List<Encounter> encounters) {
        return reduceByProximity(encounters, ProximityMeasurementUnit.RSSI, 1d);
    }
    public final static Map<Double,TimeInterval> reduceByProximity(List<Encounter> encounters, ProximityMeasurementUnit unit, Double bin) {
        final Map<PayloadData,Date> targets = new HashMap<>();
        final Map<Double,TimeInterval> histogram = new HashMap<>();
        for (Encounter encounter : encounters) {
            if (encounter.proximity.unit != unit) {
                continue;
            }
            final Double value = Math.round(encounter.proximity.value / bin) * bin;
            Date lastSeenAt = targets.get(encounter.payload);
            if (lastSeenAt == null) {
                // One encounter is assumed to be at least 1 second minimum
                final TimeInterval timeInterval = histogram.get(value);
                histogram.put(value, new TimeInterval(1 + (timeInterval == null ? 0 : timeInterval.value)));
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
            histogram.put(value, new TimeInterval(elapsed.value + (timeInterval == null ? 0 : timeInterval.value)));
            targets.put(encounter.payload, encounter.timestamp);
        }
        return histogram;
    }
}
