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

public class Int16Tests {

    @Test
    public void testRange() {
        // Constants
        assertEquals(Short.MIN_VALUE, Int16.min.value);
        assertEquals(Short.MAX_VALUE, Int16.max.value);
        // Zero
        assertEquals(0, new Int16(0).value);
        // Clamping
        assertEquals(Int16.min.value, new Int16(Int16.min.value - 1).value);
        assertEquals(Int16.max.value, new Int16(Int16.max.value + 1).value);
        // String
        assertNotNull(Int16.min.toString());
        assertNotNull(Int16.max.toString());
    }

    @Test
    public void testCompare() {
        assertEquals(new Int16(1), new Int16(1));
        assertNotEquals(new Int16(1), new Int16(2));
        assertEquals(new Int16(1).hashCode(), new Int16(1).hashCode());
        assertNotEquals(new Int16(1).hashCode(), new Int16(2).hashCode());
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("int16.csv");
        out.println("value,data");
        for (int i=Int16.min.value; i<=Int16.max.value; i++) {
            final Data data = new Data();
            data.append(new Int16(i));
            assertEquals(new Int16(i), data.int16(0));
            out.println(i + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("int16.csv");
    }
}
