//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import com.vmware.herald.sensor.analysis.Sample;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PseudoDeviceAddressTests {

    @Test
    public void testRandom() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            int repeated = 0;
            long lastAddress = 0;
            byte[] lastBytes = new byte[6];
            for (int i = 0; i < 1000; i++) {
                final PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress(method);
                // Address should be different every time
                assertNotEquals(lastAddress, pseudoDeviceAddress.address);
                // Bytes should be different most of the time
                assertEquals(6, pseudoDeviceAddress.data.length);
                for (int j=0; j<6; j++) {
                    if (pseudoDeviceAddress.data[j] == lastBytes[j]) {
                        repeated++;
                    }
                }
                lastAddress = pseudoDeviceAddress.address;
                lastBytes = pseudoDeviceAddress.data;
            }
            // Tolerate 10% repeats for individual bytes
            assertTrue(repeated < (1000 * 6 / 10));
        }
    }

    @Test
    public void testEncodeDecode() {
        // Test encoding and decoding to ensure same data means same address
        for (int i=0; i<1000; i++) {
            final PseudoDeviceAddress expected = new PseudoDeviceAddress();
            final PseudoDeviceAddress actual = new PseudoDeviceAddress(expected.data);
            assertEquals(expected.address, actual.address);
            assertEquals(expected, actual);
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(expected.toString(), actual.toString());
        }
    }

    @Test
    public void testVisualCheck() {
        // Visual check for randomness and byte fill
        for (int i=0; i<10; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress();
            System.err.println(Arrays.toString(address.data));
        }
    }

    @Test
    public void testPerformance() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final Sample sample = new Sample();
            long t0, t1;
            for (int i = 100000; i-- > 0; ) {
                t0 = System.nanoTime();
                new PseudoDeviceAddress(method);
                t1 = System.nanoTime();
                sample.add(t1 - t0);
            }
            System.err.println(method.name() + " : " + sample);
        }
    }
}
