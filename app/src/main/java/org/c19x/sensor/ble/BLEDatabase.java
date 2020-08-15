package org.c19x.sensor.ble;

import android.bluetooth.BluetoothDevice;

import org.c19x.sensor.datatype.TargetIdentifier;

import java.util.List;

/// Registry for collating sniplets of information from asynchronous BLE operations.
public interface BLEDatabase {
    /// Add delegate for handling database events
    void add(BLEDatabaseDelegate delegate);

    /// Get or create device for collating information from asynchronous BLE operations.
    BLEDevice device(BluetoothDevice bluetoothDevice);

    /// Get all devices
    List<BLEDevice> devices();

    /// Delete
    void delete(TargetIdentifier identifier);
}
