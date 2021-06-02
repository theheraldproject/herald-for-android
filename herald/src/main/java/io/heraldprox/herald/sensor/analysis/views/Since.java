//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

@SuppressWarnings("unchecked")
public class Since<T extends DoubleValue> implements Filter<T> {
    private final long afterTime;

    public Since(final long secondsSinceUnixEpoch) {
        this(new Date(secondsSinceUnixEpoch));
    }

    public Since(@NonNull final Date after) {
        this.afterTime = after.getTime();
    }

    @NonNull
    public static <T extends DoubleValue> Since<T> recent(final long inLastSeconds) {
        return new Since(new Date().secondsSinceUnixEpoch() - inLastSeconds);
    }

    @Override
    public boolean test(@NonNull final Sample<T> item) {
        return item.taken().getTime() >= afterTime;
    }
}
