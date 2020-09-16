//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x.sensor;

import org.c19x.sensor.datatype.Location;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.SensorState;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.TargetIdentifier;

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
