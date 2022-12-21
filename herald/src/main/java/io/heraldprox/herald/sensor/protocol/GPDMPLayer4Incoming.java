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

public interface GPDMPLayer4Incoming {
    /**
     * Receive parsed data from layer 3
     * @param from The nearby Herald device we received this message via (From Layer 2)
     * @param channelIdEncoded The encrypted V4 UUID representing the shared channel identifier
     * @param timeToAccess The time for which the channel UUID has been encrypted
     * @param timeout The message onward transmission timeout
     * @param ttl Time to live - number of remaining hops to pass the message onto, for each next node
     * @param minTransmissions Minimum number of nodes to pass the message onto (space allowing)
     * @param maxTransmissions Maximum number of nodes to pass the message onto (before stopping)
     * @param l4Data The unhandled data to be assessed by layer 4
     */
    void incoming(TargetIdentifier from, // L2
                  UUID channelIdEncoded,
                  Date timeToAccess, Date timeout,
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                  PayloadData l4Data);
}
