//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.ble;

import com.vmware.squire.sensor.datatype.BluetoothState;

public interface BluetoothStateManagerDelegate {
    void bluetoothStateManager(BluetoothState didUpdateState);
}
