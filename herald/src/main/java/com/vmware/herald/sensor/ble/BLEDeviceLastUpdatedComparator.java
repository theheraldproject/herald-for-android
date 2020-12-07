//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

import java.util.Comparator;

/**
 * Orders a set of BLEDevices such that they are in descending order of BLEDevice.lastUpdatedAt().
 * This effectively orders them such that the first element is the one most recently connected to,
 * discovered, or to last had it's RSSI updated.
 *
 * Note: Be sure to pre-filter your sorted set by using !device.ignore() to prevent invalid
 * devices from receiving requests if using this for methods like immediateSendAll()
 */
public class BLEDeviceLastUpdatedComparator implements Comparator<BLEDevice> {
    public int compare(BLEDevice a, BLEDevice b)
    {
        // Descending order of last updated at (hence reversed logic)
        long bt = b.lastUpdatedAt.getTime();
        long at = a.lastUpdatedAt.getTime();
        if (bt > at) {
            return 1;
        }
        if (bt < at) {
            return -1;
        }
        return 0;
    }
}
