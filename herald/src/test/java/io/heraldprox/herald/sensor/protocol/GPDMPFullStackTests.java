//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class GPDMPFullStackTests {
    @Test
    public void testIncomingFullStack() {
        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        // TODO replace this with the use of Concrete layer 7 instead
        DummyGPDMPLayer7Manager layer7 = new DummyGPDMPLayer7Manager();
        stack.replaceLayer7(layer7);

        // Now get linked layer 1 and 3 objects (for test verification)
        DummyGPDMPLayer1BluetoothLEManager layer1 = (DummyGPDMPLayer1BluetoothLEManager)stack.getLayer1();

        // TESTS
        // Pass appropriate data to dummy Layer 1
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        PayloadData fromNetwork = new PayloadData("010203040506");
        layer1.sendHeraldProtocolV1SecureCharacteristicData(ti,fromNetwork);
        // Test output at dummy layer 7
        List<ConcreteExtendedDataSectionV1> sections = layer7.lastSections;
        assertEquals(1, sections.size());
        // TODO validate encoded uuid, time for access, and timeout
    }

    @Test
    public void testSendAndReceive() {
        // SET UP RECEIVER STACK
        ConcreteGPDMPProtocolStack senderStack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();

        // TODO replace this with the use of Concrete layer 7 instead
        DummyGPDMPLayer7Manager layer7 = new DummyGPDMPLayer7Manager();
        senderStack.replaceLayer7(layer7);

        // Check there are no nearby recipients
        ConcreteGPDMPLayer3Manager layer3 = (ConcreteGPDMPLayer3Manager)senderStack.getLayer3();
        assertEquals(0, layer3.getPotentialRecipientsCount());

        // PART A: SENDER
        // Ensure there's the receiving Bluetooth device nearby!
        TargetIdentifier receiver = new TargetIdentifier();
        layer3.sensor(SensorType.BLE, receiver);
        PayloadData heraldData = new PayloadData(); // didDetect
        layer3.sensor(SensorType.BLE, heraldData, receiver); // didRead (I.e. is a Herald device)

        assertEquals(1, layer3.getPotentialRecipientsCount());

        // Now get linked layer 1 and 3 objects (for test verification)
        DummyGPDMPLayer1BluetoothLEManager layer1 = (DummyGPDMPLayer1BluetoothLEManager)senderStack.getLayer1();

        UUID channelId = UUID.fromString("12345678-9012-1234-1234-123456789012");
        Date timeToAccess = new Date(); // now
        Date timeout = new Date(timeToAccess.secondsSinceUnixEpoch() + (60 * 60 * 24));
        UInt16 ttl = new UInt16(7);
        UInt16 min = new UInt16(3);
        UInt16 max = new UInt16(60);
        UUID mySenderChannelUniqueId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ArrayList sections = new ArrayList<ConcreteExtendedDataSectionV1>();
        Data sentData = new Data((byte)6,4);
        ConcreteExtendedDataSectionV1 s1 = new ConcreteExtendedDataSectionV1(new UInt8(0),new UInt8(4),sentData);
        sections.add(s1);
//        sections.add(new ConcreteExtendedDataSectionV1(new UInt8(2),new UInt8(7),new Data((byte)5,7)));

        UUID messageId = layer7.sendOutgoing(channelId, timeToAccess, timeout, ttl,
                min,max,mySenderChannelUniqueId, sections);
        assertNotNull(messageId);
        assertNotEquals(0,layer1.outgoingData.size());
        PayloadData raw = layer1.outgoingData.get(0).b;
        assertNotEquals(0, raw.size());


        // PART B: Retransmit the data up the receiver stack
        ConcreteGPDMPProtocolStack receiverStack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        // TODO replace this with the use of Concrete layer 7 instead
        DummyGPDMPLayer7Manager layer7Receiver = new DummyGPDMPLayer7Manager();
        receiverStack.replaceLayer7(layer7Receiver);
        // Ensure there's the sending Bluetooth device nearby!
        ConcreteGPDMPLayer3Manager layer3Receiver = (ConcreteGPDMPLayer3Manager)receiverStack.getLayer3();
        TargetIdentifier sender = new TargetIdentifier();
        layer3.sensor(SensorType.BLE, receiver);
        layer3.sensor(SensorType.BLE, heraldData, sender); // didRead (I.e. is a Herald device)

        DummyGPDMPLayer1BluetoothLEManager layer1Receiver = (DummyGPDMPLayer1BluetoothLEManager)receiverStack.getLayer1();

        // TODO send all not just first (in case it's fragmented)
        layer1Receiver.sendHeraldProtocolV1SecureCharacteristicData(sender,raw);

        // Now see what we have back at layer 7
        assertEquals(sender, layer7Receiver.lastFrom);
        List<ConcreteExtendedDataSectionV1> receivedSections = layer7Receiver.lastSections;
        assertEquals(sections.size(),receivedSections.size());
        assertEquals(s1.data.size(),receivedSections.get(0).data.size());
        assertEquals(s1.data,receivedSections.get(0).data);

        // Ensure original data we sent in first section is the data we've received back
        assertEquals(sentData,receivedSections.get(0).data);

    }
}
