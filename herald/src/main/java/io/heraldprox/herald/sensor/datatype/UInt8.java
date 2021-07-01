//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Unsigned integer (8 bits)
 */
public class UInt8 implements DoubleValue {
    public final static int bitWidth = 8;
    public final static UInt8 min = new UInt8(0);
    public final static UInt8 max = new UInt8(255);
    public final int value;

    public UInt8(final int value) {
        this.value = (value < 0 ? 0 : Math.min(value, 255));
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        UInt8 uInt8 = (UInt8) o;
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
