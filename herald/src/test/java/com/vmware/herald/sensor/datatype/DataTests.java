//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataTests {

    @Test
    public void testBase64EncodeDecodeEmpty() {
        final Data expected = new Data();
        final Data actual = new Data(expected.base64EncodedString());
        assertNotNull(actual.value);
        assertArrayEquals(expected.value, actual.value);
    }

    @Test
    public void testBase64EncodeDecode() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        // Ensure data of all lengths are encoded and decoded correctly
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final String base64String = expected.base64EncodedString();
            assertNotNull(base64String);
            if (length > 0) {
                assertTrue(!base64String.isEmpty());
            }
            final Data actual = new Data(base64String);
            assertArrayEquals(expected.value, actual.value);
        }
    }

    @Test
    public void testHexEncodeDecode() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        // Ensure data of all lengths are encoded and decoded correctly
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final String hexString = expected.hexEncodedString();
            assertNotNull(hexString);
            if (length > 0) {
                assertTrue(!hexString.isEmpty());
            }
            final Data actual = Data.fromHexEncodedString(hexString);
            assertArrayEquals(expected.value, actual.value);
        }
    }
}
