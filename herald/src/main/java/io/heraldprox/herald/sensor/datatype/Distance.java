//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Distance in metres.
 */
public class Distance implements DoubleValue {
    public final double value;

    public Distance(final double value) {
        this.value = value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Distance distance = (Distance) o;
        return 0 == Double.compare(distance.value, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Distance{" +
                "value=" + value +
                '}';
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
