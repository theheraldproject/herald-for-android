//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.views;

import com.vmware.herald.sensor.analysis.sampling.Filter;
import com.vmware.herald.sensor.analysis.sampling.Sample;
import com.vmware.herald.sensor.datatype.DoubleValue;

public class NoOp<T extends DoubleValue> implements Filter<T> {

    public NoOp() {
    }

    @Override
    public boolean test(Sample<T> item) {
        return true;
    }
}
