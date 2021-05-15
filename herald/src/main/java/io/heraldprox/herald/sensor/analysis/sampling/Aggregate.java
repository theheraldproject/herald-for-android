//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface Aggregate<T extends DoubleValue> {
    int runs();

    void beginRun(int thisRun);

    void map(Sample<T> value);

    Double reduce();

    void reset();
}
