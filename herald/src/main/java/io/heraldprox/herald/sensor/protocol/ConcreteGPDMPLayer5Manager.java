//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.ArrayList;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;

public class ConcreteGPDMPLayer5Manager implements GPDMPLayer5Manager, GPDMPLayer5Incoming,
        GPDMPLayer5Outgoing {
    private GPDMPLayer4Outgoing outgoingInterface = null;
    private GPDMPLayer6Incoming incomingInterface = null;

    private ArrayList<GPDMPSession> sessions = new ArrayList<>();

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                         UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                         UInt16 fragmentsCurrentlyAvailable,
                         PayloadData l5ChannelEncryptedFragmentData) {

        GPDMPLayer5MessageType messageType = GPDMPLayer5MessageType.MESSAGE;
        UInt16 senderPartialHash = l5ChannelEncryptedFragmentData.uint16(0);
        for (GPDMPSession session: sessions) {
            System.out.println("Evaluating session for remoteRecipient: " + session.getRemoteRecipientId() +
                    " with MSB: " + ((new UInt16((int)(session.getRemoteRecipientId().getMostSignificantBits() & 0xffff)))).toString() +
                    " against partial hash: " + senderPartialHash.toString());
            if (!session.isPartialSession() && session.senderMatchesPartialHash(timeToAccess, senderPartialHash)) {
                System.out.println("Matches!");
                // We've found our session
                // If we don't recognise the sender we can't decrypt the data, so don't pass it on to layer 6 (the above if)
                PayloadData data = new PayloadData();
                data.append(session.decrypt(timeToAccess,l5ChannelEncryptedFragmentData.subdata(2)));

                incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,minTransmissions,
                        maxTransmissions,channelId,messageId,fragmentSeqNum,fragmentPartialHash,
                        totalFragmentsExpected,fragmentsCurrentlyAvailable,messageType,session.getRemoteRecipientId(),data);
            }
        }
    }

    @Override
    public void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch,
                         UUID remoteRecipientId) {
        GPDMPSession newSession = new GPDMPSession(channelId, mySenderRecipientId, channelEpoch,
                remoteRecipientId);
        sessions.add(newSession);
    }

    @Override
    public void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch) {
        GPDMPSession newSession = new GPDMPSession(channelId, mySenderRecipientId, channelEpoch);
        sessions.add(newSession);
    }

    @Override
    public void addRemoteRecipientToSession(UUID channelId, UUID mySenderRecipientId,
                                            UUID remoteRecipientId) {
        for (GPDMPSession session: sessions) {
            if (session.isPartialSession() && session.getChannelId().equals(channelId) &&
                session.getMySenderRecipientId().equals(mySenderRecipientId)) {
                session.setRemoteRecipientId(remoteRecipientId);
            }
        }
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
                         UInt16 maxTransmissions, UUID channelId, UUID mySenderRecipientId,
                         PayloadData l6SenderEncryptedData) {
        // NOTE: UNENCRYPTED PACKET FORMAT FOLLOWS
        // 8 bytes: Most Significant 64 bits of the sender UUID
        // 3+ bytes: l6 payload data
        for (GPDMPSession session: sessions) {
            if (!session.isPartialSession() &&
                    session.getMySenderRecipientId() == mySenderRecipientId) {
                PayloadData l5EncData = new PayloadData();
                l5EncData.append(session.getMySenderPartialHash(timeToAccess));
                l5EncData.append(session.encrypt(timeToAccess,l6SenderEncryptedData));

                return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                        maxTransmissions,channelId,l5EncData);
            }
        }
        return null;
    }
}
