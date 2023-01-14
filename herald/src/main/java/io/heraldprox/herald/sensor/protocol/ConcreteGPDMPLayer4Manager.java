//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.Data;
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
        // TODO create a better method for raw data to UUID
        // TODO rather than assume channelIdEncoded is unencrypted, decrypt by using timeToAccess
//        Data uuidMsb = l4Data.subdata(0, 4);
//        Data uuidLsb = l4Data.subdata(4,4);
//        String hex = "";
//        hex += uuidMsb.hexEncodedString();
//        hex += uuidLsb.hexEncodedString();
        UUID channelIdDecoded = l4Data.uuid(0);

        PayloadData l5Data = new PayloadData();
        l5Data.append(l4Data.subdata(16));

//        UUID channelIdDecoded = UUID.fromString("00000000-0000-0000-0000-000000000000");
//        UUID messageId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        UUID messageId = UUID.randomUUID();
        // TODO decode the data instead before passing on
        incomingInterface.incoming(from, channelIdEncoded, timeToAccess, timeout, ttl,
                minTransmissions, maxTransmissions, channelIdDecoded, messageId,
                // TODO decode actual sequence number, and decode actual partial hash
                new UInt16(1), new UInt16(0),
                // TODO don't just rely on one fragment
                new UInt16(1), new UInt16(1),
                l5Data);
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
        // NOTE: UNECNRYPTED PACKET FORMAT FOLLOWS:-
        // 32 bytes: channelId (IN THE PLAIN WHEN UNENCRYPTED)
        // 11+ bytes: l5 payload data (FRAGMENT)
        PayloadData l4EncData = new PayloadData();
        l4EncData.append(channelId); // TODO encode/encrypt this UUID
        l4EncData.append(l5SessionEncryptedData);
        // TODO implement fragmentation of data as necessary
        return outgoingInterface.outgoing(timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,l4EncData);
    }
}
