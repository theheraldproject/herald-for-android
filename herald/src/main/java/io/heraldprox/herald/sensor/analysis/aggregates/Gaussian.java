//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Distribution;
import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

/**
 * Aggregate wrapper for {@link Distribution} to provide a one-pass Gaussian model for estimating
 * mean, variance, standard deviation, min and max for sample values.
 * @param <T>
 */
public class Gaussian<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    @NonNull
    private Distribution model = new Distribution();

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

    /**
     * Sample mean.
     * @return Mean, or null if no sample has been observed.
     */
    @Nullable
    @Override
    public Double reduce() {
        return model.mean();
    }

    @Override
    public void reset() {
        model = new Distribution();
    }

    /**
     * Get distribution model to obtain mean, variance, standard deviation, count, min and max for
     * sample values.
     * @return Distribution model.
     */
    @NonNull
    public Distribution model() {
        return model;
    }
}
