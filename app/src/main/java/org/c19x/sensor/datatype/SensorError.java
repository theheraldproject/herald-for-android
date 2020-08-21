package org.c19x.sensor.datatype;

/// Sensor error
public class SensorError {
    public final String description;

    public SensorError(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "SensorError[" + description + "]";
    }
}
