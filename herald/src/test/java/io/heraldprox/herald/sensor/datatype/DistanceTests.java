//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class DistanceTests {

    @Test
    public void testEquals() {
        assertEquals(0, new Distance(0).value, Double.MIN_VALUE);
        assertEquals(1, new Distance(1).value, Double.MIN_VALUE);
        assertEquals(-1, new Distance(-1).value, Double.MIN_VALUE);
        assertNotEquals(new Distance(0), new Distance(1));
        assertNotEquals(new Distance(-1), new Distance(1));
    }

    @Test
    public void testHash() {
        assertEquals(new Distance(0).hashCode(), new Distance(0).hashCode());
        assertEquals(new Distance(1).hashCode(), new Distance(1).hashCode());
        assertEquals(new Distance(-1).hashCode(), new Distance(-1).hashCode());
    }

    @Test
    public void testToString() {
        assertNotNull(new RSSI(0).toString());
    }
}
