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

public class Int32Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(Integer.MIN_VALUE, Int32.min.value);
        assertEquals(Integer.MAX_VALUE, Int32.max.value);
        // Zero
        assertEquals(0, new Int32(0).value);
        // Clamping
        assertEquals(Int32.min.value, new Int32((long) Int32.min.value - 1).value);
        assertEquals(Int32.max.value, new Int32((long) Int32.max.value + 1).value);
        // String
        assertNotNull(Int32.min.toString());
        assertNotNull(Int32.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new Int32(1), new Int32(1));
        assertNotEquals(new Int32(1), new Int32(2));
        assertEquals(new Int32(1).hashCode(), new Int32(1).hashCode());
        assertNotEquals(new Int32(1).hashCode(), new Int32(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("int32.csv");
        out.println("value,data");
        int i = 1;
        while (i <= (Int32.max.value / 7)) {
            final Data dataPositive = new Data();
            dataPositive.append(new Int32(i));
            assertEquals(new Int32(i), dataPositive.int32(0));
            out.println(i + "," + dataPositive.base64EncodedString());
            final Data dataNegative = new Data();
            dataNegative.append(new Int32(-i));
            assertEquals(new Int32(-i), dataNegative.int32(0));
            out.println(-i + "," + dataNegative.base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("int32.csv");
    }

}
