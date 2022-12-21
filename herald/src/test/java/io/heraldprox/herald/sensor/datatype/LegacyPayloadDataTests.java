//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;

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
            final LegacyPayloadData payloadData = new LegacyPayloadData(BLESensorConfiguration.linuxFoundationServiceUUID, data);
            assertArrayEquals(data, payloadData.value);
            assertNotNull(payloadData.shortName());
            assertEquals(payloadData.toString(), payloadData.shortName());
        }
    }

    @Test
    public void testProtocol() {
        assertEquals(LegacyPayloadData.ProtocolName.NOT_AVAILABLE, new LegacyPayloadData(null, new byte[0]).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.OPENTRACE, new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, new byte[0]).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.ADVERT, new LegacyPayloadData(BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID, new byte[0]).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.HERALD, new LegacyPayloadData(BLESensorConfiguration.linuxFoundationServiceUUID, new byte[0]).protocolName());
        assertEquals(LegacyPayloadData.ProtocolName.UNKNOWN, new LegacyPayloadData(UUID.fromString("A6BA4286-0000-0000-0000-9467EF0B31A8"), new byte[0]).protocolName());
    }

    @Test
    public void testOpenTracePayloadShortName() {
        final PayloadData payloadData = new PayloadData(new byte[13]);
        final String openTracePayloadString = "{\"id\":\"" + payloadData.base64EncodedString() + "\"}";
        final LegacyPayloadData legacyPayloadData = new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, openTracePayloadString.getBytes());
        assertEquals(payloadData.shortName(), legacyPayloadData.shortName());
    }
}
