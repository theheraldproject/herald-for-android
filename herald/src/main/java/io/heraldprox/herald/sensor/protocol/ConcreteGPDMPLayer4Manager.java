//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt64;

public class ConcreteGPDMPLayer4Manager implements GPDMPLayer4Manager, GPDMPLayer4Incoming,
        GPDMPLayer4Outgoing {
    // TODO state storage here

    private GPDMPLayer3Outgoing outgoingInterface = null;
    private GPDMPLayer5Incoming incomingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         PayloadData l4Data) {
        UUID channelIdDecoded = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID messageId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        // TODO decode the data instead before passing on
        incomingInterface.incoming(from, channelIdEncoded, timeToAccess, timeout, ttl,
                minTransmissions, maxTransmissions, channelIdDecoded, messageId,
                new UInt16(0), new UInt16(0), new UInt16(1), new UInt16(1),
                l4Data);
    }

    @Override
    public void setIncoming(GPDMPLayer5Incoming in) {
        incomingInterface = in;
    }

    @Override
    public void setOutgoing(GPDMPLayer3Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer4Incoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer4Outgoing getOutgoingInterface() {
        return this;
    }

    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl, UInt16 minTransmissions,
                         UInt16 maxTransmissions, UUID channelId,
                         PayloadData l5SessionEncryptedData) {
        PayloadData l4EncData = new PayloadData();
        // TODO encode data rather than passthrough
        return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,l5SessionEncryptedData);
    }
}
