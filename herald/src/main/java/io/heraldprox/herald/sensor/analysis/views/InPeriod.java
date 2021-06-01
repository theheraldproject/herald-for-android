//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class InPeriod<T extends DoubleValue> implements Filter<T> {
    @NonNull
    private final Date after;
    private final long afterTime;
    @NonNull
    private final Date before;
    private final long beforeTime;

    public InPeriod(final long afterSecondsSinceUnixEpoch, final long beforeSecondsSinceUnixEpoch) {
        this(new Date(afterSecondsSinceUnixEpoch), new Date(beforeSecondsSinceUnixEpoch));
    }

    public InPeriod(@NonNull final Date after, @NonNull final Date before) {
        this.after = after;
        this.afterTime = after.getTime();
        this.before = before;
        this.beforeTime = before.getTime();
    }

    @Override
    public boolean test(@NonNull Sample<T> item) {
        final long takenTime = item.taken().getTime();
        return afterTime <= takenTime && takenTime <= beforeTime;
    }
}
