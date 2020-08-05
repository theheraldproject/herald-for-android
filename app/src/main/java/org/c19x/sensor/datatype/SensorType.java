package org.c19x.sensor.datatype;

/// Sensor type as qualifier for target identifier.
public enum SensorType {
    /// Bluetooth Low Energy (BLE)
    /// GPS location sensor
    GPS,
    /// Physical beacon, e.g. iBeacon
    BEACON,
    /// Ultrasound audio beacon.
    ULTRASOUND
}
