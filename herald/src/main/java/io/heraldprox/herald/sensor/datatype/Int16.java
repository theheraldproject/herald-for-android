//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Signed integer (16 bits)
 */
public class Int16 implements DoubleValue {
    public final static int bitWidth = 16;
    public final static Int16 min = new Int16(Short.MIN_VALUE);
    public final static Int16 max = new Int16(Short.MAX_VALUE);
    public final int value;

    public Int16(final int value) {
        this.value = (value < Short.MIN_VALUE ? Short.MIN_VALUE : (value > Short.MAX_VALUE ? Short.MAX_VALUE : value));
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Int16 uInt16 = (Int16) o;
        return value == uInt16.value;
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
