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
import com.vmware.herald.sensor.analysis.views.InRange;
import com.vmware.herald.sensor.analysis.views.Since;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.Distance;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.TimeInterval;

public class SmoothedLinearModelAnalyser implements AnalysisProvider<RSSI, Distance> {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "SmoothedLinearModelAnalyser");
    private final TimeInterval interval;
    private final TimeInterval smoothingWindow;
    private final SmoothedLinearModel model;
    private Date lastRan = new Date(0);
    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public SmoothedLinearModelAnalyser() {
        this(1, TimeInterval.seconds(60), -10.6522, -0.181);
    }

    public SmoothedLinearModelAnalyser(final long interval, final TimeInterval smoothingWindow, final double intercept, final double coefficient) {
        this.interval = new TimeInterval(interval);
        this.smoothingWindow = smoothingWindow;
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
        final TimeInterval secondsSinceLastRan = new TimeInterval(timeNow.secondsSinceUnixEpoch() - lastRan.secondsSinceUnixEpoch());
        if (secondsSinceLastRan.value < interval.value) {
            logger.debug("analyse, skipped (reason=elapsedSinceLastRanBelowInterval,interval={}s,timeSinceLastRan={}s,lastRan={})", interval, secondsSinceLastRan, lastRan);
            return false;
        }
        // Input guard : Must have valid data to analyse
        final SampleList<RSSI> validInput = input.filter(valid).toView();
        if (validInput.size() == 0) {
            logger.debug("analyse, skipped (reason=noValidData,inputSamples={},validInputSamples={})", input.size(), validInput.size());
            return false;
        }
        // Input guard : Must cover entire smoothing window
        final TimeInterval observed = new TimeInterval(timeNow.secondsSinceUnixEpoch() - validInput.get(0).taken().secondsSinceUnixEpoch());
        if (observed.value < smoothingWindow.value) {
            logger.debug("analyse, skipped (reason=insufficientHistoricDataForSmoothing,required={}s,observed={}s)", smoothingWindow, observed);
            return false;
        }
        // Input guard : Must have sufficient data in smoothing window
        final SampleList<RSSI> window = validInput.filter(new Since<>(timeNow.secondsSinceUnixEpoch() - smoothingWindow.value)).toView();
        if (window.size() < 5) {
            logger.debug("analyse, skipped (reason=insufficientDataInSmoothingWindow,minimum=5,samplesInWindow={})", window.size());
            return false;
        }
        // Estimate distance based on smoothed linear model
        model.reset();
        final Double distance = window.aggregate(model).get(SmoothedLinearModel.class);
        if (distance == null) {
            logger.debug("analyse, skipped (reason=outOfModelRange,mediaOfRssi={},maximumRssiAtZeroDistance={})", model.medianOfRssi(), model.maximumRssi());
            return false;
        }
        // Publish distance data
        final Date timeStart = window.get(0).taken();
        final Date timeEnd = window.latest();
        final Date timeMiddle = new Date(timeEnd.secondsSinceUnixEpoch() - ((timeEnd.secondsSinceUnixEpoch() - timeStart.secondsSinceUnixEpoch()) / 2));
        logger.debug("analyse (timeStart={},timeEnd={},timeMiddle={},samples={},medianOfRssi={},distance={})", timeStart, timeEnd, timeMiddle, window.size(), model.medianOfRssi(), distance);
        final Sample<Distance> newSample = new Sample<>(timeEnd, new Distance(distance));
        output.push(newSample);
        callable.newSample(sampled, newSample);
        return true;
    }
}
