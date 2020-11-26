//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.app;

import com.vmware.herald.sensor.analysis.Sample;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.util.Date;

public class Target {
    private TargetIdentifier targetIdentifier = null;
    private PayloadData payloadData = null;
    private Date lastUpdatedAt = null;
    private Proximity proximity = null;
    private ImmediateSendData received = null;
    private Date didRead = null, didMeasure = null, didShare = null, didReceive = null;
    private Sample didReadTimeInterval = new Sample();
    private Sample didMeasureTimeInterval = new Sample();

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

    public Sample didReadTimeInterval() { return didReadTimeInterval; }

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

    public Sample didMeasureTimeInterval() {
        return didMeasureTimeInterval;
    }

    public Date didShare() {
        return didShare;
    }

    public void didShare(Date date) {
        didShare = date;
        lastUpdatedAt = didShare;
    }
}
