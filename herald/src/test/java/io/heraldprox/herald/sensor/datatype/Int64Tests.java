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

public class Int64Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(Long.MIN_VALUE, Int64.min.value);
        assertEquals(Long.MAX_VALUE, Int64.max.value);
        // Zero
        assertEquals(0, new Int64(0).value);
        // Clamping
        assertEquals(Int64.min.value, new Int64(Int64.min.value).value);
        assertEquals(Int64.max.value, new Int64(Int64.max.value).value);
        // String
        assertNotNull(Int64.min.toString());
        assertNotNull(Int64.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new Int64(1), new Int64(1));
        assertNotEquals(new Int64(1), new Int64(2));
        assertEquals(new Int64(1).hashCode(), new Int64(1).hashCode());
        assertNotEquals(new Int64(1).hashCode(), new Int64(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("int64.csv");
        out.println("value,data");
        long i = 1;
        while (i <= (Int64.max.value / 7)) {
            final Data dataPositive = new Data();
            dataPositive.append(new Int64(i));
            assertEquals(new Int64(i), dataPositive.int64(0));
            out.println(i + "," + dataPositive.base64EncodedString());
            final Data dataNegative = new Data();
            dataNegative.append(new Int64(-i));
            assertEquals(new Int64(-i), dataNegative.int64(0));
            out.println(-i + "," + dataNegative.base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("int64.csv");
    }

}
