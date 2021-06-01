//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class Summary<T extends DoubleValue> {
    @NonNull
    private final Aggregate<T>[] aggregates;

    public Summary(@NonNull final Aggregate<T> ... aggregates) {
        this.aggregates = aggregates;
    }

    @Nullable
    public Double get(@NonNull final Class<? extends Aggregate> byClass) {
        for (int i=0; i<aggregates.length; i++) {
            if (byClass.isInstance(aggregates[i])) {
                return aggregates[i].reduce();
            }
        }
        return null;
    }

    @Nullable
    public Double get(final int index) {
        if (index < 0 || index >= aggregates.length) {
            // Index out of bounds
            return null;
        }
        return aggregates[index].reduce();
    }
}
