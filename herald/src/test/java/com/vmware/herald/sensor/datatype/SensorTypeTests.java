//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SensorTypeTests {

    @Test
    public void testEncodeDecode() {
        for (SensorType value : SensorType.values()) {
            assertEquals(value, SensorType.valueOf(value.name()));
        }
    }
}
