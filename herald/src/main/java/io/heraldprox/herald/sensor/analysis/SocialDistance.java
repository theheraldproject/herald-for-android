//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Encounter;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.Date;
import java.util.List;

/**
 * Estimate social distance to other app users to encourage people to keep their distance from
 * people. This is intended to be used to generate a daily score as indicator of behavioural change
 * to improve awareness of social mixing behaviour.
 */
public class SocialDistance extends Interactions {

    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget) {
        final Encounter encounter = new Encounter(didMeasure, new PayloadData(fromTarget.value.getBytes()));
        if (encounter.isValid()) {
            append(encounter);
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget, @NonNull final PayloadData withPayload) {
        // Interactions require a valid payload, but that is unnecessary for social distance
        // Overriding parent function with no-op, replaced with sensor(sensor:didMeasure:fromTarget)
    }

    /**
     * Calculate social distance score based on maximum RSSI per 1 minute time window over duration.
     * A score of 1.0 means RSSI >= measuredPower in every minute, score of 0.0 means no encounter
     * or RSSI less than excludeRssiBelow in every minute.
     * @param start Start time (inclusive)
     * @param end End time (exclusive)
     * @return Social distance score with range [0,1]
     */
    @NonNull
    public Double scoreByProximity(@NonNull final Date start, @NonNull final Date end) {
        // measuredPower defines RSSI at 1 metre
        // excludeRssiBelow defines minimum RSSI to include in analysis
        return scoreByProximity(start, end, -32d, -65d);
    }

    @NonNull
    public Double scoreByProximity(@NonNull final Date start, @NonNull final Date end, final double measuredPower, final double excludeRssiBelow) {
        // Get encounters over time period
        final List<Encounter> encounters = subdata(start, end);
        // Get number of minutes in time period
        final double duration = Math.ceil(new TimeInterval(start, end).value / 60d);
        if (0 == duration) {
            return 0d;
        }
        // Get interactions for each time windows over time period
        final List<InteractionsForTime> timeWindows = reduceByTime(encounters,  TimeInterval.minute);
        // Get sum of exposure in each time window
        final double rssiRange = measuredPower - excludeRssiBelow;
        double totalScore = 0;
        for (final InteractionsForTime timeWindow : timeWindows) {
            Double maxRSSI = null;
            for (List<Proximity> proximities : timeWindow.context.values()) {
                for (Proximity proximity : proximities) {
                    if (proximity.unit != ProximityMeasurementUnit.RSSI) {
                        continue;
                    }
                    if (!(proximity.value >= excludeRssiBelow && proximity.value <= 0)) {
                        continue;
                    }
                    if (null == maxRSSI || proximity.value > maxRSSI) {
                        maxRSSI = proximity.value;
                    }
                }
            }
            if (null == maxRSSI) {
                continue;
            }
            final double rssi = maxRSSI;
            final double rssiDelta = measuredPower - Math.min(rssi, measuredPower);
            final double rssiPercentage = 1.0 - (rssiDelta / rssiRange);
            totalScore = totalScore + rssiPercentage;
        }
        // Score for time period is totalScore / duration
        @SuppressWarnings("UnnecessaryLocalVariable") final double score = totalScore / duration;
        return score;
    }

    /**
     * Calculate social distance score based on number of different devices per 1 minute time window over duration.
     * A score of 1.0 means 6 or more in every minute, score of 0.0 means no device in every minute.
     * @param start
     * @param end
     * @param start Start time (inclusive)
     * @param end End time (exclusive)
     * @return Social distance score with range [0,1]
     */
    @NonNull
    public Double scoreByTarget(@NonNull final Date start, @NonNull final Date end) {
        return scoreByTarget(start, end, 6, -65);
    }

    @NonNull
    public Double scoreByTarget(@NonNull final Date start, @NonNull final Date end, final int maximumDeviceCount, final double excludeRssiBelow) {
        // Get encounters over time period
        final List<Encounter> encounters = subdata(start, end);
        // Get number of minutes in time period
        final double duration = Math.ceil(new TimeInterval(start, end).value / 60d);
        if (0 == duration) {
            return 0d;
        }
        // Get interactions for each time windows over time period
        final List<InteractionsForTime> timeWindows = reduceByTime(encounters,  TimeInterval.minute);
        // Get sum of exposure in each time window
        double totalScore = 0;
        for (InteractionsForTime timeWindow : timeWindows) {
            int devices = 0;
            for (List<Proximity> proximities : timeWindow.context.values()) {
                for (Proximity proximity : proximities) {
                    if (proximity.unit != ProximityMeasurementUnit.RSSI) {
                        continue;
                    }
                    if (proximity.value >= excludeRssiBelow && proximity.value <= 0) {
                        devices = devices + 1;
                        break;
                    }
                }
            }
            final double devicesPercentage = Math.min(devices, maximumDeviceCount) / (double) maximumDeviceCount;
            totalScore = totalScore + devicesPercentage;
        }
        // Score for time period is totalScore / duration
        //noinspection UnnecessaryLocalVariable
        final double score = totalScore / duration;
        return score;
    }
}
