//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.Date;

public interface GPDMPLayer5Manager {
    void setIncoming(GPDMPLayer6Incoming in);

    void setOutgoing(GPDMPLayer4Outgoing out);

    GPDMPLayer5Incoming getIncomingInterface();

    GPDMPLayer5Outgoing getOutgoingInterface();

    void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch,
                       UUID remoteRecipientId);

    void createSession(UUID channelId, UUID mySenderRecipientId, Date channelEpoch);

    void addRemoteRecipientToSession(UUID channelId, UUID mySenderRecipientId,
                                     UUID remoteRecipientId);

}
