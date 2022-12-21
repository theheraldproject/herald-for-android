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

public interface GPDMPMessageListener {
    void received(TargetIdentifier from, UUID channelIdEncoded, Date timeToAccess,
                  Date timeout, UInt16 ttl, UInt16 minTransmissions, UInt16 maxTransmissions,
                  UUID channelId, UUID messageId, UInt16 fragmentSeqNum,
                  UInt16 fragmentPartialHash, UInt16 totalFragmentsExpected,
                  UInt16 fragmentsCurrentlyAvailable,
                  GPDMPLayer5MessageType sessionMessageType,
                  UUID senderRecipientId, boolean messageIsValid,
                  List<ConcreteExtendedDataSectionV1> sections);
}
