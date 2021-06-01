//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.BluetoothState;

public interface BluetoothStateManagerDelegate {

    void bluetoothStateManager(@NonNull final BluetoothState didUpdateState);
}
