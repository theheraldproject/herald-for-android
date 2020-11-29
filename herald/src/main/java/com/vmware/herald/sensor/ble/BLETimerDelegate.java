//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

public interface BLETimerDelegate {

    void bleTimer(long currentTimeMillis);
}
