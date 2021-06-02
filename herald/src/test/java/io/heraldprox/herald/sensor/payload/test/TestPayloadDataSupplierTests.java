//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.test;

import androidx.annotation.NonNull;

import org.junit.Test;

import io.heraldprox.herald.sensor.ble.BLEDevice;
import io.heraldprox.herald.sensor.ble.BLEDeviceAttribute;
import io.heraldprox.herald.sensor.ble.BLEDeviceDelegate;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class TestPayloadDataSupplierTests {

    @Test
    public void testPayload() {
        for (int i=0; i<200; i++) {
            final TestPayloadDataSupplier testPayloadDataSupplier = new TestPayloadDataSupplier(i, i);
            final PayloadData payloadData = testPayloadDataSupplier.payload(null, null);
            assertNotNull(payloadData);
            // Payload data is always >= 7 bytes
            assertTrue(payloadData.value.length >= 7);
            // Bytes 0..2 are reserved
            assertEquals(0, payloadData.value[0]);
            assertEquals(0, payloadData.value[1]);
            assertEquals(0, payloadData.value[2]);
            // Bytes 3..6 are Int32 identifier
            assertEquals(i, payloadData.int32(3).value);
            // Remaining bytes are fillers
            for (int j=7; j<i; j++) {
                assertEquals(0, payloadData.value[j]);
            }
        }
    }

    @Test
    public void testLegacyPayload() {
        final TestPayloadDataSupplier testPayloadDataSupplier = new TestPayloadDataSupplier(5, 7);
        final BLEDevice bleDevice = new BLEDevice(new TargetIdentifier("test"), new BLEDeviceDelegate() {
            @Override
            public void device(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute didUpdate) {
            }
        });
        bleDevice.rssi(new RSSI(-10));
        final PayloadData payloadData = testPayloadDataSupplier.payload(null, null);
        final String payloadDataBase64 = payloadData.base64EncodedString();
        final LegacyPayloadData legacyPayloadData = testPayloadDataSupplier.legacyPayload(null, bleDevice);
        final String json = new String(legacyPayloadData.value);
        assertTrue(json.contains("\"rs\":-10"));
        assertTrue(json.contains("\"v\":2"));
        assertTrue(json.contains("\"id\":\"" + payloadDataBase64 + "\""));
        assertTrue(json.contains("\"o\":\"OT_HA\""));
    }
}
