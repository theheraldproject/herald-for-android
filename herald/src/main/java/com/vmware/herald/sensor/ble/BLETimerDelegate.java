//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(long currentTimeMillis);
}
