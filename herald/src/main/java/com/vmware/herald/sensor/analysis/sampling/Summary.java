//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.DoubleValue;

public class Summary<T extends DoubleValue> {
    private final Aggregate<T>[] aggregates;

    public Summary(Aggregate<T> ... aggregates) {
        this.aggregates = aggregates;
    }

    public Double get(final Class<? extends Aggregate> byClass) {
        for (int i=0; i<aggregates.length; i++) {
            if (byClass.isInstance(aggregates[i])) {
                return aggregates[i].reduce();
            }
        }
        return null;
    }

    public Double get(final int index) {
        if (index < 0 || index >= aggregates.length) {
            // Index out of bounds
            return null;
        }
        return aggregates[index].reduce();
    }
}
