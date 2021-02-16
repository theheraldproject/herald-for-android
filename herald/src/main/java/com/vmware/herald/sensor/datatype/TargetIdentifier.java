//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import android.bluetooth.BluetoothDevice;

import java.lang.annotation.Target;
import java.util.Objects;
import java.util.UUID;

/// Ephemeral identifier for detected target (e.g. smartphone, beacon, place).
// This is likely to be an UUID but using String for variable identifier length.
public class TargetIdentifier {
    public final String value;

    protected TargetIdentifier(final String value) {
        this.value = value;
    }

    /// Create random target identifier
    public TargetIdentifier() {
        // generated securely, see https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html#randomUUID()
        this(UUID.randomUUID().toString());
    }

    /// Create target identifier based on bluetooth device address
    public TargetIdentifier(BluetoothDevice bluetoothDevice) {
        this(bluetoothDevice.getAddress());
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
