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

    /// Immediate send data.
    boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier);

    // Immediate send to all (connected / recent / nearby)
    @SuppressWarnings("SameReturnValue")
    boolean immediateSendAll(@NonNull final Data data);
}
