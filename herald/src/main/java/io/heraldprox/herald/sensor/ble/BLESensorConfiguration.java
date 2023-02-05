//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.SensorLoggerLevel;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.random.NonBlockingSecureRandom;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.UUID;

/**
 * Defines BLE sensor configuration data, e.g. service and characteristic UUIDs
 */
@SuppressWarnings("CanBeFinal")
public class BLESensorConfiguration {
    // MARK:- BLE service and characteristic UUID, and manufacturer ID

    /**
     * Service UUID for beacon service. This is a fixed UUID to enable iOS devices to find each other even
     *  in background mode. Android devices will need to find Apple devices first using the manufacturer code
     *  then discover services to identify actual beacons.
     *  <br>- Service and characteristic UUIDs are V4 UUIDs that have been randomly generated and tested
     *    for uniqueness by conducting web searches to ensure it returns no results.
     *    Default UUID for HERALD is 428132af-4746-42d3-801e-4572d65bfd9b
     *  <br>
     *  <br>- Switch to 16-bit UUID by setting the value xxxx in base UUID 0000xxxx-0000-1000-8000-00805F9B34FB
     */
    @NonNull
    public final static UUID legacyHeraldServiceUUID = UUID.fromString("428132af-4746-42d3-801e-4572d65bfd9b");
    public static boolean legacyHeraldServiceDetectionEnabled = true;

    /**
     * Service UUID for beacon service. This is a fixed UUID to enable iOS devices to find each other even
     *  in background mode. Android devices will need to find Apple devices first using the manufacturer code
     *  then discover services to identify actual beacons.
     *  <br>- Service and characteristic UUIDs are V4 UUIDs that have been randomly generated and tested
     *    for uniqueness by conducting web searches to ensure it returns no results.
     *    Default UUID for HERALD from v2.1.0 is the Linux Foundation id: 0000FCF6-0000-1000-8000-00805F9B34FB.
     *    Default UUID for HERALD prior to v2.1.0 is 428132af-4746-42d3-801e-4572d65bfd9b.
     *  <br>
     *  <br>- Switch to 16-bit UUID by setting the value xxxx in base UUID 0000xxxx-0000-1000-8000-00805F9B34FB
     */
    @NonNull
    public final static UUID linuxFoundationServiceUUID = UUID.fromString("0000FCF6-0000-1000-8000-00805F9B34FB");

    /**
     * Enables detection of the current standard Herald service UUID.
     * Enabled by default
     * @since v2.2 February 2023
     */
    public static boolean standardHeraldServiceDetectionEnabled = true;
    /**
     * Enables advertising of the current standard Herald service UUID.
     * Enabled by default
     * @since v2.2 February 2023
     */
    public static boolean standardHeraldServiceAdvertisingEnabled = true;

    /**
     * Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
     * <br>- Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
     */
    @NonNull
    public final static UUID androidSignalCharacteristicUUID = UUID.fromString("f617b813-092e-437a-8324-e09a80821a11");

    /**
     *  Signaling characteristic for controlling connection between peripheral and central, e.g. keep each other from suspend state
     *  <br>- Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
     */
    @NonNull
    public final static UUID iosSignalCharacteristicUUID = UUID.fromString("0eb0d5f2-eae4-4a9a-8af3-a4adb02d4363");
    /**
     *  Primary payload characteristic (read) for distributing payload data from peripheral to central, e.g. identity data
     *  <br>- Characteristic UUID is randomly generated V4 UUIDs that has been tested for uniqueness by conducting web searches to ensure it returns no results.
     */
    @NonNull
    public final static UUID payloadCharacteristicUUID = UUID.fromString("3e98c0f8-8f05-4829-a121-43e38f8933e7");

