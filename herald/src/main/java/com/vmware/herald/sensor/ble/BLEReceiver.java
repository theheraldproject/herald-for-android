//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.Sensor;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Beacon receiver scans for peripherals with fixed service UUID.
 */
public interface BLEReceiver extends Sensor {
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();

    /// Immediate send data.
    boolean immediateSend(Data data, TargetIdentifier targetIdentifier);
}
