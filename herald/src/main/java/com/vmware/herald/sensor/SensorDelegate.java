//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor;

import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.SensorState;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

import java.util.List;

/// Sensor delegate for receiving sensor events.
public interface SensorDelegate {
    /// Detection of a target with an ephemeral identifier, e.g. BLE central detecting a BLE peripheral.
    void sensor(SensorType sensor, TargetIdentifier didDetect);

    /// Read payload data from target, e.g. encrypted device identifier from BLE peripheral after successful connection.
    void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget);

    /// Receive written immediate send data from target, e.g. important timing signal.
    void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget);

    /// Read payload data of other targets recently acquired by a target, e.g. Android peripheral sharing payload data acquired from nearby iOS peripherals.
    void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget);

    /// Measure proximity to target, e.g. a sample of RSSI values from BLE peripheral.
    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget);

    /// Detection of time spent at location, e.g. at specific restaurant between 02/06/2020 19:00 and 02/06/2020 21:00
    void sensor(SensorType sensor, Location didVisit);

    /// Measure proximity to target with payload data. Combines didMeasure and didRead into a single convenient delegate method
    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload);

    /// Sensor state update
    void sensor(SensorType sensor, SensorState didUpdateState);
}
