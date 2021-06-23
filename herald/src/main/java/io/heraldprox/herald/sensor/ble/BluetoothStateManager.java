//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.BluetoothState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BluetoothStateManager {
    @NonNull
    Queue<BluetoothStateManagerDelegate> delegates = new ConcurrentLinkedQueue<>();

    @NonNull
    BluetoothState state();
}
