//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

public interface GPDMPLayer4Manager {
    void setIncoming(GPDMPLayer5Incoming in);

    void setOutgoing(GPDMPLayer3Outgoing out);

    GPDMPLayer4Incoming getIncomingInterface();

    GPDMPLayer4Outgoing getOutgoingInterface();
}
