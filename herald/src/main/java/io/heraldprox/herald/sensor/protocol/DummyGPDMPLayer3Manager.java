//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.ArrayList;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.datatype.UInt16;

public class DummyGPDMPLayer3Manager implements GPDMPLayer3Manager, GPDMPLayer3Incoming, GPDMPLayer3Outgoing {
    public ArrayList<Tuple<TargetIdentifier,PayloadData>> incomingData =
            new ArrayList<Tuple<TargetIdentifier,PayloadData>>();

    public GPDMPLayer2Outgoing outgoingInterface = null;
    public GPDMPLayer4Incoming incomingInterface = null;

    /// MARK: Dummy methods
    public void sendIncomingData(TargetIdentifier from, UUID channelId, Date timeToAccess,
                                 Date timeout, UInt16 ttl, UInt16 minTransmissions,
                                 UInt16 maxTransmissions, PayloadData data) {
        incomingInterface.incoming(from, channelId, timeToAccess, timeout, ttl, minTransmissions,
                maxTransmissions, data);
    }

    /// MARK Manager methods for layer 3

    @Override
    public void setIncoming(GPDMPLayer4Incoming in) { incomingInterface = in; }

    @Override
    public void setOutgoing(GPDMPLayer2Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer3Incoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer3Outgoing getOutgoingInterface() { return this; }

    @Override
    public int getPotentialRecipientsCount() {
        return 0;
    }

    @Override
    public void incoming(TargetIdentifier from,  PayloadData data) {
        incomingData.add(new Tuple(from, data));
    }

    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions,
                         PayloadData encryptedL4DataFragment) {
        return new UUID(5,6);
    }
}
