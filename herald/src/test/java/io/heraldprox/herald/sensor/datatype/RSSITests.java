//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class RSSITests {

    @Test
    public void testEquals() {
        assertEquals(0, new RSSI(0).value, Double.MIN_VALUE);
        assertEquals(1, new RSSI(1).value, Double.MIN_VALUE);
        assertEquals(-1, new RSSI(-1).value, Double.MIN_VALUE);
        assertNotEquals(new RSSI(0), new RSSI(1));
        assertNotEquals(new RSSI(-1), new RSSI(1));
    }

    @Test
    public void testHash() {
        assertEquals(new RSSI(0).hashCode(), new RSSI(0).hashCode());
        assertEquals(new RSSI(1).hashCode(), new RSSI(1).hashCode());
        assertEquals(new RSSI(-1).hashCode(), new RSSI(-1).hashCode());
    }

    @Test
    public void testToString() {
        assertNotNull(new RSSI(0).toString());
    }
}
