package org.c19x.sensor.datatype;

import android.util.Base64;

/// Raw byte array data
public class Data {
    public final byte[] value;

    public Data(byte[] value) {
        this.value = value;
    }

    public String description() {
        return Base64.encodeToString(value, Base64.DEFAULT);
    }
}
