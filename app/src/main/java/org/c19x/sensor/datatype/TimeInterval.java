package org.c19x.sensor.datatype;

/// Time interval in seconds.
public class TimeInterval {
    public long value;
    public static TimeInterval hour = new TimeInterval(60 * 60);
    public static TimeInterval minute = new TimeInterval(60);

    public TimeInterval(long value) {
        this.value = value;
    }
}
