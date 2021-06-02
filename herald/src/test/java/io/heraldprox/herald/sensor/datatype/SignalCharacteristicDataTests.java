//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class SignalCharacteristicDataTests {

    @Test
    public void testEncodeDecodeWriteRSSI() {
        for (short i=Short.MIN_VALUE; i<Short.MAX_VALUE; i++) {
            final RSSI rssi = SignalCharacteristicData.decodeWriteRSSI(SignalCharacteristicData.encodeWriteRssi(new RSSI(i)));
            assertEquals(i, rssi.value, Double.MIN_VALUE);
        }
        assertNull(SignalCharacteristicData.decodeWriteRSSI(null));
        assertNull(SignalCharacteristicData.decodeWriteRSSI(new Data()));
    }

    @Test
    public void testEncodeDecodeWritePayload() {
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final byte[] bytes = new byte[i];
            random.nextBytes(bytes);
            final PayloadData expected = new PayloadData(bytes);
            final PayloadData actual = SignalCharacteristicData.decodeWritePayload(SignalCharacteristicData.encodeWritePayload(expected));
            assertNotNull(actual);
            assertArrayEquals(expected.value, actual.value);
        }
        assertNull(SignalCharacteristicData.decodeWritePayload(null));
        assertNull(SignalCharacteristicData.decodeWritePayload(new Data()));
    }

    @Test
    public void testEncodeDecodeWritePayloadSharing() {
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final byte[] bytes = new byte[i];
            random.nextBytes(bytes);
            final PayloadSharingData expected = new PayloadSharingData(new RSSI(i), new Data(bytes));
            final PayloadSharingData actual = SignalCharacteristicData.decodeWritePayloadSharing(SignalCharacteristicData.encodeWritePayloadSharing(expected));
            assertNotNull(actual);
            assertEquals(expected.rssi, actual.rssi);
            assertEquals(expected.data, actual.data);
        }
        assertNull(SignalCharacteristicData.decodeWritePayloadSharing(null));
        assertNull(SignalCharacteristicData.decodeWritePayloadSharing(new Data()));
    }

    @Test
    public void testEncodeDecodeImmediateSend() {
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final byte[] bytes = new byte[i];
            random.nextBytes(bytes);
            final ImmediateSendData expected = new ImmediateSendData(new Data(bytes));
            final ImmediateSendData actual = SignalCharacteristicData.decodeImmediateSend(SignalCharacteristicData.encodeImmediateSend(expected));
            assertNotNull(actual);
            assertEquals(expected.data, actual.data);
        }
        assertNull(SignalCharacteristicData.decodeImmediateSend(null));
        assertNull(SignalCharacteristicData.decodeImmediateSend(new Data()));
    }

    @Test
    public void testDetect() {
        assertEquals(SignalCharacteristicDataType.rssi, SignalCharacteristicData.detect(SignalCharacteristicData.encodeWriteRssi(new RSSI(0))));
        assertEquals(SignalCharacteristicDataType.payload, SignalCharacteristicData.detect(SignalCharacteristicData.encodeWritePayload(new PayloadData())));
        assertEquals(SignalCharacteristicDataType.payloadSharing, SignalCharacteristicData.detect(SignalCharacteristicData.encodeWritePayloadSharing(new PayloadSharingData(new RSSI(0), new Data()))));
        assertEquals(SignalCharacteristicDataType.immediateSend, SignalCharacteristicData.detect(SignalCharacteristicData.encodeImmediateSend(new ImmediateSendData(new Data()))));
        assertEquals(SignalCharacteristicDataType.unknown, SignalCharacteristicData.detect(new Data()));
    }
}
