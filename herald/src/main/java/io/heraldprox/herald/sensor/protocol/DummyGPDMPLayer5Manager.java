//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;

public class DummyGPDMPLayer5Manager implements GPDMPLayer5Manager, GPDMPLayer5Incoming, GPDMPLayer5Outgoing {
    public static UUID dummyMessageId = new UUID(8,9);

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
    public UUID lastMessageId;
    public UUID lastChannelId;

    // Holder for outgoing only data
    public UInt16 lastFragmentSeqNum;
    public UInt16 lastFragmentPartialHash;
    public UInt16 lastTotalFragmentsExpected;
    public UInt16 lastFragmentsCurrentlyAvailable;
    public PayloadData lastL5ChannelEncryptedFragmentData;
    public PayloadData lastL6SenderEncryptedData;

    public GPDMPLayer4Outgoing outgoingInterface = null;
    public GPDMPLayer6Incoming incomingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions,
                         UInt16 maxTransmissions, UUID channelId, UUID messageId,
                         UInt16 fragmentSeqNum, UInt16 fragmentPartialHash,
                         UInt16 totalFragmentsExpected, UInt16 fragmentsCurrentlyAvailable,
                         PayloadData l4EncryptedFragmentData) {
        lastFrom = from;
        lastChannelIdEncoded = channelIdEncoded;
        lastL4Data = l4EncryptedFragmentData; // Note: Dummy layer so not actually encrypted

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
        lastL5ChannelEncryptedFragmentData = l4EncryptedFragmentData;

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
    public void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch, UUID remoteRecipientId) {
        // TODO use this information
    }

    @Override
    public void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch) {
        // TODO use this information
    }

    @Override
    public void addRemoteRecipientToSession(UUID channelId, UUID mySenderRecipientId, UUID remoteRecipientId) {
        // TODO use this information
    }

    @Override
    public UUID outgoing(Date timeToAccess, Date timeout, UInt16 ttl, UInt16 minTransmissions,
                         UInt16 maxTransmissions, UUID channelId, UUID senderRecipientId,
                         PayloadData l6SenderEncryptedData) {
        lastMessageId = dummyMessageId;

        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastChannelId = channelId;

        lastL6SenderEncryptedData = l6SenderEncryptedData;

        return dummyMessageId;
    }

    public void sendIncomingData(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                                 Date timeout, UInt16 ttl, UInt16 minTranmissions,
                                 UInt16 maxTransmissions, UUID channelId, UUID messageId,
                                 UInt16 fragmentSeqNum, UInt16 fragmentPartialHash,
                                 UInt16 totalFragmentsExpected, UInt16 fragmentsCurrentlyAvailable,
                                 GPDMPLayer5MessageType messageType,
                                 UUID senderRecipientId,
                                 PayloadData l6ChannelDecryptedFragmentData) {
        incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,
                minTranmissions,maxTransmissions,channelId,messageId,fragmentSeqNum,
                fragmentPartialHash,totalFragmentsExpected,fragmentsCurrentlyAvailable,
                messageType, senderRecipientId, l6ChannelDecryptedFragmentData);
    }
}
