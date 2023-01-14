//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Ephemeral identifier for detected target (e.g. smartphone, beacon, place). This is likely
 * to be an UUID but using String for variable identifier length.
 */
public class TargetIdentifier implements Comparable<TargetIdentifier> {
    @NonNull
    public final String value;

    public TargetIdentifier(@NonNull final String value) {
        this.value = value;
    }

    /**
     * Create random target identifier
     */
    public TargetIdentifier() {
        // generated securely, see https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html#randomUUID()
        this(UUID.randomUUID().toString());
    }

    /**
     * Create target identifier based on bluetooth device address
     * @param bluetoothDevice
     */
    public TargetIdentifier(@NonNull final BluetoothDevice bluetoothDevice) {
        this(bluetoothDevice.getAddress());
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        TargetIdentifier that = (TargetIdentifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(@Nullable TargetIdentifier o) {
        final String oValue = (null == o ? null : o.value);
        return value.compareTo(oValue);
    }
}
