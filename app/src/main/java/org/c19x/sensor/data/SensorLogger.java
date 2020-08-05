package org.c19x.sensor.data;

public interface SensorLogger {
    void log(SensorLoggerLevel level, String message, final Object... values);

    void debug(String message, final Object... values);

    void info(String message, final Object... values);

    void fault(String message, final Object... values);
}
