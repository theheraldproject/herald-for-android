//  Copyright 2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//
package io.heraldprox.herald.sensor.protocol;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public interface GPDMPLayer1BluetoothLEIncoming {
    void incoming(TargetIdentifier from, PayloadData data);
}
