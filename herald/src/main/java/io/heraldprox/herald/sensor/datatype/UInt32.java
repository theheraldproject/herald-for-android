//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Unsigned integer (32 bits)
 */
public class UInt32 implements DoubleValue {
    public final static int bitWidth = 32;
    public final static UInt32 min = new UInt32(0);
    public final static UInt32 max = new UInt32(4294967295L);
    public final long value;

    public UInt32(final long value) {
        this.value = (value < 0 ? 0 : Math.min(value, 4294967295L));
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        UInt32 uInt32 = (UInt32) o;
        return value == uInt32.value;
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
