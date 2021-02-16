//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BluetoothStateTests {

    @Test
    public void testEncodeDecode() {
        for (BluetoothState value : BluetoothState.values()) {
            assertEquals(value, BluetoothState.valueOf(value.name()));
        }
    }
}
