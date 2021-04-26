//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.util.Objects;

/// Distance in metres.
public class PhysicalDistance implements DoubleValue {
    public final double value;

    public PhysicalDistance(double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhysicalDistance distance = (PhysicalDistance) o;
        return Double.compare(distance.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Distance{" +
                "value=" + value +
                '}';
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
