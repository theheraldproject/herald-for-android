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

public class DummyGPDMPMessageListener implements GPDMPMessageListener {
    public TargetIdentifier lastFrom;
    public UUID lastChannelIdEncoded;
    public Date lastTimeToAccess;
    public Date lastTimeout;
    public UInt16 lastTtl;
    public UInt16 lastMinTransmissions;
    public UInt16 lastMaxTransmissions;
    public UUID lastChannelId;
    public UUID lastMessageId;
    public UInt16 lastFragmentSeqNum;
    public UInt16 lastFragmentPartialHash;
    public UInt16 lastTotalFragmentsExpected;
    public UInt16 lastFragmentsCurrentlyAvailable;
    public GPDMPLayer5MessageType lastSessionMessageType;
    public UUID lastSenderRecipientId;
    public boolean lastMessageIsValid;
    public List<ConcreteExtendedDataSectionV1> lastSections;

    @Override
    public void received(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
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
        lastSenderRecipientId = senderRecipientId;
        lastMessageIsValid = messageIsValid;
        lastSections = sections;
    }
}
