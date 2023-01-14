//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import io.heraldprox.herald.sensor.TestUtil;

import org.junit.Test;

import java.io.PrintWriter;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
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
                assertFalse(base64String.isEmpty());
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
                assertFalse(hexString.isEmpty());
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
                assertFalse(base64String.isEmpty());
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
            if (0 == length) {
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
                assertFalse(hexString.isEmpty());
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

    // MARK:- Tests for Int/UInt append methods are found with the Int/UInt tests

    @Test
    public void testData() throws Exception {
        // Zero
        final Data dataRange = new Data();
        dataRange.append(new Data(), Data.DataLengthEncodingOption.UINT8);
        assertNotNull(dataRange.data(0, Data.DataLengthEncodingOption.UINT8));
        assertEquals(new Data(), dataRange.data(0, Data.DataLengthEncodingOption.UINT8).value);
        assertEquals(1, dataRange.data(0, Data.DataLengthEncodingOption.UINT8).start);
        assertEquals(1, dataRange.data(0, Data.DataLengthEncodingOption.UINT8).end);

        // Encoding options
        final Data dataEncoding = new Data();
        dataEncoding.append(new Data((byte) 1, 1), Data.DataLengthEncodingOption.UINT8);
        dataEncoding.append(new Data((byte) 2, 2), Data.DataLengthEncodingOption.UINT16);
        dataEncoding.append(new Data((byte) 3, 3), Data.DataLengthEncodingOption.UINT32);
        dataEncoding.append(new Data((byte) 4, 4), Data.DataLengthEncodingOption.UINT64);
        assertNotNull(dataEncoding.data(0, Data.DataLengthEncodingOption.UINT8));
        assertEquals(new Data((byte) 1, 1), dataEncoding.data(0, Data.DataLengthEncodingOption.UINT8).value);
        assertEquals(1, dataEncoding.data(0, Data.DataLengthEncodingOption.UINT8).start);
        assertEquals(2, dataEncoding.data(0, Data.DataLengthEncodingOption.UINT8).end);
        assertNotNull(dataEncoding.data(2, Data.DataLengthEncodingOption.UINT16));
        assertEquals(new Data((byte) 2, 2), dataEncoding.data(2, Data.DataLengthEncodingOption.UINT16).value);
        assertEquals(4, dataEncoding.data(2, Data.DataLengthEncodingOption.UINT16).start);
        assertEquals(6, dataEncoding.data(2, Data.DataLengthEncodingOption.UINT16).end);
        assertNotNull(dataEncoding.data(6, Data.DataLengthEncodingOption.UINT32));
        assertEquals(new Data((byte) 3, 3), dataEncoding.data(6, Data.DataLengthEncodingOption.UINT32).value);
        assertEquals(10, dataEncoding.data(6, Data.DataLengthEncodingOption.UINT32).start);
        assertEquals(13, dataEncoding.data(6, Data.DataLengthEncodingOption.UINT32).end);
        assertNotNull(dataEncoding.data(13, Data.DataLengthEncodingOption.UINT64));
        assertEquals(new Data((byte) 4, 4), dataEncoding.data(13, Data.DataLengthEncodingOption.UINT64).value);
        assertEquals(21, dataEncoding.data(13, Data.DataLengthEncodingOption.UINT64).start);
        assertEquals(25, dataEncoding.data(13, Data.DataLengthEncodingOption.UINT64).end);

        // Values in range
        final PrintWriter out = TestUtil.androidPrintWriter("data.csv");
        out.println("value,data");
        for (int i=0; i<=5; i++) {
            final Data data = new Data();
            data.append(new Data((byte) i, i), Data.DataLengthEncodingOption.UINT8);
            assertNotNull(data.data(0, Data.DataLengthEncodingOption.UINT8));
            assertEquals(new Data((byte) i, i), data.data(0, Data.DataLengthEncodingOption.UINT8).value);
            out.println(i + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("data.csv");
    }

    @Test
    public void testString() throws Exception {
        // Zero
        final Data dataRange = new Data();
        dataRange.append("");
        assertNotNull(dataRange.string(0));
        assertEquals("", dataRange.string(0).value);
        assertEquals(1, dataRange.string(0).start);
        assertEquals(1, dataRange.string(0).end);

        // Encoding options
        final Data dataEncoding = new Data();
        dataEncoding.append("a", Data.DataLengthEncodingOption.UINT8);
        dataEncoding.append("bb", Data.DataLengthEncodingOption.UINT16);
        dataEncoding.append("ccc", Data.DataLengthEncodingOption.UINT32);
        dataEncoding.append("dddd", Data.DataLengthEncodingOption.UINT64);
        assertNotNull(dataEncoding.string(0, Data.DataLengthEncodingOption.UINT8));
        assertEquals("a", dataEncoding.string(0, Data.DataLengthEncodingOption.UINT8).value);
        assertEquals(1, dataEncoding.string(0, Data.DataLengthEncodingOption.UINT8).start);
        assertEquals(2, dataEncoding.string(0, Data.DataLengthEncodingOption.UINT8).end);
        assertNotNull(dataEncoding.string(2, Data.DataLengthEncodingOption.UINT16));
        assertEquals("bb", dataEncoding.string(2, Data.DataLengthEncodingOption.UINT16).value);
        assertEquals(4, dataEncoding.string(2, Data.DataLengthEncodingOption.UINT16).start);
        assertEquals(6, dataEncoding.string(2, Data.DataLengthEncodingOption.UINT16).end);
        assertNotNull(dataEncoding.string(6, Data.DataLengthEncodingOption.UINT32));
        assertEquals("ccc", dataEncoding.string(6, Data.DataLengthEncodingOption.UINT32).value);
        assertEquals(10, dataEncoding.string(6, Data.DataLengthEncodingOption.UINT32).start);
        assertEquals(13, dataEncoding.string(6, Data.DataLengthEncodingOption.UINT32).end);
        assertNotNull(dataEncoding.string(13, Data.DataLengthEncodingOption.UINT64));
        assertEquals("dddd", dataEncoding.string(13, Data.DataLengthEncodingOption.UINT64).value);
        assertEquals(21, dataEncoding.string(13, Data.DataLengthEncodingOption.UINT64).start);
        assertEquals(25, dataEncoding.string(13, Data.DataLengthEncodingOption.UINT64).end);

        // Values in range
        final PrintWriter out = TestUtil.androidPrintWriter("string.csv");
        out.println("value,data");
        for (final String s : new String[]{"","a","bb","ccc","dddd","eeeee"}) {
            final Data data = new Data();
            data.append(s);
            assertNotNull(data.string(0));
            assertEquals(s, data.string(0).value);
            out.println(s + "," + data.base64EncodedString());
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("string.csv");
    }

    @Test
    public void testLength() throws Exception {
        // Zero length
        assertEquals(0, new Data().length());
        assertEquals(0, new Data(new byte[0]).length());
        assertEquals(0, new Data((byte) 0,0).length());
        assertEquals(0, new Data("").length());
        assertEquals(0, new Data((String) null).length());
        // Length = 1
        assertEquals(1, new Data(new byte[1]).length());
        assertEquals(1, new Data((byte) 0,1).length());
        // Length = 2
        assertEquals(2, new Data(new byte[2]).length());
        assertEquals(2, new Data((byte) 0,2).length());
        // Should not be possible due to @NonNull but good to check anyway
        assertEquals(0, new Data((byte[]) null).length());
    }

    @Test
    public void testSize() throws Exception {
        // Zero length
        assertEquals(0, new Data().size());
        assertEquals(0, new Data(new byte[0]).size());
        assertEquals(0, new Data((byte) 0,0).size());
        assertEquals(0, new Data("").size());
        assertEquals(0, new Data((String) null).size());
        // Length = 1
        assertEquals(1, new Data(new byte[1]).size());
        assertEquals(1, new Data((byte) 0,1).size());
        // Length = 2
        assertEquals(2, new Data(new byte[2]).size());
        assertEquals(2, new Data((byte) 0,2).size());
        // Should not be possible due to @NonNull but good to check anyway
        assertEquals(0, new Data((byte[]) null).size());
    }

    @Test
    public void testReversed() {
        byte[] bytes = new byte[] {0,1,2,3};
        Data d = new Data(bytes);
        assertEquals(4,d.length());
        assertEquals(0,d.uint8(0).value);
        assertEquals(1,d.uint8(1).value);
        assertEquals(2,d.uint8(2).value);
        assertEquals(3,d.uint8(3).value);

        Data rev = d.reversed();
        assertEquals(4,rev.length());
        assertEquals(0,rev.uint8(3).value);
        assertEquals(1,rev.uint8(2).value);
        assertEquals(2,rev.uint8(1).value);
        assertEquals(3,rev.uint8(0).value);

        Data back = rev.reversed();
        assertEquals(4,back.length());
        assertEquals(d,back);
    }
}

