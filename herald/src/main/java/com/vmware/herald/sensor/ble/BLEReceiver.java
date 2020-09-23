//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.Sensor;
import com.vmware.herald.sensor.SensorDelegate;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Beacon receiver scans for peripherals with fixed service UUID.
 */
public interface BLEReceiver extends Sensor {
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
}
