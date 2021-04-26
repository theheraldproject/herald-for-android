//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class PhysicalDistanceTests {

    @Test
    public void testEquals() {
        assertEquals(0, new PhysicalDistance(0).value, Double.MIN_VALUE);
        assertEquals(1, new PhysicalDistance(1).value, Double.MIN_VALUE);
        assertEquals(-1, new PhysicalDistance(-1).value, Double.MIN_VALUE);
        assertNotEquals(new PhysicalDistance(0), new PhysicalDistance(1));
        assertNotEquals(new PhysicalDistance(-1), new PhysicalDistance(1));
    }

    @Test
    public void testHash() {
        assertEquals(new PhysicalDistance(0).hashCode(), new PhysicalDistance(0).hashCode());
        assertEquals(new PhysicalDistance(1).hashCode(), new PhysicalDistance(1).hashCode());
        assertEquals(new PhysicalDistance(-1).hashCode(), new PhysicalDistance(-1).hashCode());
    }

    @Test
    public void testToString() {
        assertNotNull(new RSSI(0).toString());
    }
}
