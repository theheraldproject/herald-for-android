//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt64;

public interface GPDMPLayer4Outgoing {
    /**
     * Send data outwards from Layer5 to Layer4 of GPDMP. (Segment, Datagram)
     *
     * At Layer 4 the channelId is encrypted. The message data is fragmented (if required) and
     * a message UUID is assigned and written to the output data
     *
     * @param timeToAccess The time for which the channel UUID has been encrypted
     * @param timeout The message onward transmission timeout
     * @param ttl Time to live - number of remaining hops to pass the message onto, for each next node
     * @param minTransmissions Minimum number of nodes to pass the message onto (space allowing)
     * @param maxTransmissions Maximum number of nodes to pass the message onto (before stopping)
     * @param channelId The unencrypted channelId (A valid V4 UUID)
     * @param l5SessionEncryptedData The data encrypted by this sender for transport.
     * @return The UUID for this message in this channel (A Valid V4 UUID)
     */
    UUID outgoing(Date timeToAccess, // For encryption at Layers 6, 4
                  Date timeout, // For Layer 3 to use
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions, // For Layer 3 to use
                  UUID channelId, // For encryption at Layer 4 (used as an index at Layer 5 and 6)
                  PayloadData l5SessionEncryptedData); // Encrypted data to send from Layer 5
}
