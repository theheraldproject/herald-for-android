//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

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

    public int compare(@NonNull final BLEDevice a, @NonNull final BLEDevice b) {
        // BLEDevice.lastUpdatedAt is @NonNull
        // This is optional logic if BLEDevice.lastUpdatedAt becomes @Nullable
//        if (null == a.lastUpdatedAt && null == b.lastUpdatedAt) {
//            return 0;
//        } else if (null == a.lastUpdatedAt && null != b.lastUpdatedAt) {
//            return 1;
//        } else if (null != a.lastUpdatedAt && null == b.lastUpdatedAt) {
//            return -1;
//        }
        // Descending order of last updated at (hence reversed logic)
        final long bt = b.lastUpdatedAt.getTime();
        final long at = a.lastUpdatedAt.getTime();
        if (bt > at) {
            return 1;
        }
        if (bt < at) {
            return -1;
        }
        return 0;
    }
}
