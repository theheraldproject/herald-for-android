//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class GreaterThan<T extends DoubleValue> implements Filter<T> {
    private final double min;

    public GreaterThan(final double min) {
        this.min = min;
    }

    @Override
    public boolean test(@NonNull final Sample<T> item) {
        return item.value().doubleValue() > min;
    }
}
