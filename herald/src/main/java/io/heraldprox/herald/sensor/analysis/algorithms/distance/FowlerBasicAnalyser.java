//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import io.heraldprox.herald.sensor.analysis.aggregates.Mode;
import io.heraldprox.herald.sensor.analysis.aggregates.Variance;
import io.heraldprox.herald.sensor.analysis.sampling.AnalysisProvider;
import io.heraldprox.herald.sensor.analysis.sampling.CallableForNewSample;
import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.analysis.sampling.SampleList;
import io.heraldprox.herald.sensor.analysis.sampling.SampledID;
import io.heraldprox.herald.sensor.analysis.sampling.Summary;
import io.heraldprox.herald.sensor.analysis.views.InRange;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

public class FowlerBasicAnalyser implements AnalysisProvider<RSSI, Distance> {
    private final TimeInterval interval;
    private final FowlerBasic basic;
    private Date lastRan = new Date(0);

    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public FowlerBasicAnalyser(final long interval, final double intercept, final double coefficient) {
        this.interval = new TimeInterval(interval);
        this.basic = new FowlerBasic(intercept, coefficient);
    }

    public FowlerBasicAnalyser() {
        this(10, -11, -0.4);
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
    public boolean analyse(Date timeNow, SampledID sampled, SampleList<RSSI> src, final SampleList<Distance> output, CallableForNewSample<Distance> callable) {
        // Interval guard
        if (lastRan.secondsSinceUnixEpoch() + interval.value >= timeNow.secondsSinceUnixEpoch()) {
            return false;
        }
        basic.reset();
        final SampleList<RSSI> values = src.filter(valid).toView();
        final Summary<RSSI> summary = values.aggregate(new Mode<RSSI>(), new Variance<RSSI>());
        final Double mode = summary.get(Mode.class);
        if (null == mode) {
            return false;
        }
        final Double var = summary.get(Variance.class);
        if (null == var) {
            return false;
        }
        final double sd = Math.sqrt(var);

        final Double distance = src.filter(valid).filter(new InRange(mode - 2 * sd, mode + 2 * sd)).aggregate(basic).get(FowlerBasic.class);
        if (null == distance) {
            return false;
        }
        final Date latestTime = values.latest();
        lastRan = latestTime;
        final Sample<Distance> newSample = new Sample<>(latestTime, new Distance(distance));
        output.push(newSample);
        callable.newSample(sampled, newSample);
        return true;
    }
}
