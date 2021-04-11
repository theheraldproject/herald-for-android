//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.algorithms.distance;

import com.vmware.herald.sensor.analysis.sampling.AnalysisProvider;
import com.vmware.herald.sensor.analysis.sampling.CallableForNewSample;
import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampleList;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.analysis.views.InPeriod;
import com.vmware.herald.sensor.analysis.views.InRange;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.Distance;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.TimeInterval;

public class SmoothedLinearModelAnalyser implements AnalysisProvider<RSSI, Distance> {
    private final TimeInterval interval;
    private final TimeInterval smoothingWindow;
    private final long halfOfSmoothingWindow;
    private final SmoothedLinearModel model;
    private Date lastRan = new Date(0);
    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public SmoothedLinearModelAnalyser() {
        this(10, TimeInterval.minute, -17.7275, -0.2754);
    }

    public SmoothedLinearModelAnalyser(final long interval, final TimeInterval smoothingWindow, final double intercept, final double coefficient) {
        this.interval = new TimeInterval(interval);
        this.smoothingWindow = smoothingWindow;
        this.halfOfSmoothingWindow = smoothingWindow.value / 2;
        this.model = new SmoothedLinearModel(intercept, coefficient);
    }

    @Override
    public Class<RSSI> inputType() {
        return RSSI.class;
    }

    @Override
    public Class<Distance> outputType() {
        return Distance.class;
    }

    @Override
    public boolean analyse(Date timeNow, SampledID sampled, SampleList<RSSI> input, final SampleList<Distance> output, CallableForNewSample<Distance> callable) {
        // Interval guard
        if (lastRan.secondsSinceUnixEpoch() + interval.value >= timeNow.secondsSinceUnixEpoch()) {
            return false;
        }
        // Input guard : Must have data to analyse
        if (input.size() == 0) {
            return false;
        }
        // Smoothing window guard : Look ahead for 1/2 of smoothing window duration
        if (input.latest().secondsSinceUnixEpoch() - timeNow.secondsSinceUnixEpoch() < halfOfSmoothingWindow) {
            return false;
        }
        // Smoothing window guard : Look behind for 1/2 of smoothing window duration
        if (timeNow.secondsSinceUnixEpoch() - input.get(0).taken().secondsSinceUnixEpoch() < halfOfSmoothingWindow) {
            return false;
        }
        // Only consider valid samples within smoothing window
        model.reset();
        final Filter<RSSI> inSmoothingWindow = new InPeriod<>(timeNow.secondsSinceUnixEpoch() - halfOfSmoothingWindow, timeNow.secondsSinceUnixEpoch() + halfOfSmoothingWindow);
        final SampleList<RSSI> window = input.filter(valid).filter(inSmoothingWindow).toView();
        // Estimate distance based on smoothed linear model
        final double distance = window.aggregate(model).get(SmoothedLinearModel.class);
        // Publish distance data
        final Sample<Distance> newSample = new Sample<>(timeNow, new Distance(distance));
        output.push(newSample);
        callable.newSample(sampled, newSample);
        return true;
    }
}
