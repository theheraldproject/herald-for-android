//  Copyright 2021 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.util.Objects;

/// Unsigned integer (32 bits)
public class UInt32 implements DoubleValue {
    public final static int bitWidth = 32;
    public final static UInt32 min = new UInt32(0);
    public final static UInt32 max = new UInt32(4294967295l);
    public final long value;

    public UInt32(long value) {
        this.value = (value < 0 ? 0 : (value > 4294967295l ? 4294967295l : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UInt32 uInt32 = (UInt32) o;
        return value == uInt32.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
