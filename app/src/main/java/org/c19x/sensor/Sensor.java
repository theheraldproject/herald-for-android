package org.c19x.sensor;

/// Sensor for detecting and tracking various kinds of disease transmission vectors, e.g. contact with people, time at location.
public interface Sensor {
    /// Add delegate for responding to sensor events.
    void add(SensorDelegate delegate);

    /// Start sensing.
    void start();

    /// Stop sensing.
    void stop();
}
