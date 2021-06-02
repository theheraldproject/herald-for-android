//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

public interface BLEDeviceDelegate {

    void device(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute didUpdate);
}

