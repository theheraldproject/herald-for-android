//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SensorStateTests {

    @Test
    public void testEncodeDecode() {
        for (SensorState value : SensorState.values()) {
            assertEquals(value, SensorState.valueOf(value.name()));
        }
    }
}
