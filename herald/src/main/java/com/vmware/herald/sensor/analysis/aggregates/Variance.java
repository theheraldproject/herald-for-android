//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.aggregates;

import com.vmware.herald.sensor.analysis.sampling.Aggregate;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class Variance<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private long count = 0;
    private double sum = 0;
    private double mean = 0;

    @Override
    public int runs() {
        return 2;
    }

    @Override
    public void beginRun(int thisRun) {
        run = thisRun;
        if (run == 2) {
            // initialise mean
            mean = sum / count;
            // reinitialise counters
            sum = 0.0;
            count = 0;
        }
    }

    @Override
    public void map(Sample<T> value) {
        if (run == 1) {
            sum += value.value().doubleValue();
        } else {
            // run == 2
            final double dv = value.value().doubleValue();
            sum += (dv - mean) * (dv - mean);
        }
        count++;
    }

    @Override
    public double reduce() {
        if (count < 1) {
            return 0; // div by zero check
        }
        return sum / (count - 1); // Sample variance
    }

    @Override
    public void reset() {
        count = 0;
        run = 1;
        sum = 0;
        mean = 0;
    }
}
