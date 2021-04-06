//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.aggregates;

import com.vmware.herald.sensor.analysis.sampling.Aggregate;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class Mean<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private long count = 0;
    private double sum = 0;

    @Override
    public int runs() {
        return 1;
    }

    @Override
    public void beginRun(int thisRun) {
        run = thisRun;
    }

    @Override
    public void map(Sample<T> value) {
        if (run > 1) return;
        sum += value.value().doubleValue();
        count++;
    }

    @Override
    public double reduce() {
        return sum / count;
    }

    @Override
    public void reset() {
        count = 0;
        sum = 0;
    }
}
