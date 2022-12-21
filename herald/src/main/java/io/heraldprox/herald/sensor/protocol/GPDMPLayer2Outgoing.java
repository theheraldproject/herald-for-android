//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public interface GPDMPLayer2Outgoing {
    /**
     * Called 1 or more times per message from Layer 3, once per encoded fragment.
     * Each call is a separate pass on to L2. Layer 3 specifies which nearby Herald nodes may be permitted to be sent the data
     * @param mayPassOnto The list of nearby Herald TargetIdentifiers that MAY be passed this information (specified by Layer 3)
     * @param gpdmpMessageData The payload data, including encrypted channel UUID, fragment numbers, ttl, access time, etc.
     * @param gpdmpMessageTransportRequestId To allow error tracking at lower layers to be reportable. Not the message UUID at Layer 4. (Layer 4 should link this ID to the message ID)
     */
    void outgoing(List<TargetIdentifier> mayPassOnto, // from Layer 3
                  PayloadData gpdmpMessageData, // from Layer 3
                  UUID gpdmpMessageTransportRequestId);
}
