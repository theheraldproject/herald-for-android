//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import android.bluetooth.BluetoothDevice;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class TargetIdentifierTests {

    @Test
    public void testInit() {
        final TargetIdentifier a1 = new TargetIdentifier("A");
        final TargetIdentifier a2 = new TargetIdentifier("A");
        final TargetIdentifier b = new TargetIdentifier("B");

        assertEquals(a1, a2);
        assertNotEquals(a1, b);
        assertNotEquals(a2, b);

        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1.hashCode(), b.hashCode());
        assertNotEquals(a2.hashCode(), b.hashCode());

        assertEquals("A", a1.toString());
        assertEquals("A", a2.toString());
        assertEquals("B", b.toString());
    }

    @Test
    public void testRandom() {
        String last = null;
        for (int i=100; i-->0;) {
            final TargetIdentifier targetIdentifier = new TargetIdentifier();
            assertNotEquals(targetIdentifier.value, last);
            last = targetIdentifier.value;
        }
    }
}
