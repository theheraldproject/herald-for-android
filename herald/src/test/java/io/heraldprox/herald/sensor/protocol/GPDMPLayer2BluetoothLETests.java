//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import org.junit.Test;

// Herald imports here

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class GPDMPLayer2BluetoothLETests {
    @Test
    public void testInsecureDataReceipt() {
        // TODO fill this out properly with real test data
        // SET UP
        ConcreteGPDMPProtocolStack stack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();
        DummyGPDMPLayer3Manager layer3 =  new DummyGPDMPLayer3Manager();
        stack.replaceLayer3(layer3);

        // Ensure we're testing the right implementation (type checks for Protocol Stack)
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.ConcreteGPDMPLayer2BluetoothLEManager",
                stack.getLayer2().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer1BluetoothLEManager",
                stack.getLayer1().getClass().getName()
        );
        assertEquals(
                "io.heraldprox.herald.sensor.protocol.DummyGPDMPLayer3Manager",
                stack.getLayer3().getClass().getName()
        );
        // Now get linked layer 1 and 3 objects (for test verification)
        DummyGPDMPLayer1BluetoothLEManager layer1 = (DummyGPDMPLayer1BluetoothLEManager)stack.getLayer1();
        assertEquals(layer3,stack.getLayer3());

        // TESTS
        // Pass appropriate data to dummy Layer 1
        TargetIdentifier ti = new TargetIdentifier("00:00:00:FF:00:00");
        PayloadData fromNetwork = new PayloadData("010203040506");
        layer1.sendHeraldProtocolV1SecureCharacteristicData(ti,fromNetwork);
        // Test output at dummy layer 3
        // TODO actually get layer 2 to process something...
        assertEquals(1, layer3.incomingData.size());
        assertEquals(ti, layer3.incomingData.get(0).a);
        assertEquals(fromNetwork, layer3.incomingData.get(0).b);
        // TODO validate encoded uuid, time for access, and timeout
    }
}
