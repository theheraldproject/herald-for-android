//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.List;

/**
 * Default implementation of SensorDelegate for making all interface methods optional.
 */
public abstract class DefaultSensorDelegate implements SensorDelegate {

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final TargetIdentifier didDetect) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, final boolean available, @NonNull final TargetIdentifier didDeleteOrDetect) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final ImmediateSendData didReceive, @NonNull final TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Location didVisit) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget, @NonNull final PayloadData withPayload) {
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final SensorState didUpdateState) {
    }
}
