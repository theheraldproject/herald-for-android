//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.DoubleValue;

public interface Filterable<T extends DoubleValue> {

    IteratorProxy<T> filter(Filter<T> filter);
}
