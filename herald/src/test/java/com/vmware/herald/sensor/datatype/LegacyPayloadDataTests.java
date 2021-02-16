//  Copyright 2021 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import com.vmware.herald.sensor.ble.BLESensorConfiguration;

import org.json.JSONException;
import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LegacyPayloadDataTests {

    @Test
    public void testShortName() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final byte[] data = new byte[i];
            random.nextBytes(data);
            final LegacyPayloadData payloadData = new LegacyPayloadData(BLESensorConfiguration.serviceUUID, data);
            assertArrayEquals(data, payloadData.value);
            assertNotNull(payloadData.shortName());
            assertEquals(payloadData.toString(), payloadData.shortName());
        }
    }

    @Test
    public void testProtocol() {
        assertEquals(LegacyPayloadData.ProtocolName.NOT_AVAILABLE, new LegacyPayloadData(null, null).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.OPENTRACE, new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, null).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.ADVERT, new LegacyPayloadData(BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID, null).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.HERALD, new LegacyPayloadData(BLESensorConfiguration.serviceUUID, null).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.UNKNOWN, new LegacyPayloadData(UUID.fromString("A6BA4286-0000-0000-0000-9467EF0B31A8"), null).protocolName());
    }

    @Test
    public void testOpenTracePayloadShortName() throws JSONException {
        final PayloadData payloadData = new PayloadData(new byte[13]);
        final String openTracePayloadString = "{\"id\":\"" + payloadData.base64EncodedString() + "\"}";
        final LegacyPayloadData legacyPayloadData = new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, openTracePayloadString.getBytes());
        assertEquals(payloadData.shortName(), legacyPayloadData.shortName());
    }
}
