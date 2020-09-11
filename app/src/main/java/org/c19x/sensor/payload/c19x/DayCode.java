package org.c19x.sensor.payload.c19x;

/// Day codes are published by the server to enable on-device matching in a de-centralised solution.
public class DayCode {
    public long value = 0;

    public DayCode(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DayCode{" +
                "value=" + value +
                '}';
    }
}
