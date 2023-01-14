//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.Int64;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class GPDMPLayer4Tests {
    @Test
    public void testIncomingStackReceipt() {

        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer3Manager layer3 = new DummyGPDMPLayer3Manager();
        stack.replaceLayer3(layer3);
        DummyGPDMPLayer5Manager layer5 = new DummyGPDMPLayer5Manager();
        stack.replaceLayer5(layer5);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer3Manager",
                stack.getLayer3().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer4Manager",
                stack.getLayer4().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer5Manager",
                stack.getLayer5().getClass().getName()
        );
        // Now get linked layer 3 and 5 objects (for test verification)
        assertEquals(layer3,stack.getLayer3());
        assertEquals(layer5,stack.getLayer5());

        // TESTS
        // Pass appropriate data to dummy Layer 3
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        UUID channelId = UUID.randomUUID();
        System.out.println("Random UUID String: " + channelId.toString());
        Date timeToAccess = new Date(100000);
        Date timeout = new Date(200000);
        UInt16 ttl = new UInt16(32);
        UInt16 minTransmissions = new UInt16(3);
        UInt16 maxTransmissions = new UInt16(21);
        PayloadData fromNetwork = new PayloadData();
        fromNetwork.append(channelId);
        assertEquals(16,fromNetwork.length());
        // TODO encode/encrypt
        fromNetwork.append(Data.fromHexEncodedString("0102030405060708090a0b0c0d0e0f"));

        PayloadData l5Data = new PayloadData();
        l5Data.append(Data.fromHexEncodedString("0102030405060708090a0b0c0d0e0f"));
        layer3.sendIncomingData(ti, channelId, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions, fromNetwork);
        // Test output at dummy layer 5
        assertEquals(ti, layer5.lastFrom);
        assertEquals(l5Data, layer5.lastL4Data);
        // TODO validate other fields too
    }
}