    // MARK:- Custom Service UUID interoperability - Since v2.2
    /**
     * A custom service UUID to use for a Herald service. Required for custom apps (without Herald interop).
     *
     * @since v2.2 February 2023
     * @note Requires customHeraldServiceDetectionEnabled to be set to true to enable.
     */
    @Nullable
    public static UUID customServiceUUID = null;
    /**
     * Whether to detect a custom service UUID. Disabled by default.
     * Doesn't affect advertising.
     * in preference to the default Linux Foundation Herald UUID if specified.
     * Only takes effect if customServiceUUID is set to a valid non-null UUID.
     *
     * @since v2.2 February 2023
     */
    public static boolean customServiceDetectionEnabled = false;
    /**
     * Whether to advertise using the main customServiceUUID instead of the standard Herald
     * Service UUID.
     *
     * @since v2.2 February 2023
     */
    public static boolean customServiceAdvertisingEnabled = false;
    /**
     * Additional UUIDs beyond just customServiceUUID to detect. Useful for 'legacy' custom
     * application detections. You do not have to include customServiceUUID in this list.
     *
     * @since v2.2 February 2023
     * @note Requires customHeraldServiceDetectionEnabled to be set to true to enable.
     */
    @Nullable
    public static UUID[] customAdditionalServiceUUIDs = null;
    /**
     * The custom manufacturer ID to use. Note this MUST be a Bluetooth SIG registered ID to
     * ensure there is no interference.
     * Note that if this is not specified, then the default Linux Foundation Herald service
     * manufacturer ID will be used.
     *
     * @since v2.2 February 2023
     * @note Requires customHeraldServiceDetectionEnabled to be set to true to enable.
     * @note Requires pseudoDeviceAddress to be enabled.
     */
    static int customManufacturerIdForSensor = 0;

    // MARK:- Interoperability with OpenTrace

    /**
     *  OpenTrace service UUID, characteristic UUID, and manufacturer ID
     *  <br>- Enables capture of OpenTrace payloads, e.g. for transition to HERALD
     *  <br>- HERALD will discover devices advertising OpenTrace service UUID (can be the same as HERALD service UUID)
     *  <br>- HERALD will search for OpenTrace characteristic, write payload of self to target,
     *    read payload from target, and capture payload written to self by target.
     *  <br>- HERALD will read/write payload from/to OpenTrace at regular intervals if update time
     *    interval is not .never. Tests have confirmed that using this feature, instead of relying
     *    solely on OpenTrace advert updates on idle Android and iOS devices offers more
     *    regular measurements for OpenTrace.
     *  <br>- OpenTrace payloads will be reported via SensorDelegate:didRead where the payload
     *    has type LegacyPayloadData, and service will be the OpenTrace characteristic UUID.
     *  <br>- Set interopOpenTraceEnabled = false to disable feature
     */
    public static boolean interopOpenTraceEnabled = false;
    @NonNull
    public static UUID interopOpenTraceServiceUUID = UUID.fromString("A6BA4286-C550-4794-A888-9467EF0B31A8");
	@NonNull
    public static UUID interopOpenTracePayloadCharacteristicUUID = UUID.fromString("D1034710-B11E-42F2-BCA3-F481177D5BB2");
    public static int interopOpenTraceManufacturerId = 1023;
    @NonNull
    public static TimeInterval interopOpenTracePayloadDataUpdateTimeInterval = TimeInterval.minutes(5);

    // MARK:- Interoperability with Advert based protocols

    /**
     *  Advert based protocol service UUID, service data key
     *  <br>- Enable capture of advert based protocol payloads, e.g. for transition to HERALD
     *  <br>- HERALD will discover devices advertising protocol service UUID (can be the same as HERALD service UUID)
     *  <br>- HERALD will parse service data to read payload from target
     *  <br>- Protocol payloads will be reported via SensorDelegate:didRead where the payload
     *    has type LegacyPayloadData, and service will be the protocol service UUID.
     *  <br>- Set interopAdvertBasedProtocolEnabled = false to disable feature
     *  <br>- Scan for 16-bit service UUID by setting the value xxxx in base UUID 0000xxxx-0000-1000-8000-00805F9B34FB
     */
    public static boolean interopAdvertBasedProtocolEnabled = false;
    @NonNull
    public static UUID interopAdvertBasedProtocolServiceUUID = UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB");
    @NonNull
    public static Data interopAdvertBasedProtocolServiceDataKey = Data.fromHexEncodedString("FD6F");


