//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class DummyGPDMPLayer6Manager implements GPDMPLayer6Manager, GPDMPLayer6Incoming, GPDMPLayer6Outgoing {
    // Incoming only
    public TargetIdentifier lastFrom;
    public UUID lastChannelIdEncoded;
    public UUID lastMessageId;
    public UInt16 lastFragmentSeqNum;
    public UInt16 lastFragmentPartialHash;
    public UInt16 lastTotalFragmentsExpected;
    public UInt16 lastFragmentsCurrentlyAvailable;
    public GPDMPLayer5MessageType lastSessionMessageType;
    public PayloadData lastL6ChannelDecryptedData;

    // Shared
    public Date lastTimeToAccess;
    public Date lastTimeout;
    public UInt16 lastTtl;
    public UInt16 lastMinTransmissions;
    public UInt16 lastMaxTransmissions;
    public UUID lastChannelId;

    // Outgoing only
    public UUID lastMySenderRecipientId;
    public List<ConcreteExtendedDataSectionV1> lastSections;

    public GPDMPLayer5Outgoing outgoingInterface = null;
    public GPDMPLayer7Incoming incomingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                         UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                         UInt16 fragmentsCurrentlyAvailable,
                         GPDMPLayer5MessageType sessionMessageType,
                         UUID mySenderRecipientId,
                         PayloadData l6ChannelDecryptedData) {
        lastFrom = from;
        lastChannelIdEncoded = channelIdEncoded;
        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastChannelId = channelId;
        lastMessageId = messageId;
        lastFragmentSeqNum = fragmentSeqNum;
        lastFragmentPartialHash = fragmentPartialHash;
        lastTotalFragmentsExpected = totalFragmentsExpected;
        lastFragmentsCurrentlyAvailable = fragmentsCurrentlyAvailable;
        lastSessionMessageType = sessionMessageType;
        lastMySenderRecipientId = mySenderRecipientId;
        lastL6ChannelDecryptedData = l6ChannelDecryptedData;
    }

    @Override
    public void setIncoming(GPDMPLayer7Incoming in) {
        incomingInterface = in;
    }

    @Override
    public void setOutgoing(GPDMPLayer5Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer6Incoming getIncomingInterface() {
        return this;
    }

    @Override
    public GPDMPLayer6Outgoing getOutgoingInterface() {
        return this;
    }

    @Override
    public UUID outgoing(UUID channelId, Date timeToAccess, Date timeout, UInt16 ttl,
                         UInt16 minTransmissions, UInt16 maxTransmissions, UUID mySenderRecipientId,
                         List<ConcreteExtendedDataSectionV1> sections) {
        lastChannelId = channelId;
        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastMySenderRecipientId = mySenderRecipientId;
        lastSections = sections;

        return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,null,null);
    }

    public void sendIncoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                             Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                             UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                             UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                             UInt16 fragmentsCurrentlyAvailable,
                             GPDMPLayer5MessageType sessionMessageType,
                             UUID senderRecipientId, boolean messageIsValid,
                             List<ConcreteExtendedDataSectionV1> sections) {
        incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,messageId,fragmentSeqNum,fragmentPartialHash,
                totalFragmentsExpected,fragmentsCurrentlyAvailable,sessionMessageType,
                senderRecipientId,messageIsValid,sections);
    }
}
