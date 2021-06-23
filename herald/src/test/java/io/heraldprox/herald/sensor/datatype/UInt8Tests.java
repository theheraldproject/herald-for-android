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

public class UInt8Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(0, UInt8.min.value);
        assertEquals(255, UInt8.max.value);
        // Zero
        assertEquals(0, new UInt8(0).value);
        // Clamping
        assertEquals(UInt8.min.value, new UInt8(UInt8.min.value - 1).value);
        assertEquals(UInt8.max.value, new UInt8(UInt8.max.value + 1).value);
        // String
        assertNotNull(UInt8.min.toString());
        assertNotNull(UInt8.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new UInt8(1), new UInt8(1));
        assertNotEquals(new UInt8(1), new UInt8(2));
        assertEquals(new UInt8(1).hashCode(), new UInt8(1).hashCode());
        assertNotEquals(new UInt8(1).hashCode(), new UInt8(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uint8.csv");
        out.println("value,data");
        for (int i=UInt8.min.value; i<=UInt8.max.value; i++) {
            final Data data = new Data();
            data.append(new UInt8(i));
            assertEquals(new UInt8(i), data.uint8(0));
            out.println(i + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("uint8.csv");
    }
}
