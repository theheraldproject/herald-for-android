//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.UInt16;

public interface GPDMPLayer3Outgoing {
    /**
     * Called 1 or more times per message from Layer 4, once per encoded fragment.
     * Each call is a separate pass on to L3/L2/L1. (I.e. it's fragmented at Layer 4)
     * @param timeToAccess The time for which the channel UUID has been encrypted
     * @param timeout The message onward transmission timeout
     * @param ttl Time to live - number of remaining hops to pass the message onto, for each next node
     * @param minTransmissions Minimum number of nodes to pass the message onto (space allowing)
     * @param maxTransmissions Maximum number of nodes to pass the message onto (before stopping)
     * @param encryptedL4DataFragment The payload data, including encrypted channel UUID, fragment numbers, etc.
     * @return gpdmpMessageTransportRequestId To allow error tracking at lower layers to be reportable. Not the message UUID at Layer 4. (Layer 4 should link this ID to the message ID)
     */
    UUID outgoing(Date timeToAccess, // For encryption at Layers 6, 4
                  Date timeout, // For Layer 3 to use
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions, // For Layer 3 to use
                  PayloadData encryptedL4DataFragment); // from Layer 4
}
