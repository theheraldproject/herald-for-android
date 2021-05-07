//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.algorithms.distance;

import com.vmware.herald.sensor.analysis.aggregates.Mode;
import com.vmware.herald.sensor.analysis.sampling.Aggregate;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

import java.util.HashMap;
import java.util.Map;

public class FowlerBasic<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private final Mode<T> mode = new Mode<>();
    private final double intercept;
    private final double coefficient;

    public FowlerBasic(final double intercept, final double coefficient) {
        this.intercept = intercept;
        this.coefficient = coefficient;
    }

    @Override
    public int runs() {
        return 1;
    }

    @Override
    public void beginRun(int thisRun) {
        run = thisRun;
        mode.beginRun(thisRun);
    }

    @Override
    public void map(Sample<T> value) {
        mode.map(value);
   }

    @Override
    public Double reduce() {
        final double exponent = (mode.reduce() - intercept) / coefficient;
        return Math.pow(10, exponent);
    }

    @Override
    public void reset() {
        mode.reset();
    }
}
