//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import io.heraldprox.herald.sensor.TestUtil;

import org.junit.Test;

import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class UInt32Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(0, UInt32.min.value);
        assertEquals(4294967295L, UInt32.max.value);
        // Zero
        assertEquals(0, new UInt32(0).value);
        // Clamping
        assertEquals(UInt32.min.value, new UInt32(UInt32.min.value - 1).value);
        assertEquals(UInt32.max.value, new UInt32(UInt32.max.value + 1).value);
        // String
        assertNotNull(UInt32.min.toString());
        assertNotNull(UInt32.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new UInt32(1), new UInt32(1));
        assertNotEquals(new UInt32(1), new UInt32(2));
        assertEquals(new UInt32(1).hashCode(), new UInt32(1).hashCode());
        assertNotEquals(new UInt32(1).hashCode(), new UInt32(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uint32.csv");
        out.println("value,data");
        long i = 1;
        while (i <= (UInt32.max.value / 7)) {
            final Data data = new Data();
            data.append(new UInt32(i));
            assertEquals(new UInt32(i), data.uint32(0));
            out.println(i + "," + data.base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("uint32.csv");
    }

}
