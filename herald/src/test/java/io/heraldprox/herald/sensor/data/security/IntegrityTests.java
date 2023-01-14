//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import org.junit.Test;

import java.io.PrintWriter;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.Data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IntegrityTests {

    @Test
    public void testHash() {
        final Integrity integrity = new SHA256();
        for (int i=0; i<100; i++) {
            final Data hashA = integrity.hash(new Data((byte) i, i));
            final Data hashB = integrity.hash(new Data((byte) i, i));
            assertNotNull(hashA);
            assertNotNull(hashB);
            assertEquals(hashA, hashB);
        }
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final Integrity integrity = new SHA256();
        final PrintWriter out = TestUtil.androidPrintWriter("integrity.csv");
        out.println("key,value");
        for (int i=0; i<100; i++) {
            final Data hashA = integrity.hash(new Data((byte) i, i));
            final Data hashB = integrity.hash(new Data((byte) i, i));
            assertNotNull(hashA);
            assertNotNull(hashB);
            assertEquals(hashA, hashB);
            out.println(i+","+hashA.hexEncodedString());
        }
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("integrity.csv");
    }
}
