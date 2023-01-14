//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.protocol;

public interface GPDMPLayer1BluetoothLEManager {
    void setIncoming(GPDMPLayer2BluetoothLEIncoming in);

    GPDMPLayer1BluetoothLEOutgoing getOutgoingInterface();
}
