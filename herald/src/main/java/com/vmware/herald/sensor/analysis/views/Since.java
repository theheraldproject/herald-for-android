//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.views;

import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.Date;
import com.vmware.herald.sensor.datatype.DoubleValue;

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

    @Override
    public boolean test(Sample<T> item) {
        return item.taken().getTime() >= afterTime;
    }
}
