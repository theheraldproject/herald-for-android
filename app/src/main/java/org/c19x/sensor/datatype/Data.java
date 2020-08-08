package org.c19x.sensor.datatype;

import android.util.Base64;

import java.util.Arrays;

/// Raw byte array data
public class Data {
    public final byte[] value;

    public Data(byte[] value) {
        this.value = value;
    }

    public String base64EncodedString() {
        return Base64.encodeToString(value, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public String description() {
        return base64EncodedString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return base64EncodedString();
    }
}
