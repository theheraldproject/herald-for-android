package org.c19x.sensor.datatype;

/// Time interval in seconds.
public class TimeInterval {
    public long value;
    public static TimeInterval day = new TimeInterval(24 * 60 * 60);
    public static TimeInterval hour = new TimeInterval(60 * 60);
    public static TimeInterval minute = new TimeInterval(60);

    public TimeInterval(long seconds) {
        this.value = seconds;
    }

    public static TimeInterval hours(long hours) {
        return new TimeInterval(hour.value * hours);
    }

    public static TimeInterval minutes(long minutes) {
        return new TimeInterval(minute.value * minutes);
    }

    public static TimeInterval seconds(long seconds) {
        return new TimeInterval(seconds);
    }

    public long millis() {
        return value * 1000;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
