//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PayloadSharingDataTests {

    @Test
    public void testInit() {
        final RSSI rssi = new RSSI(1);
        final Data data = new Data((byte) 3, 3);
        final PayloadSharingData payloadSharingData = new PayloadSharingData(rssi, data);
        assertEquals(rssi, payloadSharingData.rssi);
        assertEquals(rssi.value, payloadSharingData.rssi.value, Double.MIN_VALUE);
        assertEquals(data, payloadSharingData.data);
        assertArrayEquals(data.value, payloadSharingData.data.value);
    }
}
