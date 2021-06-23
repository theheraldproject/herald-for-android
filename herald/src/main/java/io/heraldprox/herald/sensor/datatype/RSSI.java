//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * RSSI in locally measured units.
 *
 * Value is typically -1 to -99 on most systems, with 0 indicating RSSI is not ready,
 * and -100 indicating an out of range or error condition.
 *
 * This logic varies by source system though. Some (non Android) systems use 0 to -128.
 */
public class RSSI implements DoubleValue {
    public final double value;

    public RSSI(final double value) {
        this.value = value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        RSSI rssi = (RSSI) o;
        return 0 == Double.compare(rssi.value, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "RSSI{" +
                "value=" + value +
                '}';
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
