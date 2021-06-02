//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class Gaussian<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    @NonNull
    private io.heraldprox.herald.sensor.analysis.Sample model = new io.heraldprox.herald.sensor.analysis.Sample();

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
        model.add(value.value().doubleValue());
    }

    @Nullable
    @Override
    public Double reduce() {
        return model.mean();
    }

    @Override
    public void reset() {
        model = new io.heraldprox.herald.sensor.analysis.Sample();
    }

    @NonNull
    public io.heraldprox.herald.sensor.analysis.Sample model() {
        return model;
    }
}
