//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.aggregates;

import com.vmware.herald.sensor.analysis.sampling.Aggregate;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class Gaussian<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private com.vmware.herald.sensor.analysis.Sample model = new com.vmware.herald.sensor.analysis.Sample();

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
        model.add(value.value().doubleValue());
    }

    @Override
    public double reduce() {
        return model.mean();
    }

    @Override
    public void reset() {
        model = new com.vmware.herald.sensor.analysis.Sample();
    }

    public com.vmware.herald.sensor.analysis.Sample model() {
        return model;
    }
}
