//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import androidx.annotation.NonNull;

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
    @NonNull
    private final TimeInterval interval;
    @NonNull
    private final TimeInterval smoothingWindow;
    @NonNull
    private final SmoothedLinearModel model;
    @NonNull
    private Date lastRan = new Date(0);
    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public SmoothedLinearModelAnalyser() {
        this(new TimeInterval(4), new TimeInterval(60), new SmoothedLinearModel<RSSI>());
    }

    public SmoothedLinearModelAnalyser(@NonNull final TimeInterval interval, @NonNull final TimeInterval smoothingWindow, @NonNull final SmoothedLinearModel<RSSI> smoothedLinearModel) {
        this.interval = interval;
        this.smoothingWindow = smoothingWindow;
        this.model = smoothedLinearModel;
    }

    @NonNull
    @Override
    public Class<RSSI> inputType() {
        return RSSI.class;
    }

    @NonNull
    @Override
    public Class<Distance> outputType() {
        return Distance.class;
    }

    @Override
    public boolean analyse(@NonNull final Date timeNow, @NonNull final SampledID sampled, @NonNull final SampleList<RSSI> input, @NonNull final SampleList<Distance> output, @NonNull CallableForNewSample<Distance> callable) {
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
        final Sample<RSSI> firstValidInput = validInput.get(0);
        if (null == firstValidInput) {
            logger.debug("analyse, skipped (reason=noValidData,inputSamples={},validInputSamples={})", input.size(), validInput.size());
            return false;
        }
        final TimeInterval observed = new TimeInterval(timeNow.secondsSinceUnixEpoch() - firstValidInput.taken().secondsSinceUnixEpoch());
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
        final Sample<RSSI> sampleStart = window.get(0);
        if (null == sampleStart) {
            logger.debug("analyse, skipped (reason=emptyWindow)");
            return false;
        }
        final Date timeStart = sampleStart.taken();
        final Date timeEnd = window.latest();
        if (null == timeEnd) {
            logger.debug("analyse, skipped (reason=missingTimeEnd)");
            return false;
        }
        final Date timeMiddle = new Date(timeEnd.secondsSinceUnixEpoch() - ((timeEnd.secondsSinceUnixEpoch() - timeStart.secondsSinceUnixEpoch()) / 2));
        logger.debug("analyse (timeStart={},timeEnd={},timeMiddle={},samples={},medianOfRssi={},distance={})", timeStart, timeEnd, timeMiddle, window.size(), model.medianOfRssi(), distance);
        final Sample<Distance> newSample = new Sample<>(timeMiddle, new Distance(distance));
        output.push(newSample);
        callable.newSample(sampled, newSample);
        lastRan = new Date();
        return true;
    }
}
