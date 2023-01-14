//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

/**
 * Delegate for receiving registry create/update/delete events.
 */
public interface BLEDatabaseDelegate {

    void bleDatabaseDidCreate(@NonNull final BLEDevice device);

    void bleDatabaseDidUpdate(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute attribute);

    void bleDatabaseDidDelete(@NonNull final BLEDevice device);
}
