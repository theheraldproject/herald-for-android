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

public interface GPDMPLayer7Incoming {
    /**
     * Receive parsed data from layer 6
     * @param from The nearby Herald device we received this message via (From Layer 2)
     * @param channelIdEncoded The encrypted V4 UUID representing the shared channel identifier
     * @param timeToAccess The time for which the channel UUID has been encrypted
     * @param timeout The message onward transmission timeout
     * @param ttl Time to live - number of remaining hops to pass the message onto, for each next node
     * @param minTransmissions Minimum number of nodes to pass the message onto (space allowing)
     * @param maxTransmissions Maximum number of nodes to pass the message onto (before stopping)
     * @param channelId The unencrypted channelId (A valid V4 UUID)
     * @param messageId The message UUID generated by layer 4 on the sender (A valid V4 UUID)
     * @param fragmentSeqNum This fragment's sequence number
     * @param fragmentPartialHash This fragment's partial hash (i.e. half the hash of this fragment)
     * @param totalFragmentsExpected Metadata from this or previous fragments, total fragments expected
     * @param fragmentsCurrentlyAvailable Metadata from all fragments, fragments now available
     * @param sessionMessageType The message type as assessed by Layer5 (Only Message type need passing on)
     * @param senderPartialHash The partial hash to help identify the sender's recipient ID on this channel
     * @param senderRecipientId The full recipientId (A valid V4 UUID) to allow replies.
     * @param messageIsValid If the message is correctly decrypted and is valid (i.e. we have the key)
     * @param sections The payload data sections decoded from the fragments in this message
     */
    void incoming(TargetIdentifier from, // L2
                  UUID channelIdEncoded, // L3
                  Date timeToAccess, Date timeout, // L3
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions, // L3
                  UUID channelId, // L4
                  UUID messageId, // L4
                  UInt16 fragmentSeqNum, UInt16 fragmentPartialHash, // L4
                  UInt16 totalFragmentsExpected, UInt16 fragmentsCurrentlyAvailable, // L4
                  GPDMPLayer5MessageType sessionMessageType, // L5 - Not used today. May be used for joining channels or group DH in future
                  UInt16 senderPartialHash, UUID senderRecipientId, // L6
                  boolean messageIsValid, // L6
                  List<ConcreteExtendedDataSectionV1> sections);
}
