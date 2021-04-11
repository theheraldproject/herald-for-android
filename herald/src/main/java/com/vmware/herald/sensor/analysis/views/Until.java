//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.views;

import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class Until<T extends DoubleValue> implements Filter<T> {
    private final Date before;
    private final long beforeTime;

    public Until(final long secondsSinceUnixEpoch) {
        this(new Date(secondsSinceUnixEpoch));
    }

    public Until(final Date before) {
        this.before = before;
        this.beforeTime = before.getTime();
    }

    @Override
    public boolean test(Sample<T> item) {
        return item.taken().getTime() <= beforeTime;
    }
}
