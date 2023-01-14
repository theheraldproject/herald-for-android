//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;

public class DummyGPDMPLayer4Manager implements GPDMPLayer4Manager, GPDMPLayer4Incoming, GPDMPLayer4Outgoing {
    // Incoming data only
    public TargetIdentifier lastFrom;
    public UUID lastChannelIdEncoded;
    public PayloadData lastL4Data;

    // Shared by incoming and outgoing
    public Date lastTimeToAccess;
    public Date lastTimeout;
    public UInt16 lastTtl;
    public UInt16 lastMinTransmissions;
    public UInt16 lastMaxTransmissions;

    // Holder for outgoing only data
    public UUID lastL4MessageId;
    public UUID lastChannelId;
    public PayloadData lastL5SessionEncryptedData;

    public GPDMPLayer3Outgoing outgoingInterface = null;
    public GPDMPLayer5Incoming incomingInterface = null;

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
    public void incoming(TargetIdentifier from, UUID channelIdEncoded,
                         Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions,
                         PayloadData l4Data) {
        lastFrom = from;
        lastChannelIdEncoded = channelIdEncoded;
        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastL4Data = l4Data;
    }

    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, PayloadData l5SessionEncryptedData) {
        lastL4MessageId = UUID.randomUUID();
        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastChannelId = channelId;
        lastL5SessionEncryptedData = l5SessionEncryptedData;
        return lastL4MessageId;
    }

    public void sendIncomingData(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                                 Date timeout, UInt16 ttl, UInt16 minTranmissions,
                                 UInt16 maxTransmissions, UUID channelId, UUID messageId,
                                 UInt16 fragmentSeqNum, UInt16 fragmentPartialHash,
                                 UInt16 totalFragmentsExpected, UInt16 fragmentsCurrentlyAvailable,
                                 PayloadData l5ChannelEncryptedFragmentData) {
        incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,
                minTranmissions,maxTransmissions,channelId,messageId,fragmentSeqNum,
                fragmentPartialHash,totalFragmentsExpected,fragmentsCurrentlyAvailable,
                l5ChannelEncryptedFragmentData);
    }
}