    /**
     *  Standard Bluetooth service and characteristics
     *  These are all fixed UUID from the BLE standard.
     *  Standard Bluetooth Service UUID for Generic Access Service
     *  <br>- Service UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothGenericAccessServiceUUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Characteristic UUID for Generic Access Service : Device Name
     *  <br>- Characteristic UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothGenericAccessServiceDeviceNameCharacteristicUUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Characteristic UUID for Generic Access Service : Device Name
     *  <br>- Characteristic UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothGenericAccessServiceAppearanceCharacteristicUUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Service UUID for Device Information Service
     *  <br>- Service UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothDeviceInformationServiceUUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Characteristic UUID for Device Information Service : Model
     *  <br>- Characteristic UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothDeviceInformationServiceModelCharacteristicUUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Characteristic UUID for Device Information Service : Manufacturer
     *  <br>- Characteristic UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothDeviceInformationServiceManufacturerCharacteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    /**
     *  Standard Bluetooth Characteristic UUID for Device Information Service : TX Power
     *  <br>- Characteristic UUID from BLE standard
     */
    @NonNull
    public final static UUID bluetoothDeviceInformationServiceTxPowerCharacteristicUUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");

    /**
     *  The pre v2.1.0 Manufacturer data is being used on Android to store pseudo device address
     *  <br>- Was updated from this value in v2.1.0 of Herald. @see linuxFoundationManufacturerIdForSensor
     */
    public final static int legacyHeraldManufacturerIdForSensor = 65530;

    /**
     *  Manufacturer data is being used on Android to store pseudo device address
     *  <br>- Changed in v2.1.0 to 1521 (aka 0x05f1) - the Linux Foundation manufacturer ID
     */
    public final static int linuxFoundationManufacturerIdForSensor = 1521; // aka 0x05F1

    /**
     *  BLE advert manufacturer ID for Apple, for scanning of background iOS devices
     */
    public final static int manufacturerIdForApple = 76;

    // MARK:- BLE signal characteristic action codes

    /**
     *  Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload data length, then payload data
     */
    public final static byte signalCharacteristicActionWritePayload = (byte) 1;
    /**
     *  Signal characteristic action code for write RSSI, expect 1 byte action code followed by 4 byte little-endian Int32 integer value for RSSI value
     */
    public final static byte signalCharacteristicActionWriteRSSI = (byte) 2;
    /**
     *  Signal characteristic action code for write payload, expect 1 byte action code followed by 2 byte little-endian Int16 integer value for payload sharing data length, then payload sharing data
     */
    public final static byte signalCharacteristicActionWritePayloadSharing = (byte) 3;
    /**
     *  Arbitrary immediate write
     */
    public final static byte signalCharacteristicActionWriteImmediate = (byte) 4;

    // MARK:- App configurable BLE features

    /**
     *  Log level for BLESensor
     */
    @NonNull
    public static SensorLoggerLevel logLevel = SensorLoggerLevel.debug;

    /**
     *  Payload update at regular intervals, in addition to default HERALD communication process.
     *  <br>- Use this to enable regular payload reads according to app payload lifespan.
     *  <br>- Set to .never to disable this function.
     *  <br>- Payload updates are reported to SensorDelegate as didRead.
     *  <br>- Setting take immediate effect, no need to restart BLESensor, can also be applied while BLESensor is active.
     */
    @NonNull
    public static TimeInterval payloadDataUpdateTimeInterval = TimeInterval.never;

    /**
     *  Filter duplicate payload data and suppress sensor(didRead:fromTarget) delegate calls
     *  <br>- Set to .never to disable this feature
     *  <br>- Set time interval N to filter duplicate payload data seen in last N seconds
     *  <br>- Example : 60 means filter duplicates in last minute
     *  <br>- Filters all occurrences of payload data from all targets
     */
    @NonNull
    public static TimeInterval filterDuplicatePayloadData = TimeInterval.never;

    /**
     *  Expiry time for shared payloads, to ensure only recently seen payloads are shared
     */
    @NonNull
    public static TimeInterval payloadSharingExpiryTimeInterval = new TimeInterval(5 * TimeInterval.minute.value);

    /**
     *  Advert refresh time interval
     */
    @NonNull
    public static TimeInterval advertRefreshTimeInterval = TimeInterval.minutes(15);

