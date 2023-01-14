//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;

/**
 * Acts as a dummy Bluetooth Low Energy Layer 1, allowing testing of Layer2+.
 * @since 22 May 2022
 * @author Adam Fowler &lt;adam@adamfowler.org&gt;
 */
public class DummyGPDMPLayer1BluetoothLEManager implements GPDMPLayer1BluetoothLEManager, GPDMPLayer1BluetoothLEOutgoing {
    private GPDMPLayer2BluetoothLEIncoming layer2 = null;
    public ArrayList<Tuple<TargetIdentifier,PayloadData>> outgoingData = new ArrayList<>();

    public Hashtable<TargetIdentifier, DummyGPDMPLayer1BluetoothLEManager> nearby = new Hashtable<>();

    public TargetIdentifier identifier;

    public DummyGPDMPLayer1BluetoothLEManager(TargetIdentifier me) {
        identifier = me;
    }

    public DummyGPDMPLayer1BluetoothLEManager() {
        identifier = new TargetIdentifier(); // Random identifier - fine for a mock
    }

    /// MARK DUMMY METHODS THAT MOCK PHYSICAL BLE NETWORK ACTIVITY

    public void sendConnectionInitiated(TargetIdentifier from) {
        // Null op for now
    }

    public void sendConnectionTerminated(TargetIdentifier from) {
        // Null op for now
    }

    /**
     * The method to call when this mock (Bluetooth) device receives data
     *
     * @param from The remote device sending us GPDMP data
     * @param sent The GPDMP payload data we've received
     */
    public void sendHeraldProtocolV1SecureCharacteristicData(TargetIdentifier from, PayloadData sent) {
        layer2.incoming(from, sent);
    }

    /// MARK METHODS USED TO ENSURE CORRECT FUNCTION IN TESTING

    public ArrayList<Tuple<TargetIdentifier,PayloadData>> getOutgoingData() {
        return outgoingData;
    }

    public void resetOutgoingData() {
        outgoingData.clear();
    }

    /// MARK Manager methods for Layer 1
    @Override
    public void setIncoming(GPDMPLayer2BluetoothLEIncoming in) {
        layer2 = in;
    }

    @Override
    public GPDMPLayer1BluetoothLEOutgoing getOutgoingInterface() {
        return this;
    }

    // Layer 1 outgoing as invoked by Layer 2 layer
    public void outgoing(TargetIdentifier sendTo, PayloadData data, UUID gpdmpMessageTransportRequestId) {
        outgoingData.add(new Tuple<>(sendTo,data));
        if (nearby.containsKey(sendTo)) {
            nearby.get(sendTo).sendHeraldProtocolV1SecureCharacteristicData(identifier,data);
        }
    }

    /// MARK Mock methods to allow multiple virtual Bluetooth node testing
    public void advertisementSeen(TargetIdentifier target,DummyGPDMPLayer1BluetoothLEManager mockInterface) {
        nearby.put(target,mockInterface);
    }

    public void advertisementCeases(TargetIdentifier target) {
        nearby.remove(target);
    }

}
