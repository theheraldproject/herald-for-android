//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.app;

import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.Date;

public class Target {
    private TargetIdentifier targetIdentifier = null;
    private PayloadData payloadData = null;
    private Date lastUpdatedAt = null;
    private Proximity proximity = null;
    private Distance distance = null;
    private ImmediateSendData received = null;
    private Date didRead = null, didMeasure = null, didShare = null, didReceive = null;
    private Distribution didReadTimeInterval = new Distribution();
    private Distribution didMeasureTimeInterval = new Distribution();
    private Distribution didShareTimeInterval = new Distribution();

    public Target(TargetIdentifier targetIdentifier, PayloadData payloadData) {
        this.targetIdentifier = targetIdentifier;
        this.payloadData = payloadData;
        lastUpdatedAt = new Date();
        didRead = lastUpdatedAt;
    }

    public TargetIdentifier targetIdentifier() {
        return targetIdentifier;
    }

    public void targetIdentifier(TargetIdentifier targetIdentifier) {
        lastUpdatedAt = new Date();
        this.targetIdentifier = targetIdentifier;
    }

    public PayloadData payloadData() {
        return payloadData;
    }

    public Date lastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Proximity proximity() {
        return proximity;
    }

    public void proximity(Proximity proximity) {
        final Date date = new Date();
        if (didMeasure != null) {
            final TimeInterval timeInterval = new TimeInterval(didMeasure, date);
            didMeasureTimeInterval.add(timeInterval.value);
        }
        lastUpdatedAt = date;
        didMeasure = lastUpdatedAt;
        this.proximity = proximity;
    }

    public Distance distance() { return distance; }

    public void distance(Distance distance) {
        this.distance = distance;
    }

    public ImmediateSendData received() {
        return received;
    }

    public void received(ImmediateSendData received) {
        lastUpdatedAt = new Date();
        didReceive = lastUpdatedAt;
        this.received = received;
    }

    public Date didReceive() {
        return didReceive;
    }

    public Date didRead() {
        return didRead;
    }

    public Distribution didReadTimeInterval() { return didReadTimeInterval; }

    public void didRead(Date date) {
        if (didRead != null && date != null) {
            final TimeInterval timeInterval = new TimeInterval(didRead, date);
            didReadTimeInterval.add(timeInterval.value);
        }
        didRead = date;
        lastUpdatedAt = didRead;
    }

    public Date didMeasure() {
        return didMeasure;
    }

    public Distribution didMeasureTimeInterval() {
        return didMeasureTimeInterval;
    }

    public Date didShare() {
        return didShare;
    }

    public void didShare(Date date) {
        if (didShare != null && date != null) {
            final TimeInterval timeInterval = new TimeInterval(didShare, date);
            didShareTimeInterval.add(timeInterval.value);
        }
        didShare = date;
        lastUpdatedAt = didShare;
    }

    public Distribution didShareTimeInterval() {
        return didShareTimeInterval;
    }
}
