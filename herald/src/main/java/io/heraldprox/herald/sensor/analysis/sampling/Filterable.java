//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface Filterable<T extends DoubleValue> {

    @NonNull
    IteratorProxy<T> filter(@NonNull final Filter<T> filter);
}
