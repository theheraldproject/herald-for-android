package org.c19x.sensor.datatype;

/// Generic callback function
public interface Callback<T> {
    void accept(T value);
}
