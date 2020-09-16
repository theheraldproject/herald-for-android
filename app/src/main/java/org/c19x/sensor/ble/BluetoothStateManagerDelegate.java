//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x.sensor.ble;

import org.c19x.sensor.datatype.BluetoothState;

public interface BluetoothStateManagerDelegate {
    void bluetoothStateManager(BluetoothState didUpdateState);
}
