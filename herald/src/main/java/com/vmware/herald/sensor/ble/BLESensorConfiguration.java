//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble;

import com.vmware.herald.sensor.data.SensorLoggerLevel;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.util.UUID;

/// Defines BLE sensor configuration data, e.g. service and characteristic UUIDs
public class BLESensorConfiguration {
    // MARK:- BLE service and characteristic UUID, and manufacturer ID

    /// Service UUID for beacon service. This is a fixed UUID to enable iOS devices to find each other even
    /// in background mode. Android devices will need to find Apple devices first using the manufacturer code
    /// then discover services to identify actual beacons.
    /// - Service and characteristic UUIDs are V4 UUIDs that have been randomly generated and tested
    /// for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID serviceUUID = UUID.fromString("428132af-4746-42d3-801e-4572d65bfd9b");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID androidSignalCharacteristicUUID = UUID.fromString("f617b813-092e-437a-8324-e09a80821a11");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID iosSignalCharacteristicUUID = UUID.fromString("0eb0d5f2-eae4-4a9a-8af3-a4adb02d4363");
    /// Primary payload characteristic (read) for distributing payload data from peripheral to central, e.g. identity data
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID payloadCharacteristicUUID = UUID.fromString("3e98c0f8-8f05-4829-a121-43e38f8933e7");
    /// Manufacturer data is being used on Android to store pseudo device address
    /// - Pending update to dedicated ID
    public final static int manufacturerIdForSensor = 65530;
    /// BLE advert manufacturer ID for Apple, for scanning of background iOS devices
    public final static int manufacturerIdForApple = 76;

    // MARK:- BLE signal characteristic action codes

    /// Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload data length, then payload data
    public final static byte signalCharacteristicActionWritePayload = (byte) 1;
    /// Signal characteristic action code for write RSSI, expect 1 byte action code followed by 4 byte little-endian Int32 integer value for RSSI value
    public final static byte signalCharacteristicActionWriteRSSI = (byte) 2;
    /// Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload sharing data length, then payload sharing data
    public final static byte signalCharacteristicActionWritePayloadSharing = (byte) 3;
    /// Arbitrary immediate write
    public final static byte signalCharacteristicActionWriteImmediate = (byte) 4;

    // MARK:- App configurable BLE features

    /// Log level for BLESensor
    public static SensorLoggerLevel logLevel = SensorLoggerLevel.debug;

    /// Payload update at regular intervals, in addition to default HERALD communication process.
    /// - Use this to enable regular payload reads according to app payload lifespan.
    /// - Set to .never to disable this function.
    /// - Payload updates are reported to SensorDelegate as didRead.
    /// - Setting take immediate effect, no need to restart BLESensor, can also be applied while BLESensor is active.
    public static TimeInterval payloadDataUpdateTimeInterval = TimeInterval.never;

    /// Expiry time for shared payloads, to ensure only recently seen payloads are shared
    public static TimeInterval payloadSharingExpiryTimeInterval = new TimeInterval(5 * TimeInterval.minute.value);

    /// Advert refresh time interval
    public static TimeInterval advertRefreshTimeInterval = TimeInterval.minutes(15);
}
