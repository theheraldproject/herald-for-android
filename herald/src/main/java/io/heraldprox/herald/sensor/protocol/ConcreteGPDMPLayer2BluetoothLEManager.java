//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class ConcreteGPDMPLayer2BluetoothLEManager implements GPDMPLayer2BluetoothLEManager,
        GPDMPLayer2BluetoothLEIncoming, GPDMPLayer2BluetoothLEOutgoing {

    private GPDMPLayer1BluetoothLEOutgoing outgoingIface = null;
    private GPDMPLayer3Incoming incomingIface = null;

    /// MARK: MANAGER METHODS

    /**
     * Set the interface which this manager sends encapsulated data onto.
     * @param out
     */
    public void setOutgoing(GPDMPLayer1BluetoothLEOutgoing out) {
        outgoingIface = out;
    }

    /**
     * Set the interface which this manager passes parsed data onto.
     *
     * @param in
     */
    public void setIncoming(GPDMPLayer3Incoming in) {
        incomingIface = in;
    }

    @Override
    public GPDMPLayer2BluetoothLEIncoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer2BluetoothLEOutgoing getOutgoingInterface() {
        return this;
    }

    // MARK INCOMING METHOD

    @Override
    public void incoming(TargetIdentifier from, PayloadData data) {
        // TODO actually process data and ensure it's GPDMP relevant data
        incomingIface.incoming(from, data);
    }

    // MARK OUTGOING METHOD

    @Override
    public void outgoing(List<TargetIdentifier> mayPassOnto, PayloadData data, UUID gpdmpMessageTransportRequestId) {
        for (TargetIdentifier target : mayPassOnto) {
            outgoingIface.outgoing(target, data, gpdmpMessageTransportRequestId);
        }
    }
}
