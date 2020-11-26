//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

/// Day is the number of whole days since epoch (2020-01-01 00:00:00)
public class Day {
    public int value = 0;

    public Day(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Day{" +
                "value=" + value +
                '}';
    }
}
