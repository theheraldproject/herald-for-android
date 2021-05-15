//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import io.heraldprox.herald.sensor.TestUtil;

import org.junit.Test;

import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class UInt16Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(0, UInt16.min.value);
        assertEquals(65535, UInt16.max.value);
        // Zero
        assertEquals(0, new UInt16(0).value);
        // Clamping
        assertEquals(UInt16.min.value, new UInt16(UInt16.min.value - 1).value);
        assertEquals(UInt16.max.value, new UInt16(UInt16.max.value + 1).value);
        // String
        assertNotNull(UInt16.min.toString());
        assertNotNull(UInt16.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new UInt16(1), new UInt16(1));
        assertNotEquals(new UInt16(1), new UInt16(2));
        assertEquals(new UInt16(1).hashCode(), new UInt16(1).hashCode());
        assertNotEquals(new UInt16(1).hashCode(), new UInt16(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uint16.csv");
        out.println("value,data");
        for (int i=UInt16.min.value; i<=UInt16.max.value; i++) {
            final Data data = new Data();
            data.append(new UInt16(i));
            assertEquals(new UInt16(i), data.uint16(0));
            out.println(i + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("uint16.csv");
    }
}
