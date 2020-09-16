//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x.sensor.ble;

public interface BLEDeviceDelegate {
    void device(BLEDevice device, BLEDeviceAttribute didUpdate);
}

