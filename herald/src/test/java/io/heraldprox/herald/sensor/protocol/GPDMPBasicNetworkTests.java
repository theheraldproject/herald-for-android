//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

public class GPDMPBasicNetworkTests {
    // Disabled for the v2.1.0 Beta release of GPDMP
//    @Test
//    public void testSimpleNetwork() {
//        // 1. Create a network of three nodes where A can see B, and B can see A and C, and C can see B
//        ConcreteGPDMPProtocolStack stackA = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
//        ConcreteGPDMPProtocolStack stackB = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
//        ConcreteGPDMPProtocolStack stackC = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
//        // Ensure IDs are unique
//        DummyGPDMPLayer1BluetoothLEManager layer1A = (DummyGPDMPLayer1BluetoothLEManager)stackA.getLayer1();
//        DummyGPDMPLayer1BluetoothLEManager layer1B = (DummyGPDMPLayer1BluetoothLEManager)stackB.getLayer1();
//        DummyGPDMPLayer1BluetoothLEManager layer1C = (DummyGPDMPLayer1BluetoothLEManager)stackC.getLayer1();
//        assertNotEquals(layer1A.identifier,layer1B.identifier);
//        assertNotEquals(layer1B.identifier,layer1C.identifier);
//
//        // Now introduce devices to each other
//        layer1A.advertisementSeen(layer1B.identifier,layer1B);
//        layer1B.advertisementSeen(layer1A.identifier,layer1A);
//
//        layer1B.advertisementSeen(layer1C.identifier,layer1C);
//        layer1C.advertisementSeen(layer1B.identifier,layer1B);
//
//        assertEquals(1,layer1A.nearby.keySet().size());
//        assertEquals(2,layer1B.nearby.keySet().size());
//        assertEquals(1,layer1C.nearby.keySet().size());
//        assertTrue(layer1A.nearby.keySet().contains(layer1B.identifier));
//        assertTrue(layer1B.nearby.keySet().contains(layer1A.identifier));
//        assertTrue(layer1B.nearby.keySet().contains(layer1C.identifier));
//        assertTrue(layer1C.nearby.keySet().contains(layer1B.identifier));
//
//        // 2. Now at Layer 7, introduce just devices A and C to the same network of interest
//        DummyGPDMPLayer7Manager layer7A = new DummyGPDMPLayer7Manager();
//        stackA.replaceLayer7(layer7A);
//        DummyGPDMPLayer7Manager layer7B = new DummyGPDMPLayer7Manager();
//        stackB.replaceLayer7(layer7B);
//        DummyGPDMPLayer7Manager layer7C = new DummyGPDMPLayer7Manager();
//        stackC.replaceLayer7(layer7C);
////        GPDMPLayer7Manager layer7A = stackA.getLayer7();
////        GPDMPLayer7Manager layer7B = stackB.getLayer7();
////        GPDMPLayer7Manager layer7C = stackC.getLayer7();
//        DummyGPDMPMessageListener listenerA = new DummyGPDMPMessageListener();
//        DummyGPDMPMessageListener listenerB = new DummyGPDMPMessageListener();
//        DummyGPDMPMessageListener listenerC = new DummyGPDMPMessageListener();
//        UUID mutualChannel = UUID.randomUUID();
//        UUID bOnlyChannel = UUID.randomUUID();
//        assertNotEquals(mutualChannel,bOnlyChannel);
//        layer7A.addMessageListener(mutualChannel,listenerA);
//        layer7B.addMessageListener(bOnlyChannel,listenerB);
//        layer7C.addMessageListener(mutualChannel,listenerC);
//
//        // Don't forget to let the Herald protocol independent layer3 to 'see' the devices too
//        PayloadData heraldData = new PayloadData(); // didDetect - dummy data shared by all
//
//        ConcreteGPDMPLayer3Manager layer3A = (ConcreteGPDMPLayer3Manager)stackA.getLayer3();
//        layer3A.sensor(SensorType.BLE, layer1B.identifier);
//        layer3A.sensor(SensorType.BLE, true, layer1B.identifier);
//        layer3A.sensor(SensorType.BLE, heraldData, layer1B.identifier); // didRead (I.e. is a Herald device)
//        ConcreteGPDMPLayer3Manager layer3B = (ConcreteGPDMPLayer3Manager)stackB.getLayer3();
//        layer3B.sensor(SensorType.BLE, layer1A.identifier);
//        layer3B.sensor(SensorType.BLE, true, layer1A.identifier);
//        layer3B.sensor(SensorType.BLE, heraldData, layer1A.identifier); // didRead (I.e. is a Herald device)
//
//        ConcreteGPDMPLayer3Manager layer3C = (ConcreteGPDMPLayer3Manager)stackC.getLayer3();
//        layer3B.sensor(SensorType.BLE, layer1C.identifier);
//        layer3B.sensor(SensorType.BLE, true, layer1C.identifier);
//        layer3B.sensor(SensorType.BLE, heraldData, layer1C.identifier); // didRead (I.e. is a Herald device)
//        layer3C.sensor(SensorType.BLE, layer1B.identifier);
//        layer3C.sensor(SensorType.BLE, true, layer1B.identifier);
//        layer3C.sensor(SensorType.BLE, heraldData, layer1B.identifier); // didRead (I.e. is a Herald device)
//
//        // Ensure this is reflected too
//        assertEquals(1, layer3A.getPotentialRecipientsCount());
//        assertEquals(2, layer3B.getPotentialRecipientsCount());
//        assertEquals(1, layer3C.getPotentialRecipientsCount());
//
//        // Session data
//        Date channelEpoch = new Date(); // now
//        UUID senderRecipientIdA = UUID.randomUUID();
//        UUID senderRecipientIdB = UUID.randomUUID();
//
//        // Set up layer 5s too
//        ConcreteGPDMPLayer5Manager layer5A = (ConcreteGPDMPLayer5Manager)stackA.getLayer5();
////        ConcreteGPDMPLayer5Manager layer5B = (ConcreteGPDMPLayer5Manager)stackB.getLayer5(); // Transit Only
//        ConcreteGPDMPLayer5Manager layer5C = (ConcreteGPDMPLayer5Manager)stackC.getLayer5();
//        layer5A.createSession(mutualChannel,senderRecipientIdA,channelEpoch,senderRecipientIdB);
//        layer5C.createSession(mutualChannel,senderRecipientIdB,channelEpoch,senderRecipientIdA);
//
//        // 3. Now send a message from A to C on the mutual channel
//        Date timeToAccess = new Date(); // now
//        Date timeout = new Date(timeToAccess.secondsSinceUnixEpoch() + (60 * 60 * 24));
//        UInt16 ttl = new UInt16(7);
//        UInt16 min = new UInt16(3);
//        UInt16 max = new UInt16(60);
//        ArrayList sections = new ArrayList<ConcreteExtendedDataSectionV1>();
//        Data sentData = new Data((byte)6,4);
//        ConcreteExtendedDataSectionV1 s1 = new ConcreteExtendedDataSectionV1(new UInt8(0),new UInt8(4),sentData);
//        sections.add(s1);
////        sections.add(new ConcreteExtendedDataSectionV1(new UInt8(2),new UInt8(7),new Data((byte)5,7)));
//
//        UUID messageId = layer7A.sendOutgoing(mutualChannel, timeToAccess, timeout, ttl,
//                min,max,senderRecipientIdA, sections);
//
//        // NOTE: The below will only work once we have added in message routing at layer 3
//        // Remember we received VIA B, so this needs to be B!:-
////        assertEquals(layer1A.identifier, layer7B.lastFrom);
//        assertEquals(layer1B.identifier, layer7C.lastFrom);
//        List<ConcreteExtendedDataSectionV1> receivedSections = layer7C.lastSections;
//        assertEquals(sections.size(),receivedSections.size());
//        assertEquals(s1.data.size(),receivedSections.get(0).data.size());
//        assertEquals(s1.data,receivedSections.get(0).data);
//
//        // Ensure original data we sent in first section is the data we've received back
//        assertEquals(sentData,receivedSections.get(0).data);
//
//        // Check that layer B didn't pass the message to its layer 7
//        assertEquals(null,layer7B.lastFrom);
//        // And that layer A didn't receive its own message
//        assertEquals(null,layer7A.lastFrom);
//
//        // 4. Now send a message from C to A on the mutual channel
//    }
}