    /**
     *  Randomisation method for generating the pseudo device addresses, see PseudoDeviceAddress and RandomSource for details.
     *  <br>- Set to NonBlockingSecureRandom for reliable continuous operation, validated
     *  <br>- Other methods will cause blocking after 4-8 hours and interrupt operation on idle devices
     *  <br>- Blocking can also occur at app initialisation, advert refresh, and also impact system services
     */
    @NonNull
    public static RandomSource pseudoDeviceAddressRandomisation = new NonBlockingSecureRandom();

    /**
     * Determines whether Android devices advertise a pseudo random Bluetooth MAC address
     * equivalent in their manufacturer advertising data.
     * <br>This is required for some older (Pre Android 10) devices, and some manufacturers devices
     * (Huawei, or Samsung's older Exynos chipset phones) to be detected without being constantly
     * asked for their Herald Payload. Without this enabled those devices will have poorer
     * performance.
     * <br><br>
     * This was introduced in v2.1.0-beta4 as a controllable flag. Prior to this the feature was
     * always enabled. Additional Pseudo mac logic and read logic on iOS was added in the v2.1.0
     * release such that this is only required on very old/misbehaving Android sets. It has been
     * decided to DISABLE this from v2.1.0 onwards. If required, extra logic will be added to Herald
     * to detect on an Android device when it is 'misbehaving' by rotating it's MAC too much, and
     * if this is the case, this flag will be dynamically enabled for just that device.
     * <br><br>
     * This is to reduce cross talk. It appears Apple's iOS now always assigns two 'Peripheral ID'
     * UUIDs to every remote device, even if they only advertise one Bluetooth MAC address.
     * This is a peculiarity of Apple's Core Bluetooth API. One of these shows services AND
     * advetisement RSSIs, whilst the other only shows services and only passes RSSIs when they
     * are from the initial single discovery call or from connection events.
     * <br><br>
     * Although this mechanism is primarily aimed at very old and very badly behaved handsets,
     * it will allow us to still be efficient on iOS devices now too.
     *
     * @since v2.1.0-beta4
     */
    public static boolean pseudoDeviceAddressEnabled = false;

    /**
     *  Interrogate standard Bluetooth services to obtain device make/model data
     */
    public static boolean deviceIntrospectionEnabled = false;

    /**
     *  Enable device filter training
     *  <br>- Use this to gather device make/model and advert messages
     *  <br>- Generates "filter.csv" log file for analysis
     *  <br>- Enable device introspection to obtain device make/model data
     *  <br>- Performs device introspection even if the device does not advertise sensor services
     *  <br>- Triggers update every minute for each device to gather sample advert data
     *  <br>- Disables device filter feature patterns
     */
    public static boolean deviceFilterTrainingEnabled = false;

    /**
     *  Define device filtering rules based on message patterns
     *  <br>- Avoids connections to devices that cannot host sensor services
     *  <br>- Matches against every manufacturer specific data message (Hex format) in advert
     *  <br>- Java regular expression patterns, case insensitive, find pattern anywhere in message
     *  <br>- Remember to include ^ to match from start of message
     *  <br>- Use deviceFilterTrainingEnabled in development environment to identify patterns
     */
    @NonNull
    public static String[] deviceFilterFeaturePatterns = new String[]{
            "^10....04",
            "^10....14",
            "^0100000000000000000000000000000000",
            "^05","^07","^09",
            "^00","^1002","^06","^08","^03","^0C","^0D","^0F","^0E","^0B"
    };

    /**
     *  Enable inertia sensor
     *  <br>- Inertia sensor (accelerometer) measures acceleration in meters per second (m/s) along device X, Y and Z axis
     *  <br>- Generates SensorDelegate:didVisit callbacks with InertiaLocationReference data
     *  <br>- Set to false to disable sensor, and true value to enable sensor
     *  <br>- This is used for automated capture of RSSI at different distances, where the didVisit data is used as markers
     */
    public static boolean inertiaSensorEnabled = false;

    /**
     * Whether the General Purpose Distribution Messaging Protocol (GPDMP) is enabled
     */
    public static boolean gpdmpEnabled = false;

    /**
     * To be used if the Operating System cannot be trusted to allow scanning whilst also advertising
     */
    public static boolean manuallyEnforceAdvertGaps = false;
}


