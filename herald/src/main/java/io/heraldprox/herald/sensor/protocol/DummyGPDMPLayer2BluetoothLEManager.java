//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Triple;
import io.heraldprox.herald.sensor.datatype.Tuple;

public class DummyGPDMPLayer2BluetoothLEManager implements GPDMPLayer2BluetoothLEManager,
        GPDMPLayer2BluetoothLEIncoming, GPDMPLayer2BluetoothLEOutgoing {

    public GPDMPLayer1BluetoothLEOutgoing outgoingInterface = null;
    public GPDMPLayer3Incoming incomingInterface = null;
    public ArrayList<Tuple<TargetIdentifier,PayloadData>> incomingData =
            new ArrayList<Tuple<TargetIdentifier,PayloadData>>();
    public ArrayList<Triple<List<TargetIdentifier>,PayloadData,UUID>> outgoingData =
            new ArrayList<Triple<List<TargetIdentifier>,PayloadData,UUID>>();

    /// MARK: Dummy methods
    public void sendIncomingData(TargetIdentifier from, PayloadData data) {
        incomingInterface.incoming(from, data);
    }

    /// MARK: Overrides

    @Override
    public void setOutgoing(GPDMPLayer1BluetoothLEOutgoing out) {
        outgoingInterface = out;
    }

    @Override
    public void setIncoming(GPDMPLayer3Incoming in) {
        incomingInterface = in;
    }

    @Override
    public GPDMPLayer2BluetoothLEIncoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer2BluetoothLEOutgoing getOutgoingInterface() {
        return this;
    }


    @Override
    public void incoming(TargetIdentifier from, PayloadData data) {
        incomingData.add(new Tuple(from, data));
    }

    @Override
    public void outgoing(List<TargetIdentifier> mayPassOnto, PayloadData gpdmpMessageData, UUID gpdmpMessageTransportRequestId) {
        outgoingData.add(new Triple<>(mayPassOnto,gpdmpMessageData,gpdmpMessageTransportRequestId));
    }
}
