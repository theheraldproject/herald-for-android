//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.datatype.BluetoothState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BluetoothStateManager {
    Queue<BluetoothStateManagerDelegate> delegates = new ConcurrentLinkedQueue<>();

    BluetoothState state();
}
