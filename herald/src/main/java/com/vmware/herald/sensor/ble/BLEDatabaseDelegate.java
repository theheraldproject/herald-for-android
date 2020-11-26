//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

/// Delegate for receiving registry create/update/delete events
public interface BLEDatabaseDelegate {
    void bleDatabaseDidCreate(BLEDevice device);

    void bleDatabaseDidUpdate(BLEDevice device, BLEDeviceAttribute attribute);

    void bleDatabaseDidDelete(BLEDevice device);
}
