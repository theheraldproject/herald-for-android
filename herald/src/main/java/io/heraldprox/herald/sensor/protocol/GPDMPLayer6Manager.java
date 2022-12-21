//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

public interface GPDMPLayer6Manager {
    void setIncoming(GPDMPLayer7Incoming in);

    void setOutgoing(GPDMPLayer5Outgoing out);

    GPDMPLayer6Incoming getIncomingInterface();

    GPDMPLayer6Outgoing getOutgoingInterface();
}
