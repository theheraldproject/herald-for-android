//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.datatype.BluetoothState;

public interface BluetoothStateManagerDelegate {
    void bluetoothStateManager(BluetoothState didUpdateState);
}
