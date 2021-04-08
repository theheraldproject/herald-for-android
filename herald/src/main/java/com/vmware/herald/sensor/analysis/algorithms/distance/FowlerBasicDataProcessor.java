//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.algorithms.distance;

import com.vmware.herald.sensor.analysis.aggregates.Mode;
import com.vmware.herald.sensor.analysis.aggregates.Variance;
import com.vmware.herald.sensor.analysis.pipeline.ConcreteDataProcessor;
import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.analysis.sampling.SampleList;
import com.vmware.herald.sensor.analysis.sampling.SampledID;
import com.vmware.herald.sensor.analysis.sampling.Summary;
import com.vmware.herald.sensor.analysis.views.InRange;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.Distance;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.TimeInterval;

public class FowlerBasicDataProcessor extends ConcreteDataProcessor<RSSI, Distance> {
    private final TimeInterval interval;
    private final FowlerBasic basic;
    private Date lastRan = new Date(0);
    private final Filter<RSSI> valid = new InRange<>(-99, -10);

    public FowlerBasicDataProcessor(final long interval, final double intercept, final double coefficient) {
        super(RSSI.class, 100);
        this.interval = new TimeInterval(interval);
        this.basic = new FowlerBasic(intercept, coefficient);
    }

    public FowlerBasicDataProcessor() {
        this(10, -11, -0.4);
    }

    @Override
    public void process(SampledID sampled, SampleList<RSSI> src) {
        // Interval guard
        if (lastRan.secondsSinceUnixEpoch() + interval.value >= src.latest().secondsSinceUnixEpoch()) {
            return;
        }
        basic.reset();
        final SampleList<RSSI> values = src.filter(valid).toView();
        final Summary<RSSI> summary = values.aggregate(new Mode<RSSI>(), new Variance<RSSI>());
        final double mode = summary.get(Mode.class);
        final double var = summary.get(Variance.class);
        final double sd = Math.sqrt(var);

        final double distance = src.filter(valid).filter(new InRange(mode - 2 * sd, mode + 2 * sd)).aggregate(basic).get(FowlerBasic.class);
        final Date latestTime = values.latest();
        lastRan = latestTime;
        final Sample<Distance> newSample = new Sample<>(latestTime, new Distance(distance));
        publish(sampled, newSample);
    }
}
