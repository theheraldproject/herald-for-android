package org.c19x.sensor.datatype;

/// Sensor state
public enum SensorState {
    /// Sensor is powered on, active and operational
    on,
    /// Sensor is powered off, inactive and not operational
    off,
    /// Sensor is not available
    unavailable
}
