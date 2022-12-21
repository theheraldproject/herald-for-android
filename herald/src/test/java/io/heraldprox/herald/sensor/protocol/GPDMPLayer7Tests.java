//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class GPDMPLayer7Tests {
    @Test
    public void testIncomingStackReceipt() {

        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer6Manager layer6 = new DummyGPDMPLayer6Manager();
        stack.replaceLayer6(layer6);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer6Manager",
                stack.getLayer6().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer7Manager",
                stack.getLayer7().getClass().getName()
        );
        // Now get linked layer 6 object (for test verification)
        assertEquals(layer6,stack.getLayer6());

        ConcreteGPDMPLayer7Manager layer7 = (ConcreteGPDMPLayer7Manager)stack.getLayer7();

        UUID channelId = UUID.fromString("56785678-1234-1234-1234-123412341234");

        DummyGPDMPMessageListener listener = new DummyGPDMPMessageListener();
        layer7.addMessageListener(channelId, listener);

        // Add a listener for Layer7

        // TESTS
        // Pass appropriate data to dummy Layer 4
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        UUID channelIdEncoded = UUID.fromString("12341234-1234-1234-1234-123412341234");
        Date timeToAccess = new Date(100000);
        Date timeout = new Date(200000);
        UInt16 ttl = new UInt16(32);
        UInt16 minTransmissions = new UInt16(3);
        UInt16 maxTransmissions = new UInt16(21);

        // layer 4 data
        UUID messageId = UUID.fromString("56785678-5678-5678-5678-123412341234");
        UInt16 fragmentSeqNum = new UInt16(1);
        UInt16 fragmentPartialHash = new UInt16(0);
        UInt16 totalFragmentsExpected = new UInt16(4);
        UInt16 fragmentsCurrentlyAvailable = new UInt16(1); // the first one of 4

        // Layer 5 data
        UUID senderRecipientId = UUID.fromString("11111111-1111-1111-1111-222222222222");
        boolean valid = true;
        ArrayList<ConcreteExtendedDataSectionV1> sections = new ArrayList<>();
        Data sentData = new Data((byte)6,4);
        ConcreteExtendedDataSectionV1 s1 = new ConcreteExtendedDataSectionV1(new UInt8(0),new UInt8(4),sentData);
        sections.add(s1);

        PayloadData fromNetwork = new PayloadData("010203040506");
        layer6.sendIncoming(ti, channelIdEncoded, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions,
                channelId, messageId, fragmentSeqNum, fragmentPartialHash, totalFragmentsExpected,
                fragmentsCurrentlyAvailable,
                GPDMPLayer5MessageType.MESSAGE, senderRecipientId, valid,
                sections); // TODO rework this for the correct data
        // Test output at dummy layer 7
        assertEquals(ti, listener.lastFrom);
        List<ConcreteExtendedDataSectionV1> receivedSections = listener.lastSections;
        assertEquals(1, receivedSections.size()); // Should this be 0 until all fragment arrived?

        // remove listener and try again (lastFrom should stay null)
        listener.lastFrom = null;
        layer7.removeMessageListener(channelId, listener);
        layer6.sendIncoming(ti, channelIdEncoded, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions,
                channelId, messageId, fragmentSeqNum, fragmentPartialHash, totalFragmentsExpected,
                fragmentsCurrentlyAvailable,
                GPDMPLayer5MessageType.MESSAGE, senderRecipientId, valid,
                sections); // TODO rework this for the correct data
        assertNull(listener.lastFrom);

    }
}
