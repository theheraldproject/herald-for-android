//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public interface GPDMPLayer6Outgoing {
    /**
     * Send data from the application (Layer 7) to the next layer down for transport (Layer 6).
     * @param channelId The channel to send the data over (Valid V4 UUID, unencrypted)
     * @param mySenderRecipientId This individual's sender/recipient ID for encryption at Layer 6
     * @param sections The PayloadData sections to join, encrypt, fragment and distribute
     * @return The UUID for this message in this channel (A Valid V4 UUID)
     */
    UUID outgoing(UUID channelId, // For Layer 4 to encrypt
                  Date timeToAccess, Date timeout, // For Layer 3 to use
                  UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions, // For Layer 3 to use
                  UUID mySenderRecipientId, // For Layer 6 to obfuscate via a partial hash
                  List<ConcreteExtendedDataSectionV1> sections); // Unencrypted data from Layer 7
}
