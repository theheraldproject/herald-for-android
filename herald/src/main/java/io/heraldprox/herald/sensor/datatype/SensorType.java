//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/**
 * Sensor type as qualifier for target identifier.
 */
public enum SensorType {
    // Bluetooth Low Energy (BLE)
    BLE,
    // Bluetooth Mesh
    BLMESH,
    // Awake Sensor - Used on iOS
    AWAKE,
    // GPS location sensor - Not used in Herald by default
    GPS,
    // Physical beacon, e.g. iBeacon
    BEACON,
    // Ultrasound audio beacon.
    ULTRASOUND,
    // Accelerometer motion sensor
    ACCELEROMETER,
    // Other - in case of new sensor types in use between major versions
    OTHER,
    // Sensor array consisting of multiple sensors
    ARRAY
}
