//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt64;

public interface GPDMPLayer5Incoming {
    /**
     * Receive parsed data from layer 4
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
     * @param l5ChannelEncryptedFragmentData The unhandled data to be assessed by layer 5
     */
    void incoming(TargetIdentifier from, // L2
                  UUID channelIdEncoded, // L3
                  Date timeToAccess, Date timeout, // L3
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions, // L3
                  UUID channelId, // L4
                  UUID messageId, // L4
                  UInt16 fragmentSeqNum, UInt16 fragmentPartialHash, // L4
                  UInt16 totalFragmentsExpected, UInt16 fragmentsCurrentlyAvailable, // L4
                  PayloadData l5ChannelEncryptedFragmentData);
}