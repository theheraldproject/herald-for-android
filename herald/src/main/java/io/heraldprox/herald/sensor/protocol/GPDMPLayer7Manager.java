//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.List;
import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;

public interface GPDMPLayer7Manager extends GPDMPMessageListenerManager {
    void setOutgoing(GPDMPLayer6Outgoing out);

    GPDMPLayer7Incoming getIncomingInterface();

    UUID sendOutgoing(UUID channelId, Date timeToAccess, Date timeout, UInt16 ttl,
                      UInt16 minTransmissions, UInt16 maxTransmissions,
                      UUID mySenderRecipientId,
                      List<ConcreteExtendedDataSectionV1> sections);
}
