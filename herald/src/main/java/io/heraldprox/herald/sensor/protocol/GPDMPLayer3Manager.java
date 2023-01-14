//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

public interface GPDMPLayer3Manager {
    void setIncoming(GPDMPLayer4Incoming in);

    void setOutgoing(GPDMPLayer2Outgoing out);

    GPDMPLayer3Incoming getIncomingInterface();

    GPDMPLayer3Outgoing getOutgoingInterface();

    /// MARK: Layer 3 Informational calls
    int getPotentialRecipientsCount();
}
