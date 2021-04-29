//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(long currentTimeMillis);
}
