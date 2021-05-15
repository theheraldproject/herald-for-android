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

public class Int8Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(Byte.MIN_VALUE, Int8.min.value);
        assertEquals(Byte.MAX_VALUE, Int8.max.value);
        // Zero
        assertEquals(0, new Int8(0).value);
        // Clamping
        assertEquals(Int8.min.value, new Int8(Int8.min.value - 1).value);
        assertEquals(Int8.max.value, new Int8(Int8.max.value + 1).value);
        // String
        assertNotNull(Int8.min.toString());
        assertNotNull(Int8.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new Int8(1), new Int8(1));
        assertNotEquals(new Int8(1), new Int8(2));
        assertEquals(new Int8(1).hashCode(), new Int8(1).hashCode());
        assertNotEquals(new Int8(1).hashCode(), new Int8(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("int8.csv");
        out.println("value,data");
        for (int i=Int8.min.value; i<=Int8.max.value; i++) {
            final Data data = new Data();
            data.append(new Int8(i));
            assertEquals(new Int8(i), data.int8(0));
            out.println(i + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("int8.csv");
    }
}
