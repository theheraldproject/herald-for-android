//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class DummyGPDMPLayer7Manager implements GPDMPLayer7Manager, GPDMPLayer7Incoming {
    // Incoming only
    public TargetIdentifier lastFrom;
    public UUID lastChannelIdEncoded;
    public UUID lastMessageId;
    public UInt16 lastFragmentSeqNum;
    public UInt16 lastFragmentPartialHash;
    public UInt16 lastTotalFragmentsExpected;
    public UInt16 lastFragmentsCurrentlyAvailable;
    public GPDMPLayer5MessageType lastSessionMessageType;
    public UUID lastSenderRecipientId;
    boolean lastMessageIsValid;

    // Shared
    public UUID lastChannelId;
    public Date lastTimeToAccess;
    public Date lastTimeout;
    public UInt16 lastTtl;
    public UInt16 lastMinTransmissions;
    public UInt16 lastMaxTransmissions;
    public List<ConcreteExtendedDataSectionV1> lastSections;

    // Outgoing only
    public UUID lastMySenderRecipientId;

    public GPDMPLayer6Outgoing outgoingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                         UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                         UInt16 fragmentsCurrentlyAvailable,
                         GPDMPLayer5MessageType sessionMessageType,
                         UUID senderRecipientId, boolean messageIsValid,
                         List<ConcreteExtendedDataSectionV1> sections) {

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

        // layer 6 data
        lastSenderRecipientId = senderRecipientId;
        lastMessageIsValid = messageIsValid;
        lastSections = sections;
    }

    @Override
    public void setOutgoing(GPDMPLayer6Outgoing out) {
        outgoingInterface = out;
    }

    @Override
    public GPDMPLayer7Incoming getIncomingInterface() {
        return this;
    }


    // Send outgoing utility method
    public UUID sendOutgoing(UUID channelId, Date timeToAccess, Date timeout, UInt16 ttl,
                             UInt16 minTransmissions, UInt16 maxTransmissions,
                             UUID mySenderRecipientId,
                             List<ConcreteExtendedDataSectionV1> sections) {
        lastChannelId = channelId;
        lastTimeToAccess = timeToAccess;
        lastTimeout = timeout;
        lastTtl = ttl;
        lastMinTransmissions = minTransmissions;
        lastMaxTransmissions = maxTransmissions;
        lastMySenderRecipientId = mySenderRecipientId;
        lastSections = sections;
        return outgoingInterface.outgoing(channelId,timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,mySenderRecipientId,sections);
    }

    @Override
    public void addMessageListener(UUID channelId, GPDMPMessageListener listener) {
        // Unused
    }

    @Override
    public void removeMessageListener(UUID channelId, GPDMPMessageListener listener) {
        // Unused
    }
}
