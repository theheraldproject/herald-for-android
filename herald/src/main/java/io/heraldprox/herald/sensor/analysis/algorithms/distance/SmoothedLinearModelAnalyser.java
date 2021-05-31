//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import io.heraldprox.herald.sensor.analysis.sampling.AnalysisProvider;
import io.heraldprox.herald.sensor.analysis.sampling.CallableForNewSample;
import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.analysis.sampling.SampleList;
import io.heraldprox.herald.sensor.analysis.sampling.SampledID;
import io.heraldprox.herald.sensor.analysis.views.InRange;
import io.heraldprox.herald.sensor.analysis.views.Since;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class SmoothedLinearModelAnalyser implements AnalysisProvider<RSSI, Distance> {
    private final SensorLogger logger = new ConcreteSensorLogger("Analysis", "SmoothedLinearModelAnalyser");
    private final TimeInterval interval;
    private final TimeInterval smoothingWindow;
    private final SmoothedLinearModel model;
    private Date lastRan = new Date(0);
    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public SmoothedLinearModelAnalyser() {
        this(new TimeInterval(4), new TimeInterval(60), new SmoothedLinearModel<RSSI>());
    }

    public SmoothedLinearModelAnalyser(final TimeInterval interval, final TimeInterval smoothingWindow, final SmoothedLinearModel<RSSI> smoothedLinearModel) {
        this.interval = interval;
        this.smoothingWindow = smoothingWindow;
        this.model = smoothedLinearModel;
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
        if (0 == validInput.size()) {
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
        if (null == distance) {
            logger.debug("analyse, skipped (reason=outOfModelRange,mediaOfRssi={})", model.medianOfRssi());
            return false;
        }
        // Publish distance data
        final Date timeStart = window.get(0).taken();
        final Date timeEnd = window.latest();
        final Date timeMiddle = new Date(timeEnd.secondsSinceUnixEpoch() - ((timeEnd.secondsSinceUnixEpoch() - timeStart.secondsSinceUnixEpoch()) / 2));
        logger.debug("analyse (timeStart={},timeEnd={},timeMiddle={},samples={},medianOfRssi={},distance={})", timeStart, timeEnd, timeMiddle, window.size(), model.medianOfRssi(), distance);
        final Sample<Distance> newSample = new Sample<>(timeMiddle, new Distance(distance));
        output.push(newSample);
        callable.newSample(sampled, newSample);
        return true;
    }
}
