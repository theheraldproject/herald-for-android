//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class GPDMPLayer3Tests {
    @Test
    public void testIncomingStackReceipt() {
        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer2BluetoothLEManager layer2 = new DummyGPDMPLayer2BluetoothLEManager();
        stack.replaceLayer2(layer2);
        DummyGPDMPLayer4Manager layer4 = new DummyGPDMPLayer4Manager();
        stack.replaceLayer4(layer4);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer2BluetoothLEManager",
                stack.getLayer2().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer3Manager",
                stack.getLayer3().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer4Manager",
                stack.getLayer4().getClass().getName()
        );
        // Now get linked layer 2 and 4 objects (for test verification)
        assertEquals(layer2,stack.getLayer2());
        assertEquals(layer4,stack.getLayer4());

        // TESTS

        // Before we add configuration
        // Check there are no nearby recipients
        ConcreteGPDMPLayer3Manager layer3 = (ConcreteGPDMPLayer3Manager)stack.getLayer3();
        assertEquals(0, layer3.getPotentialRecipientsCount());

        // Pass appropriate data to dummy Layer 2
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        // A valid L3 payload is at least 41 bytes!
        PayloadData fromNetwork = new PayloadData("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        assertTrue(fromNetwork.length() > 41); // Validity check before invoking test
        layer2.sendIncomingData(ti,fromNetwork);
        // Test output at dummy layer 3
        assertEquals(ti, layer4.lastFrom);
        assertEquals(fromNetwork.length() - 14, layer4.lastL4Data.length());
        // TODO validate other fields too
    }

    @Test
    public void testDeviceDetectable() {
        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer2BluetoothLEManager layer2 = new DummyGPDMPLayer2BluetoothLEManager();
        stack.replaceLayer2(layer2);
        DummyGPDMPLayer4Manager layer4 = new DummyGPDMPLayer4Manager();
        stack.replaceLayer4(layer4);

        // TESTS

        // Before we add configuration
        // Check there are no nearby recipients
        ConcreteGPDMPLayer3Manager layer3 = (ConcreteGPDMPLayer3Manager)stack.getLayer3();
        assertEquals(0, layer3.getPotentialRecipientsCount());

        // Pass appropriate data to dummy Layer 2
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");

        // Ensure we don't detect one for didDetect only
        layer3.sensor(SensorType.BLE,ti);
        assertEquals(0, layer3.getPotentialRecipientsCount());

        // Ensure we don't detect one for didMeasure only
        layer3.sensor(SensorType.BLE,new Proximity(ProximityMeasurementUnit.RSSI,-78.0),ti);
        assertEquals(0, layer3.getPotentialRecipientsCount());

        // TODO we need to modify the below so that we're sure it supports secure messaging, NOT just the Herald Payload
        // Ensure we detect one for didRead (i.e. has Herald payload)
        PayloadData heraldPayload = new PayloadData("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        assertTrue(heraldPayload.length() > 41); // Validity check before invoking test
        layer3.sensor(SensorType.BLE,heraldPayload,ti);
        layer3.sensor(SensorType.BLE,true,ti);
        assertEquals(1, layer3.getPotentialRecipientsCount());

        // Ensure we don't count duplicates
        layer3.sensor(SensorType.BLE,heraldPayload,ti);
        layer3.sensor(SensorType.BLE,true,ti);
        assertEquals(1, layer3.getPotentialRecipientsCount());

        // Note: Newly added didDisappear() callback support in SensorDelegate for return to 0
        layer3.sensor(SensorType.BLE,false,ti);
        assertEquals(0, layer3.getPotentialRecipientsCount());
    }
}
