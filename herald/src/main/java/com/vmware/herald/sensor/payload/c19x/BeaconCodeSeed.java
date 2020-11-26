//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

import java.util.Objects;

/// Beacon code seed is derived from the day code. This is used to derive the beacon codes for the day.
public class BeaconCodeSeed {
    public long value = 0;
    public Day day = null;

    public BeaconCodeSeed(long value, Day day) {
        this.value = value;
        this.day = day;
    }

    public BeaconCodeSeed(long value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeaconCodeSeed that = (BeaconCodeSeed) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "BeaconCodeSeed{" +
                "value=" + value +
                ", day=" + day +
                '}';
    }
}
