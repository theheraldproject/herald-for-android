//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Signed integer (8 bits)
 */
public class Int8 implements DoubleValue {
    public final static int bitWidth = 8;
    public final static Int8 min = new Int8(Byte.MIN_VALUE);
    public final static Int8 max = new Int8(Byte.MAX_VALUE);
    public final int value;

    public Int8(final int value) {
        this.value = (value < Byte.MIN_VALUE ? Byte.MIN_VALUE : (value > Byte.MAX_VALUE ? Byte.MAX_VALUE : value));
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Int8 uInt8 = (Int8) o;
        return value == uInt8.value;
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
