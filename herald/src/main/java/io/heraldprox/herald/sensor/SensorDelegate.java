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
 * Sensor delegate for receiving sensor events.
 */
public interface SensorDelegate {
    /**
     * Detection of a target with an ephemeral identifier, e.g. BLE central detecting a BLE peripheral.
     * @param sensor
     * @param didDetect
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final TargetIdentifier didDetect);

    /**
     * Indicates whether a device has dropped out of being accessible (E.g. removed from BLEDatabase)
     * @param sensor The sensor type detected (available AND Herald device) or deleted (unavailable)
     * @param available Whether the device is now available to communicate with
     * @param didDeleteOrDetect The unique identified of this target via this sensor type
     */
    void sensor(@NonNull final SensorType sensor, final boolean available, @NonNull final TargetIdentifier didDeleteOrDetect);

    /**
     * Read payload data from target, e.g. encrypted device identifier from BLE peripheral after successful connection.
     * @param sensor
     * @param didRead
     * @param fromTarget
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget);

    /**
     * Receive written immediate send data from target, e.g. important timing signal.
     * @param sensor
     * @param didReceive
     * @param fromTarget
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final ImmediateSendData didReceive, @NonNull final TargetIdentifier fromTarget);

    /**
     * Read payload data of other targets recently acquired by a target, e.g. Android peripheral sharing
     * payload data acquired from nearby iOS peripherals.
     * @param sensor
     * @param didShare
     * @param fromTarget
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget);

    /**
     * Measure proximity to target, e.g. a sample of RSSI values from BLE peripheral.
     * @param sensor
     * @param didMeasure
     * @param fromTarget
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget);

    /**
     * Detection of time spent at location, e.g. at specific restaurant between 02/06/2020 19:00 and 02/06/2020 21:00
     * @param sensor
     * @param didVisit
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final Location didVisit);

    /**
     * Measure proximity to target with payload data. Combines didMeasure and didRead into a single convenient delegate method
     * @param sensor
     * @param didMeasure
     * @param fromTarget
     * @param withPayload
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget, @NonNull final PayloadData withPayload);

    /**
     * Sensor state update
     * @param sensor
     * @param didUpdateState
     */
    void sensor(@NonNull final SensorType sensor, @NonNull final SensorState didUpdateState);
}
