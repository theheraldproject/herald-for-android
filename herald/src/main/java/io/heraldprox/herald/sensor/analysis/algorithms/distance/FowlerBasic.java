//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.distance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.analysis.aggregates.Mode;
import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class FowlerBasic<T extends DoubleValue> implements Aggregate<T> {
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
    public void beginRun(final int thisRun) {
        mode.beginRun(thisRun);
    }

    @Override
    public void map(@NonNull final Sample<T> value) {
        mode.map(value);
   }

    @Nullable
    @Override
    public Double reduce() {
        if (0 == coefficient) {
            return null;
        }
        final Double modeValue = mode.reduce();
        if (null == modeValue) {
            return null;
        }
        final double exponent = (modeValue - intercept) / coefficient;
        return Math.pow(10, exponent);
    }

    @Override
    public void reset() {
        mode.reset();
    }
}
