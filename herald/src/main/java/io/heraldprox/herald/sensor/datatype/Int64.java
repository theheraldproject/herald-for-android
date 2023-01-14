//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Signed integer (64 bits)
 */
public class Int64 implements DoubleValue {
    public final static int bitWidth = 64;
    public final static Int64 min = new Int64(Long.MIN_VALUE);
    public final static Int64 max = new Int64(Long.MAX_VALUE);
    public final long value;

    public Int64(final long value) {
        this.value = value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Int64 uInt64 = (Int64) o;
        return value == uInt64.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
