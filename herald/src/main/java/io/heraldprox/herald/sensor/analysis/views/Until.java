//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class Until<T extends DoubleValue> implements Filter<T> {
    private final long beforeTime;

    public Until(final long secondsSinceUnixEpoch) {
        this(new Date(secondsSinceUnixEpoch));
    }

    public Until(@NonNull final Date before) {
        this.beforeTime = before.getTime();
    }

    @Override
    public boolean test(@NonNull final Sample<T> item) {
        return item.taken().getTime() <= beforeTime;
    }
}
