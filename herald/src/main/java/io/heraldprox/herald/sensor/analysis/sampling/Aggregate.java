//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface Aggregate<T extends DoubleValue> {
    int runs();

    void beginRun(final int thisRun);

    void map(@NonNull final Sample<T> value);

    @Nullable
    Double reduce();

    void reset();
}
