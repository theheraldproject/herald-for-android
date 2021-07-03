//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Unsigned integer (64 bits)
 */
public class UInt64 implements DoubleValue {
    public final static int bitWidth = 64;
    public final static UInt64 min = new UInt64(0);
    /**
     * Setting max to signed long max, rather than unsigned long max, as Java unsigned
     * long arithmetic functions are relatively immature, thus is likely to cause confusion.
     */
    public final static UInt64 max = new UInt64(Long.MAX_VALUE);
    public final long value;

    public UInt64(final long value) {
        this.value = (value < 0 ? 0 : value);
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        UInt64 uInt64 = (UInt64) o;
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
