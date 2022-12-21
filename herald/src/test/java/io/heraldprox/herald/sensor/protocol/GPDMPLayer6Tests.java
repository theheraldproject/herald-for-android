//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class GPDMPLayer6Tests {
    @Test
    public void testIncomingStackReceipt() {

        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer5Manager layer5 = new DummyGPDMPLayer5Manager();
        stack.replaceLayer5(layer5);
        DummyGPDMPLayer7Manager layer7 = new DummyGPDMPLayer7Manager();
        stack.replaceLayer7(layer7);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer5Manager",
                stack.getLayer5().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer6Manager",
                stack.getLayer6().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer7Manager",
                stack.getLayer7().getClass().getName()
        );
        // Now get linked layer 5 and 7 objects (for test verification)
        assertEquals(layer5,stack.getLayer5());
        assertEquals(layer7,stack.getLayer7());

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
        UUID channelId = UUID.fromString("56785678-1234-1234-1234-123412341234");
        UUID messageId = UUID.fromString("56785678-5678-5678-5678-123412341234");
        UInt16 fragmentSeqNum = new UInt16(1);
        UInt16 fragmentPartialHash = new UInt16(0);
        UInt16 totalFragmentsExpected = new UInt16(4);
        UInt16 fragmentsCurrentlyAvailable = new UInt16(1); // the first one of 4

        // layer 5 data
        UUID mySenderId = UUID.randomUUID();

        PayloadData fromNetwork = new PayloadData("010203040506");
        layer5.sendIncomingData(ti, channelIdEncoded, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions,
                channelId, messageId, fragmentSeqNum, fragmentPartialHash, totalFragmentsExpected,
                fragmentsCurrentlyAvailable,
                GPDMPLayer5MessageType.MESSAGE,
                mySenderId,
                fromNetwork); // TODO rework this for the correct data
        // Test output at dummy layer 7
        assertEquals(ti, layer7.lastFrom);
        List<ConcreteExtendedDataSectionV1> sections = layer7.lastSections;
        assertEquals(1, sections.size()); // Should this be 0 until all fragment arrived?
        // TODO validate actual data too
        // TODO validate other fields too

    }

    @Test
    public void testOutgoingStackReceipt() {

        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer5Manager layer5 = new DummyGPDMPLayer5Manager();
        stack.replaceLayer5(layer5);
        DummyGPDMPLayer7Manager layer7 = new DummyGPDMPLayer7Manager();
        stack.replaceLayer7(layer7);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer5Manager",
                stack.getLayer5().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer6Manager",
                stack.getLayer6().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer7Manager",
                stack.getLayer7().getClass().getName()
        );
        // Now get linked layer 5 and 7 objects (for test verification)
        assertEquals(layer5,stack.getLayer5());
        assertEquals(layer7,stack.getLayer7());

        // TESTS
        // Pass appropriate data to dummy Layer 7
        Date timeToAccess = new Date(100000);
        Date timeout = new Date(200000);
        UInt16 ttl = new UInt16(32);
        UInt16 minTransmissions = new UInt16(3);
        UInt16 maxTransmissions = new UInt16(21);

        UUID channelId = UUID.fromString("56785678-1234-1234-1234-123412341234");
        UUID mySenderChannelUniqueId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ArrayList sections = new ArrayList<ConcreteExtendedDataSectionV1>();
        Data rawData = new Data((byte)6,4);
        ConcreteExtendedDataSectionV1 s1 = new ConcreteExtendedDataSectionV1(new UInt8(0),new UInt8(4),rawData);
        sections.add(s1);
//        sections.add(new ConcreteExtendedDataSectionV1(new UInt8(2),new UInt8(7),new Data((byte)5,7)));

        PayloadData fromNetwork = new PayloadData("010203040506");
        UUID messageId = layer7.sendOutgoing(channelId,timeToAccess,timeout,ttl,minTransmissions,maxTransmissions,
                mySenderChannelUniqueId,sections);
        // Test output at dummy layer 5
        assertEquals(DummyGPDMPLayer5Manager.dummyMessageId,layer5.lastMessageId);
        assertNotNull(messageId);
        assertEquals(DummyGPDMPLayer5Manager.dummyMessageId,messageId);
        assertEquals(channelId, layer5.lastChannelId);
        assertEquals(timeToAccess,layer5.lastTimeToAccess);
        assertEquals(timeout,layer5.lastTimeout);
        assertEquals(ttl,layer5.lastTtl);
        assertEquals(minTransmissions,layer5.lastMinTransmissions);
        assertEquals(maxTransmissions,layer5.lastMaxTransmissions);
        // TODO change this once encryption is implemented
        assertEquals(s1.data.size(),layer5.lastL6SenderEncryptedData.size() - 2); // code and length removed
        assertEquals(rawData,layer5.lastL6SenderEncryptedData.subdata(2));
    }
}
