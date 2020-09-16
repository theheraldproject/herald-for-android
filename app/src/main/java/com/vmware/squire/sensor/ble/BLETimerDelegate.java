//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(long currentTimeMillis);
}
