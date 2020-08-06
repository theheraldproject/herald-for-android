package org.c19x.sensor.ble;

/// Delegate for receiving registry create/update/delete events
public interface BLEDatabaseDelegate {
    void bleDatabaseDidCreate(BLEDevice device);

    void bleDatabaseDidUpdate(BLEDevice device, BLEDeviceAttribute attribute);

    void bleDatabaseDidDelete(BLEDevice device);
}
