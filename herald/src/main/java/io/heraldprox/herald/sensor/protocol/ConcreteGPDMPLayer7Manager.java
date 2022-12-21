//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.Tuple;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public class ConcreteGPDMPLayer7Manager implements GPDMPLayer7Manager, GPDMPLayer7Incoming {

    // Add multiple listeners (E.g. apps, conversations, UI channels) for this layer
    // channelId and MessageListener
    ArrayList<Tuple<UUID,GPDMPMessageListener>> channelListeners =
            new ArrayList<>();

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
        // Hand off to relevant listeners (one per channel of interest)
        for (Tuple<UUID,GPDMPMessageListener> listener : channelListeners) {
            if (listener.a == channelId) {
                ((GPDMPMessageListener)listener.b).received(from,channelIdEncoded,timeToAccess,
                        timeout,ttl,minTransmissions,maxTransmissions,channelId,messageId,
                        fragmentSeqNum,fragmentPartialHash,totalFragmentsExpected,
                        fragmentsCurrentlyAvailable,sessionMessageType,
                        senderRecipientId, messageIsValid, sections);
            }
        }
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
        return outgoingInterface.outgoing(channelId,timeToAccess,timeout,ttl,minTransmissions,
                maxTransmissions,mySenderRecipientId,sections);
    }

    // Layer7 subscriber management support

    @Override
    public void addMessageListener(UUID channelId, GPDMPMessageListener listener) {
        channelListeners.add(new Tuple<UUID, GPDMPMessageListener>(channelId,listener));
    }

    @Override
    public void removeMessageListener(UUID channelId, GPDMPMessageListener listener) {
        Iterator<Tuple<UUID,GPDMPMessageListener>> iterator = channelListeners.iterator();
        while (iterator.hasNext()) {
            Tuple<UUID,GPDMPMessageListener> tuple = iterator.next();
            if (tuple.a == channelId && tuple.b == listener) {
                iterator.remove();
            }
        }
    }

}
