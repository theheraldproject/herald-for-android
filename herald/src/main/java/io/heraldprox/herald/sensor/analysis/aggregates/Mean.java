//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class Mean<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private long count = 0;
    private double sum = 0;

    @Override
    public int runs() {
        return 1;
    }

    @Override
    public void beginRun(final int thisRun) {
        run = thisRun;
    }

    @Override
    public void map(@NonNull final Sample<T> value) {
        if (run > 1) return;
        sum += value.value().doubleValue();
        count++;
    }

    @Nullable
    @Override
    public Double reduce() {
        if (0 == count) {
            return null;
        }
        return sum / count;
    }

    @Override
    public void reset() {
        count = 0;
        sum = 0;
    }
}
