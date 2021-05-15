//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class Since<T extends DoubleValue> implements Filter<T> {
    private final Date after;
    private final long afterTime;

    public Since(final long secondsSinceUnixEpoch) {
        this(new Date(secondsSinceUnixEpoch));
    }

    public Since(final Date after) {
        this.after = after;
        this.afterTime = after.getTime();
    }

    public final static <T extends DoubleValue> Since<T> recent(final long inLastSeconds) {
        return new Since(new Date().secondsSinceUnixEpoch() - inLastSeconds);
    }

    @Override
    public boolean test(Sample<T> item) {
        return item.taken().getTime() >= afterTime;
    }
}
