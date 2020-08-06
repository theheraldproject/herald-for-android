package org.c19x.sensor.datatype;

import android.bluetooth.BluetoothDevice;

import java.util.Objects;

/// Ephemeral identifier for detected target (e.g. smartphone, beacon, place). This is likely to be an UUID but using String for variable identifier length.
public class TargetIdentifier {
    public final String value;

    public TargetIdentifier(String value) {
        this.value = value;
    }

    public TargetIdentifier(BluetoothDevice bluetoothDevice) {
        this.value = bluetoothDevice.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetIdentifier that = (TargetIdentifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
