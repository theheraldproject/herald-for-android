//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import com.vmware.herald.sensor.analysis.Sample;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PseudoDeviceAddressTests {

    @Test
    public void testSecureRandom() {
        // Address should be different every time
        long last = 0;
        for (int i=0; i<1000; i++) {
            final SecureRandom secureRandom = PseudoDeviceAddress.getSecureRandom();
            final long value = secureRandom.nextLong();
            assertNotEquals(last, value);
            last = value;
        }
    }

    @Test
    public void testRandom() {
        // Address should be different every time
        long last = 0;
        for (int i=0; i<1000; i++) {
            final long value = PseudoDeviceAddress.getRandomLong();
            assertNotEquals(last, value);
            last = value;
        }
    }

    @Test
    public void testEncodeDecode() {
        // Test encoding and decoding to ensure same data means same address
        for (int i=0; i<1000; i++) {
            final PseudoDeviceAddress expected = new PseudoDeviceAddress();
            final PseudoDeviceAddress actual = new PseudoDeviceAddress(expected.data);
            assertEquals(expected.address, actual.address);
        }
    }

    /// This test may fail sometimes, as not every byte will rotate all the time
    @Test
    public void testRandomBytes() {
        // Every byte should rotate (most of the time)
        int repeated = 0;
        byte[] last = new byte[6];
        for (int i=0; i<1000; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress(false);
            assertEquals(6, address.data.length);
            for (int j=0; j<6; j++) {
                if (address.data[j] == last[j]) {
                    repeated++;
                }
            }
            last = address.data;
        }
        // Tolerate a few repeats
        System.err.println(repeated);
        assertTrue(repeated < 30);
    }

    /// This test may fail sometimes, as not every byte will rotate all the time
    @Test
    public void testSecureRandomBytes() {
        // Every byte should rotate (most of the time)
        int repeated = 0;
        byte[] last = new byte[6];
        for (int i=0; i<1000; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress(true);
            assertEquals(6, address.data.length);
            for (int j=0; j<6; j++) {
                if (address.data[j] == last[j]) {
                    repeated++;
                }
            }
            last = address.data;
        }
        // Tolerate a couple of repeats
        assertTrue(repeated < 30);
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
    public void testPerformanceOfRandom() {
        final Sample sample = new Sample();
        long t0, t1;
        for (int i=100000; i-->0;) {
            t0 = System.nanoTime();
            Math.random();
            t1 = System.nanoTime();
            sample.add(t1 - t0);
        }
        System.err.println(sample);
    }

    @Test
    public void testPerformanceOfSecureRandom() {
        final Sample sample = new Sample();
        long t0, t1;
        for (int i=100000; i-->0;) {
            t0 = System.nanoTime();
            PseudoDeviceAddress.getSecureRandom().nextLong();
            t1 = System.nanoTime();
            sample.add(t1 - t0);
        }
        System.err.println(sample);
    }
}
