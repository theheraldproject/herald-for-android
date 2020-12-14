//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DataTests {

    @Test
    public void testClone() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length = 0; length < 1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final Data actual = new Data(expected);
            // Byte array data should be the same
            assertArrayEquals(expected.value, actual.value);
            // Byte array copied, so not the same instance
            assertNotSame(expected.value, actual.value);
            // Object should be equal
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testInitRepeating() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length = 0; length < 1000; length++) {
            final byte[] bytes = new byte[1];
            random.nextBytes(bytes);
            final byte repeating = bytes[0];
            final Data data = new Data(repeating, length);
            assertNotNull(data);
            assertNotNull(data.value);
            assertEquals(length, data.value.length);
            for (int j=length; j-->0;) {
                assertEquals(repeating, data.value[j]);
            }
        }
    }

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

    @Test
    public void testHexEncodeNull() {
        final Data data = new Data((byte[]) null);
        assertNotNull(data.hexEncodedString());
        assertEquals("", data.hexEncodedString());
    }

    @Test
    public void testDescription() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final String base64String = expected.base64EncodedString();
            assertNotNull(base64String);
            if (length > 0) {
                assertTrue(!base64String.isEmpty());
            }
            assertEquals(base64String, expected.description());
        }
    }

    @Test
    public void testSubdataOffset() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length=0; length<100; length++) {
            final byte[] expected = new byte[length];
            random.nextBytes(expected);
            final Data data = new Data(expected);
            // Invalid offsets
            assertNull(data.subdata(-1));
            assertNull(data.subdata(length));
            // Check data of valid offsets
            for (int offset=length; offset-->0;) {
                final Data subdata = data.subdata(offset);
                assertNotNull(subdata);
                assertNotNull(subdata.value);
                // Check subdata is the same as fragment from original
                assertEquals(length - offset, subdata.value.length);
                for (int j=offset,k=0; k<(length-offset); j++, k++) {
                    assertEquals(expected[j], subdata.value[k]);
                }
            }
        }
    }


    @Test
    public void testSubdataOffsetLength() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length=0; length<100; length++) {
            final byte[] expected = new byte[length];
            random.nextBytes(expected);
            final Data data = new Data(expected);
            // Invalid offsets
            assertNull(data.subdata(-1, length));
            assertNull(data.subdata(length, 0));
            // Equivalent to copy
            if (length == 0) {
                // Offset 0 is invalid when length is 0
                assertNull(data.subdata(0, length));
            } else {
                assertNotNull(data.subdata(0, length));
                assertNotNull(data.subdata(0, length).value);
                assertArrayEquals(expected, data.subdata(0, length).value);
            }
            // Check data of valid offsets
            for (int offset=length; offset-->0;) {
                for (int len=length-offset; len-->0;) {
                    final Data subdata = data.subdata(offset, len);
                    assertNotNull(subdata);
                    assertNotNull(subdata.value);
                    // Check subdata is the same as fragment from original
                    assertEquals(len, subdata.value.length);
                    for (int j = offset, k = 0; k < len; j++, k++) {
                        assertEquals(expected[j], subdata.value[k]);
                    }
                }
            }
        }
    }

    @Test
    public void testAppend() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        final Data data = new Data();
        int totalLength = 0;
        byte lastByte = 0;
        for (int length=0; length<100; length++) {
            final byte[] fragment = new byte[length];
            random.nextBytes(fragment);
            data.append(new Data(fragment));
            totalLength += length;
            assertNotNull(data.value);
            assertEquals(totalLength, data.value.length);
            for (int i=length, j=totalLength-1; i-->0;) {
                assertEquals(fragment[i], data.value[j]);
                j--;
            }
            // Check last byte of last fragment to ensure previous fragments haven't been lost
            if (data.value.length > length) {
                assertEquals(lastByte, data.value[totalLength - length - 1]);
            }
            if (data.value.length > 0) {
                lastByte = data.value[totalLength - 1];
            }
        }
    }

    @Test
    public void testToString() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final String hexString = expected.hexEncodedString();
            assertNotNull(hexString);
            if (length > 0) {
                assertTrue(!hexString.isEmpty());
            }
            assertEquals(hexString, expected.toString());
        }
    }

    @Test
    public void testHash() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int length=0; length<100; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final Data expected = new Data(data);
            final Data actual = new Data(expected);
            final Data different = new Data((byte) random.nextInt(), length);
            assertEquals(expected.hashCode(), actual.hashCode());
            if (length > 0) {
                assertNotEquals(expected.hashCode(), different.hashCode());
            }
        }
    }
}

