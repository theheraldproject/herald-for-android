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

public class ConcreteGPDMPLayer5Manager implements GPDMPLayer5Manager, GPDMPLayer5Incoming,
        GPDMPLayer5Outgoing {
    // TODO state storage here

    private GPDMPLayer4Outgoing outgoingInterface = null;
    private GPDMPLayer6Incoming incomingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                         UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                         UInt16 fragmentsCurrentlyAvailable,
                         PayloadData l5ChannelEncryptedFragmentData) {

        GPDMPLayer5MessageType messageType = GPDMPLayer5MessageType.MESSAGE;
        // TODO decode rather than pass through
//        PayloadData data = new PayloadData();
        PayloadData data = l5ChannelEncryptedFragmentData;

        // TODO decode the data instead before passing on
        incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,messageId,fragmentSeqNum,fragmentPartialHash,
                totalFragmentsExpected,fragmentsCurrentlyAvailable,messageType,data);
    }

    @Override
    public void setIncoming(GPDMPLayer6Incoming in) {
        incomingInterface = in;
    }

    @Override
    public void setOutgoing(GPDMPLayer4Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer5Incoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer5Outgoing getOutgoingInterface() {
        return this;
    }

    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl, UInt16 minTransmissions,
                         UInt16 maxTransmissions, UUID channelId, UInt16 senderPartialHash,
                         PayloadData l6SenderEncryptedData) {
        PayloadData l5EncData = new PayloadData();
        // TODO encode data rather than passthrough
        return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,l6SenderEncryptedData);
    }
}
