//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

public interface Aggregate<T extends DoubleValue> {
    int runs();

    void beginRun(int thisRun);

    void map(Sample<T> value);

    double reduce();

    void reset();
}
