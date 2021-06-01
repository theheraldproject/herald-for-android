//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.DefaultSensorDelegate;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.analysis.Sample;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// CSV log of event time intervals for post event analysis and visualisation
public class EventTimeIntervalLog extends DefaultSensorDelegate {
    @NonNull
    private final TextFile textFile;
    private final PayloadData payloadData;
    private final EventType eventType;
    private final Map<TargetIdentifier, String> targetIdentifierToPayload = new ConcurrentHashMap<>();
    private final Map<String, Date> payloadToTime = new ConcurrentHashMap<>();
    private final Map<String, Sample> payloadToSample = new ConcurrentHashMap<>();
    public  enum EventType {
        detect,read,measure,share,sharedPeer,visit
    }

    public EventTimeIntervalLog(@NonNull final Context context, @NonNull final String filename, final PayloadData payloadData, final EventType eventType) {
        this.textFile = new TextFile(context, filename);
        this.payloadData = payloadData;
        this.eventType = eventType;
    }

    @NonNull
    private String csv(@NonNull String value) {
        return TextFile.csv(value);
    }

    private void add(String payload) {
        final Date time = payloadToTime.get(payload);
        final Sample sample = payloadToSample.get(payload);
        if (null == time || null == sample) {
            payloadToTime.put(payload, new Date());
            payloadToSample.put(payload, new Sample());
            return;
        }
        final Date now = new Date();
        payloadToTime.put(payload, now);
        sample.add((now.getTime() - time.getTime()) / 1000d);
        write();
    }

    private void write() {
        final StringBuilder content = new StringBuilder("event,central,peripheral,count,mean,sd,min,max\n");
        final List<String> payloadList = new ArrayList<>();
        final String event = csv(eventType.name());
        final String centralPayload = csv(payloadData.shortName());
        for (String payload : payloadToSample.keySet()) {
            if (payload.equals(payloadData.shortName())) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (String payload : payloadList) {
            final Sample sample = payloadToSample.get(payload);
            if (null == sample) {
                continue;
            }
            if (null == sample.mean() || null == sample.standardDeviation() || null == sample.min() || null == sample.max()) {
                continue;
            }
            content.append(event);
            content.append(',');
            content.append(centralPayload);
            content.append(',');
            content.append(csv(payload));
            content.append(',');
            content.append(sample.count());
            content.append(',');
            content.append(sample.mean());
            content.append(',');
            content.append(sample.standardDeviation());
            content.append(',');
            content.append(sample.min());
            content.append(',');
            content.append(sample.max());
            content.append('\n');
        }
        textFile.overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, @NonNull PayloadData didRead, TargetIdentifier fromTarget) {
        final String payload = didRead.shortName();
        targetIdentifierToPayload.put(fromTarget, payload);
        if (eventType == EventType.read) {
            add(payload);
        }
    }

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        if (eventType == EventType.detect) {
            final String payload = targetIdentifierToPayload.get(didDetect);
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        if (eventType == EventType.measure) {
            final String payload = targetIdentifierToPayload.get(fromTarget);
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }

    @Override
    public void sensor(SensorType sensor, @NonNull List<PayloadData> didShare, TargetIdentifier fromTarget) {
        if (eventType == EventType.share) {
            final String payload = targetIdentifierToPayload.get(fromTarget);
            if (null == payload) {
                return;
            }
            add(payload);
        } else if (eventType == EventType.sharedPeer) {
            for (final PayloadData sharedPeer : didShare) {
                final String payload = sharedPeer.shortName();
                if (null == payload) {
                    return;
                }
                add(payload);
            }
        }
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        if (eventType == EventType.visit) {
            final String payload = payloadData.shortName();
            if (null == payload) {
                return;
            }
            add(payload);
        }
    }
}
