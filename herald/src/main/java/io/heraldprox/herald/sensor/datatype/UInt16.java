//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import java.util.Objects;

/// Unsigned integer (16 bits)
public class UInt16 implements DoubleValue {
    public final static int bitWidth = 16;
    public final static UInt16 min = new UInt16(0);
    public final static UInt16 max = new UInt16(65535);
    public final int value;

    public UInt16(int value) {
        this.value = (value < 0 ? 0 : (value > 65535 ? 65535 : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        UInt16 uInt16 = (UInt16) o;
        return value == uInt16.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
