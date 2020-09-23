//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

public interface BLEDeviceDelegate {
    void device(BLEDevice device, BLEDeviceAttribute didUpdate);
}

