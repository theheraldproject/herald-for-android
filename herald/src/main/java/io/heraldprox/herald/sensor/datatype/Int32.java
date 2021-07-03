//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Signed integer (32 bits)
 */
public class Int32 implements DoubleValue {
    public final static int bitWidth = 32;
    public final static Int32 min = new Int32(Integer.MIN_VALUE);
    public final static Int32 max = new Int32(Integer.MAX_VALUE);
    public final int value;

    public Int32(final long value) {
        this.value = (int) (value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value));
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Int32 uInt32 = (Int32) o;
        return value == uInt32.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
