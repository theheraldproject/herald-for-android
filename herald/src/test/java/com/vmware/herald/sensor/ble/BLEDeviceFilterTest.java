//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.ble.BLEDeviceFilter;
import com.vmware.herald.sensor.datatype.Data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BLEDeviceFilterTest {

    @Test
    public void testHexTransform() throws Exception {
        final Random random = new Random(0);
        for (int i = 0; i < 1000; i++) {
            final byte[] expected = new byte[i];
            random.nextBytes(expected);
            final String hex = new Data(expected).hexEncodedString();
            final byte[] actual = Data.fromHexEncodedString(hex).value;
            assertArrayEquals(expected, actual);
        }
    }


    @Test
    public void testManufacturerData() throws Exception {
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(2, segments.size());
            assertEquals("1006071EA3DD89E0", segments.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", segments.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A14FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("0100000000000000000000200000000000", segments.get(0).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C0010060C1E4FDE4DF714FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(2, segments.size());
            assertEquals("10060C1E4FDE4DF7", segments.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", segments.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("0201060AFF4C001005421C1E616A000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> segments = BLEDeviceFilter.extractManufacturerData(raw);
            assertEquals(1, segments.size());
            assertEquals("1005421C1E616A", segments.get(0).hexEncodedString());
        }
    }

    @Test
    public void testMessageData() throws Exception {
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C001006071EA3DD89E014FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(2, messages.size());
            assertEquals("1006071EA3DD89E0", messages.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A14FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(1, messages.size());
            assertEquals("0100000000000000000000200000000000", messages.get(0).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("02011A020A0C0BFF4C0010060C1E4FDE4DF714FF4C0001000000000000000000002000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(2, messages.size());
            assertEquals("10060C1E4FDE4DF7", messages.get(0).hexEncodedString());
            assertEquals("0100000000000000000000200000000000", messages.get(1).hexEncodedString());
        }
        {
            final Data raw = Data.fromHexEncodedString("0201060AFF4C001005421C1E616A000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            final List<Data> messages = BLEDeviceFilter.extractMessageData(BLEDeviceFilter.extractManufacturerData(raw));
            assertEquals(1, messages.size());
            assertEquals("1005421C1E616A", messages.get(0).hexEncodedString());
        }
        {
            final List<Data> segments = new ArrayList<>(0);
            final Data segment = new Data();
            segment.append(Data.fromHexEncodedString("10060C1E4FDE4DF7"));
            segment.append(Data.fromHexEncodedString("1005421C1E616A"));
            segment.append(Data.fromHexEncodedString("1006071EA3DD89E0"));
            segments.add(segment);
            final List<Data> messages = BLEDeviceFilter.extractMessageData(segments);
            assertEquals(3, messages.size());
            assertEquals("10060C1E4FDE4DF7", messages.get(0).hexEncodedString());
            assertEquals("1005421C1E616A", messages.get(1).hexEncodedString());
            assertEquals("1006071EA3DD89E0", messages.get(2).hexEncodedString());
        }
    }

    @Test
    public void testCompilePatterns() throws Exception {
        final List<BLEDeviceFilter.FilterPattern> filterPatterns = BLEDeviceFilter.compilePatterns(new String[]{"^10....04", "^10....14"});
        assertEquals(2, filterPatterns.size());
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10060C144FDE4DF7"));

        // Ignoring dots
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX044FDE4DF7"));
        assertNotNull(BLEDeviceFilter.match(filterPatterns, "10XXXX144FDE4DF7"));

        // Not correct values
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "10060C154FDE4DF7"));

        // Not start of pattern
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C054FDE4DF7"));
        assertNull(BLEDeviceFilter.match(filterPatterns, "010060C154FDE4DF7"));
    }
}