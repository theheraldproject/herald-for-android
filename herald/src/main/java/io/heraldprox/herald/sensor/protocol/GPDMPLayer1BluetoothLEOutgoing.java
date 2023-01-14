//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

import java.util.UUID;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public interface GPDMPLayer1BluetoothLEOutgoing {
    void outgoing(TargetIdentifier sendTo, PayloadData data, // from Layer 2
                  UUID gpdmpMessageTransportRequestId); // From Layer 3 (for internal issue logging only)
}
