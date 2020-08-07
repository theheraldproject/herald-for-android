package org.c19x.sensor.ble;

import org.c19x.sensor.datatype.TimeInterval;

import java.util.UUID;

/// Defines BLE sensor configuration data, e.g. service and characteristic UUIDs
public class BLESensorConfiguration {
    /**
     * Service UUID for beacon service. This is a fixed UUID to enable iOS devices to find each other even
     * in background mode. Android devices will need to find Apple devices first using the manufacturer code
     * then discover services to identify actual beacons.
     */
    public static UUID serviceUUID = UUID.fromString("FFFFFFFF-EEEE-DDDD-0000-000000000000");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    public static UUID androidSignalCharacteristicUUID = UUID.fromString("FFFFFFFF-EEEE-DDDD-0000-000000000001");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    public static UUID iosSignalCharacteristicUUID = UUID.fromString("FFFFFFFF-EEEE-DDDD-0000-000000000002");
    /// Primary payload characteristic (read) for distributing payload data from peripheral to central, e.g. identity data
    public static UUID payloadCharacteristicUUID = UUID.fromString("FFFFFFFF-EEEE-DDDD-0000-000000000003");
    /// Secondary payload characteristic (read) for sharing payload data acquired by this central, e.g. identity data of other peripherals in the vincinity
    public static UUID payloadSharingCharacteristicUUID = UUID.fromString("FFFFFFFF-EEEE-DDDD-0000-000000000004");
    /// Time delay between advert restart
    public static TimeInterval advertRestartTimeInterval = TimeInterval.minute;
    /// Time delay between payload sharing
    public static TimeInterval payloadSharingTimeInterval = TimeInterval.minute;

    // BLE advert manufacturer ID for Apple, for scanning of background iOS devices
    public static int manufacturerIdForApple = 76;
}
