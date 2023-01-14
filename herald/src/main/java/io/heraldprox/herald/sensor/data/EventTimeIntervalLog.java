//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSV log of event time intervals for post event analysis ands visualisation.
 */
public class EventTimeIntervalLog extends SensorDelegateLogger {
    @NonNull
    private final PayloadData payloadData;
    @NonNull
    private final EventType eventType;
    private final Map<TargetIdentifier, String> targetIdentifierToPayload = new ConcurrentHashMap<>();
    private final Map<String, Date> payloadToTime = new ConcurrentHashMap<>();
    private final Map<String, Distribution> payloadToSample = new ConcurrentHashMap<>();
    public  enum EventType {
        detect,read,measure,share,sharedPeer,visit
    }

    public EventTimeIntervalLog(@NonNull final Context context, @NonNull final String filename, @NonNull final PayloadData payloadData, @NonNull final EventType eventType) {
        super(context, filename);
        this.payloadData = payloadData;
        this.eventType = eventType;
        write();
    }

    public EventTimeIntervalLog(@NonNull final TextFile textFile, @NonNull final PayloadData payloadData, @NonNull final EventType eventType) {
        super(textFile);
        this.payloadData = payloadData;
        this.eventType = eventType;
        write();
    }

    private void add(@NonNull final String payload) {
        final Date time = payloadToTime.get(payload);
        final Distribution distribution = payloadToSample.get(payload);
        if (null == time || null == distribution) {
            payloadToTime.put(payload, new Date());
            payloadToSample.put(payload, new Distribution());
            return;
        }
        final Date now = new Date();
        payloadToTime.put(payload, now);
        distribution.add((now.getTime() - time.getTime()) / 1000d);
        write();
    }

    private void write() {
        final StringBuilder content = new StringBuilder("event,central,peripheral,count,mean,sd,min,max\n");
        final List<String> payloadList = new ArrayList<>();
        final String event = csv(eventType.name());
        final String centralPayload = csv(payloadData.shortName());
        for (final String payload : payloadToSample.keySet()) {
            if (EventType.visit != eventType && payload.equals(payloadData.shortName())) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (final String payload : payloadList) {
            final Distribution distribution = payloadToSample.get(payload);
            if (null == distribution) {
                continue;
            }
            if (null == distribution.mean() || null == distribution.standardDeviation() || null == distribution.min() || null == distribution.max()) {
                continue;
            }
            content.append(event);
            content.append(',');
            content.append(centralPayload);
            content.append(',');
            content.append(csv(payload));
            content.append(',');
            content.append(distribution.count());
            content.append(',');
            content.append(distribution.mean());
            content.append(',');
            content.append(distribution.standardDeviation());
            content.append(',');
            content.append(distribution.min());
            content.append(',');
            content.append(distribution.max());
            content.append('\n');
        }
        overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget) {
        final String payload = didRead.shortName();
        targetIdentifierToPayload.put(fromTarget, payload);
        if (eventType == EventType.read) {
            add(payload);
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final TargetIdentifier didDetect) {
        if (eventType == EventType.detect) {
            final String payload = targetIdentifierToPayload.get(didDetect);
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget) {
        if (eventType == EventType.measure) {
            final String payload = targetIdentifierToPayload.get(fromTarget);
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
        if (eventType == EventType.share) {
            final String payload = targetIdentifierToPayload.get(fromTarget);
            if (null == payload) {
                return;
            }
            add(payload);
        } else if (eventType == EventType.sharedPeer) {
            for (final PayloadData sharedPeer : didShare) {
                final String payload = sharedPeer.shortName();
                //noinspection ConstantConditions
                if (null == payload) {
                    return;
                }
                add(payload);
            }
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Location didVisit) {
        if (eventType == EventType.visit) {
            final String payload = payloadData.shortName();
            //noinspection ConstantConditions
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }
}
