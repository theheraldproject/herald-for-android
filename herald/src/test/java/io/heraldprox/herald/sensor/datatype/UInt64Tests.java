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

public class UInt64Tests {

    @Test
    public void testRange() {
        // Constants : Note max = signed long max, this is deliberate
        assertEquals(0, UInt64.min.value);
        assertEquals(Long.MAX_VALUE, UInt64.max.value);
        // Zero
        assertEquals(0, new UInt64(0).value);
        // Clamping : Note max = signed long max, this is deliberate
        assertEquals(UInt64.min.value, new UInt64(UInt64.min.value - 1).value);
        assertEquals(UInt64.max.value, new UInt64(Long.MAX_VALUE).value);
        // String
        assertNotNull(UInt64.min.toString());
        assertNotNull(UInt64.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new UInt64(1), new UInt64(1));
        assertNotEquals(new UInt64(1), new UInt64(2));
        assertEquals(new UInt64(1).hashCode(), new UInt64(1).hashCode());
        assertNotEquals(new UInt64(1).hashCode(), new UInt64(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uint64.csv");
        out.println("value,data");
        long i = 1;
        while (i <= (UInt64.max.value / 7)) {
            final Data data = new Data();
            data.append(new UInt64(i));
            assertEquals(new UInt64(i), data.uint64(0));
            out.println(i + "," + data.base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("uint64.csv");
    }

}
