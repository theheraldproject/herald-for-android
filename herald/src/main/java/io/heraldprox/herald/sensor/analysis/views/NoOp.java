//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.views;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Filter;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

public class NoOp<T extends DoubleValue> implements Filter<T> {

    public NoOp() {
    }

    @Override
    public boolean test(@NonNull final Sample<T> item) {
        return true;
    }
}
