//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.Sensor;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Beacon receiver scans for peripherals with fixed service UUID.
 */
public interface BLEReceiver extends Sensor {
    @NonNull
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();

    /**
     * Immediate send data to target device.
     * @param data Data to be sent immediately
     * @param targetIdentifier Target device
     * @return True on success, false otherwise
     */
    boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier);

    /**
     * Immediate send to all (connected / recent / nearby) devices.
     * @param data Data to be sent immediately
     * @return True on success, false otherwise
     */
    @SuppressWarnings("SameReturnValue")
    boolean immediateSendAll(@NonNull final Data data);
}
