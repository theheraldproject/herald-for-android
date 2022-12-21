//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

public interface GPDMPLayer5Manager {
    void setIncoming(GPDMPLayer6Incoming in);

    void setOutgoing(GPDMPLayer4Outgoing out);

    GPDMPLayer5Incoming getIncomingInterface();

    GPDMPLayer5Outgoing getOutgoingInterface();
}
