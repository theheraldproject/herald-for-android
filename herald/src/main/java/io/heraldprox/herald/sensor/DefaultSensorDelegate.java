//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.List;

/// Default implementation of SensorDelegate for making all interface methods optional.
public abstract class DefaultSensorDelegate implements SensorDelegate {

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
    }

    @Override
    public void sensor(SensorType sensor, SensorState didUpdateState) {
    }
}
