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
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class GPDMPLayer5Tests {
    @Test
    public void testIncomingStackReceipt() {

        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer4Manager layer4 = new DummyGPDMPLayer4Manager();
        stack.replaceLayer4(layer4);
        DummyGPDMPLayer6Manager layer6 = new DummyGPDMPLayer6Manager();
        stack.replaceLayer6(layer6);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer4Manager",
                stack.getLayer4().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer5Manager",
                stack.getLayer5().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer6Manager",
                stack.getLayer6().getClass().getName()
        );
        // Now get linked layer 4 and 6 objects (for test verification)
        assertEquals(layer4,stack.getLayer4());
        assertEquals(layer6,stack.getLayer6());

        // TESTS
        // shared data
        UUID channelId = UUID.randomUUID();

        // Pass appropriate data to dummy Layer 4
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        // TODO make this actually encoded
        UUID channelIdEncoded = channelId;
        Date timeToAccess = new Date(100000);
        Date timeout = new Date(200000);
        UInt16 ttl = new UInt16(32);
        UInt16 minTransmissions = new UInt16(3);
        UInt16 maxTransmissions = new UInt16(21);

        // layer 4 data
        UUID messageId = UUID.randomUUID();
        UUID senderRecipientId = UUID.randomUUID();
        UUID remoteRecipientId = UUID.randomUUID();
//        Date securityEpoch = new Date();
        // Inform layer5 of supported session data
        GPDMPLayer5Manager l5 = stack.getLayer5();
        l5.createSession(channelId, senderRecipientId, timeToAccess, remoteRecipientId);
        UInt16 fragmentSeqNum = new UInt16(1);
        UInt16 fragmentPartialHash = new UInt16(0);
        UInt16 totalFragmentsExpected = new UInt16(4);
        UInt16 fragmentsCurrentlyAvailable = new UInt16(1); // the first one of 4

        PayloadData fromNetwork = new PayloadData();
        Data l6Payload = Data.fromHexEncodedString("0102030405060708090a0b0c0d0e0f10");
        fromNetwork.append(new UInt16((int)(remoteRecipientId.getMostSignificantBits() & 0xffff)));
        fromNetwork.append(l6Payload);
        layer4.sendIncomingData(ti, channelIdEncoded, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions,
                channelId, messageId, fragmentSeqNum, fragmentPartialHash, totalFragmentsExpected,
                fragmentsCurrentlyAvailable,
                fromNetwork); // TODO rework this for the correct data
        // Test output at dummy layer 6
        assertEquals(ti, layer6.lastFrom);
        assertEquals(l6Payload.hexEncodedString(), layer6.lastL6ChannelDecryptedData.hexEncodedString());
        // TODO validate other fields too

    }
}
