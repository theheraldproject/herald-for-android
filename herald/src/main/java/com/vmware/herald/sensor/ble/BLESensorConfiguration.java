//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import com.vmware.herald.sensor.data.SensorLoggerLevel;
import com.vmware.herald.sensor.datatype.RandomSource;
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
    /// - Switch to 16-bit UUID by setting the value xxxx in base UUID 0000xxxx-0000-1000-8000-00805F9B34FB
    public static UUID serviceUUID = UUID.fromString("428132af-4746-42d3-801e-4572d65bfd9b");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID androidSignalCharacteristicUUID = UUID.fromString("f617b813-092e-437a-8324-e09a80821a11");
    /// Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID iosSignalCharacteristicUUID = UUID.fromString("0eb0d5f2-eae4-4a9a-8af3-a4adb02d4363");
    /// Primary payload characteristic (read) for distributing payload data from peripheral to central, e.g. identity data
    /// - Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
    public final static UUID payloadCharacteristicUUID = UUID.fromString("3e98c0f8-8f05-4829-a121-43e38f8933e7");
	/// Legacy payload sharing characteristic
	public static UUID legacyPayloadCharacteristicUUID = null;
	/// A full characteristic description itself for a legacy payload
	public static BluetoothGattCharacteristic legacyPayloadCharacteristic = null;
	
    /// Standard Bluetooth service and characteristics
    /// These are all fixed UUID from the BLE standard.
    /// Standard Bluetooth Service UUID for Generic Access Service
    /// - Service UUID from BLE standard
    public final static UUID bluetoothGenericAccessServiceUUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Characteristic UUID for Generic Access Service : Device Name
    /// - Characteristic UUID from BLE standard
    public final static UUID bluetoothGenericAccessServiceDeviceNameCharacteristicUUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Characteristic UUID for Generic Access Service : Device Name
    /// - Characteristic UUID from BLE standard
    public final static UUID bluetoothGenericAccessServiceAppearanceCharacteristicUUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Service UUID for Device Information Service
    /// - Service UUID from BLE standard
    public final static UUID bluetoothDeviceInformationServiceUUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Characteristic UUID for Device Information Service : Model
    /// - Characteristic UUID from BLE standard
    public final static UUID bluetoothDeviceInformationServiceModelCharacteristicUUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Characteristic UUID for Device Information Service : Manufacturer
    /// - Characteristic UUID from BLE standard
    public final static UUID bluetoothDeviceInformationServiceManufacturerCharacteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    /// Standard Bluetooth Characteristic UUID for Device Information Service : TX Power
    /// - Characteristic UUID from BLE standard
    public final static UUID bluetoothDeviceInformationServiceTxPowerCharacteristicUUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");

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

    /// Filter duplicate payload data and suppress sensor(didRead:fromTarget) delegate calls
    /// - Set to .never to disable this feature
    /// - Set time interval N to filter duplicate payload data seen in last N seconds
    /// - Example : 60 means filter duplicates in last minute
    /// - Filters all occurrences of payload data from all targets
    public static TimeInterval filterDuplicatePayloadData = TimeInterval.never;

    /// Expiry time for shared payloads, to ensure only recently seen payloads are shared
    public static TimeInterval payloadSharingExpiryTimeInterval = new TimeInterval(5 * TimeInterval.minute.value);

    /// Advert refresh time interval
    public static TimeInterval advertRefreshTimeInterval = TimeInterval.minutes(15);

    /// Randomisation method for generating the pseudo device addresses, see PseudoDeviceAddress and RandomSource for details.
    /// - Set to Random for reliable continuous operation, validated
    /// - Other methods will cause blocking after 4-8 hours and interrupt operation on idle devices
    /// - Blocking can also occur at app initialisation, advert refresh, and also impact system services
    public static RandomSource.Method pseudoDeviceAddressRandomisation = RandomSource.Method.Random;

    /// Interrogate standard Bluetooth services to obtain device make/model data
    public static boolean deviceIntrospectionEnabled = false;

    /// Enable device filter training
    /// - Use this to gather device make/model and advert messages
    /// - Generates "filter.csv" log file for analysis
    /// - Enable device introspection to obtain device make/model data
    /// - Performs device introspection even if the device does not advertise sensor services
    /// - Triggers update every minute for each device to gather sample advert data
    /// - Disables device filter feature patterns
    public static boolean deviceFilterTrainingEnabled = false;

    /// Define device filtering rules based on message patterns
    /// - Avoids connections to devices that cannot host sensor services
    /// - Matches against every manufacturer specific data message (Hex format) in advert
    /// - Java regular expression patterns, case insensitive, find pattern anywhere in message
    /// - Remember to include ^ to match from start of message
    /// - Use deviceFilterTrainingEnabled in development environment to identify patterns
    public static String[] deviceFilterFeaturePatterns = new String[]{
            "^10....04",
            "^10....14",
            "^0100000000000000000000000000000000",
            "^05","^07","^09",
            "^00","^1002","^06","^08","^03","^0C","^0D","^0F","^0E","^0B"
    };
}


