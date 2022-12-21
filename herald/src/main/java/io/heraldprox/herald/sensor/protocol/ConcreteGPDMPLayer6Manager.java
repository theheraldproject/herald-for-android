//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class ConcreteGPDMPLayer6Manager  implements GPDMPLayer6Manager, GPDMPLayer6Incoming,
        GPDMPLayer6Outgoing {
    // TODO state storage here

    private GPDMPLayer5Outgoing outgoingInterface = null;
    private GPDMPLayer7Incoming incomingInterface = null;

    @Override
    public void incoming(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                         Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                         UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                         UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                         UInt16 fragmentsCurrentlyAvailable,
                         GPDMPLayer5MessageType sessionMessageType,
                         PayloadData l6ChannelDecryptedData) {

        UInt16 hash = new UInt16(0);
        UUID senderRecipientID = UUID.fromString("0000-0000-0000-00000000-000000000000");

        // TODO decode rather than pass through
        boolean valid = true;
        List sections = new ArrayList<ConcreteExtendedDataSectionV1>();
        sections.add(new ConcreteExtendedDataSectionV1(
                l6ChannelDecryptedData.uint8(0),
                l6ChannelDecryptedData.uint8(1),
                l6ChannelDecryptedData.subdata(2)
        ));

        // TODO decode the data instead before passing on
        incomingInterface.incoming(from,channelIdEncoded,timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,messageId,fragmentSeqNum,fragmentPartialHash,
                totalFragmentsExpected,fragmentsCurrentlyAvailable,sessionMessageType,
                hash, senderRecipientID, valid, sections);
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
        // TODO encode this properly, not just appending raw data
        PayloadData l6EncData = new PayloadData();
        for (ConcreteExtendedDataSectionV1 section : sections) {
            l6EncData.append(section.code);
            l6EncData.append(section.length);
            l6EncData.append(section.data);
        }
        // TODO calculate 256 bit (32 byte) hash, then pass first 16 bytes
        UInt16 hash = new UInt16(1);
        return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,channelId,hash,l6EncData);
    }
}
