//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadSharingData;
import io.heraldprox.herald.sensor.datatype.PseudoDeviceAddress;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLEDatabase implements BLEDatabase, BLEDeviceDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEDatabase");
    private final Queue<BLEDatabaseDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Map<TargetIdentifier, BLEDevice> database = new ConcurrentHashMap<>();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();

    @Override
    public void add(@NonNull final BLEDatabaseDelegate delegate) {
        delegates.add(delegate);
    }

    @Nullable
    @Override
    public BLEDevice device(@NonNull final TargetIdentifier targetIdentifier) {
        return database.get(targetIdentifier);
    }

    @NonNull
    @Override
    public BLEDevice device(@NonNull final BluetoothDevice bluetoothDevice) {
        final TargetIdentifier identifier = new TargetIdentifier(bluetoothDevice);
        BLEDevice device = database.get(identifier);
        if (null == device) {
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("create (device={})", identifier);
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.peripheral(bluetoothDevice);
        return device;
    }

    @NonNull
    @Override
    public BLEDevice device(@NonNull final ScanResult scanResult) {
        // Get device by target identifier
        final BluetoothDevice bluetoothDevice = scanResult.getDevice();
        final TargetIdentifier targetIdentifier = new TargetIdentifier(bluetoothDevice);
        final BLEDevice existingDevice = database.get(targetIdentifier);
        if (null != existingDevice) {
            return existingDevice;
        }
        // Get device by pseudo device address
        final PseudoDeviceAddress pseudoDeviceAddress = pseudoDeviceAddress(scanResult);
        if (null != pseudoDeviceAddress) {
            // Reuse existing Android device
            BLEDevice deviceWithSamePseudoDeviceAddress = null;
            for (final BLEDevice device : database.values()) {
                //noinspection ConstantConditions
                if (null != device.pseudoDeviceAddress() && device.pseudoDeviceAddress().equals(pseudoDeviceAddress)) {
                    deviceWithSamePseudoDeviceAddress = device;
                    break;
                }
            }
            if (null != deviceWithSamePseudoDeviceAddress) {
                database.put(targetIdentifier, deviceWithSamePseudoDeviceAddress);
                if (deviceWithSamePseudoDeviceAddress.peripheral() != bluetoothDevice) {
                    deviceWithSamePseudoDeviceAddress.peripheral(bluetoothDevice);
                }
                if (deviceWithSamePseudoDeviceAddress.operatingSystem() != BLEDeviceOperatingSystem.android) {
                    deviceWithSamePseudoDeviceAddress.operatingSystem(BLEDeviceOperatingSystem.android);
                }
                logger.debug("updateAddress (device={})", deviceWithSamePseudoDeviceAddress);
                return deviceWithSamePseudoDeviceAddress;
            }
            // Create new Android device
            else {
                final BLEDevice newDevice = device(bluetoothDevice);
                newDevice.pseudoDeviceAddress(pseudoDeviceAddress);
                newDevice.operatingSystem(BLEDeviceOperatingSystem.android);
                return newDevice;
            }
        }
        // Create new device
        return device(bluetoothDevice);
    }

    /**
     * Get pseudo device address for Android devices.
     * @param scanResult Scan result may contain pseudo device address
     * @return Pseudo device address extracted from scan result, or null if not found (e.g. iOS devices)
     */
    @Nullable
    private PseudoDeviceAddress pseudoDeviceAddress(@NonNull final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (null == scanRecord || null == scanRecord.getManufacturerSpecificData()) {
            return null;
        }
        // Add external entropy to RandomSource
        BLESensorConfiguration.pseudoDeviceAddressRandomisation.addEntropy(scanResult.getDevice().getAddress());
        // HERALD pseudo device address (Registered with the Bluetooth SIG)
        if (null != scanRecord.getManufacturerSpecificData(BLESensorConfiguration.linuxFoundationManufacturerIdForSensor)) {
            final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.linuxFoundationManufacturerIdForSensor);
            if (data != null && data.length == 6) {
                return new PseudoDeviceAddress(data);
            }
            // LEGACY Herald manufacturer ID (Was unregistered with the Bluetooth SIG)
        } else if (BLESensorConfiguration.legacyHeraldServiceDetectionEnabled &&
                null != scanRecord.getManufacturerSpecificData(BLESensorConfiguration.legacyHeraldManufacturerIdForSensor)) {
            final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.legacyHeraldManufacturerIdForSensor);
            if (data != null && data.length == 6) {
                return new PseudoDeviceAddress(data);
            }
            // OpenTrace device id
        } else if (BLESensorConfiguration.interopOpenTraceEnabled &&
                null != scanRecord.getManufacturerSpecificData(BLESensorConfiguration.interopOpenTraceManufacturerId)) {
            final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.interopOpenTraceManufacturerId);
            if (null != data && data.length > 0) {
                return new PseudoDeviceAddress(data);
            }
        } else if (BLESensorConfiguration.customServiceDetectionEnabled &&
                   0 != BLESensorConfiguration.customManufacturerIdForSensor) {
            final byte[] data = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.customManufacturerIdForSensor);
            if (null != data && data.length > 0) {
                return new PseudoDeviceAddress(data);
            }
        }
        // Not found
        return null;
    }

    @NonNull
    @Override
    public BLEDevice device(@NonNull PayloadData payloadData) {
        BLEDevice device = null;
        for (BLEDevice candidate : database.values()) {
            if (payloadData.equals(candidate.payloadData())) {
                device = candidate;
                break;
            }
        }
        if (null == device) {
            final TargetIdentifier identifier = new TargetIdentifier();
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("create (device={})", identifier);
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.payloadData(payloadData);
        return device;
    }

    @NonNull
    @Override
    public List<BLEDevice> devices() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void delete(@Nullable final BLEDevice device) {
        if (null == device) {
            return;
        }
        final List<TargetIdentifier> identifiers = new ArrayList<>();
        for (final Map.Entry<TargetIdentifier,BLEDevice> entry : database.entrySet()) {
            if (entry.getValue() == device) {
                identifiers.add(entry.getKey());
            }
        }
        if (identifiers.isEmpty()) {
            return;
        }
        for (final TargetIdentifier identifier : identifiers) {
            database.remove(identifier);
        }
        queue.execute(new Runnable() {
            @Override
            public void run() {
            logger.debug("delete (device={},identifiers={})", device, identifiers);
            for (final BLEDatabaseDelegate delegate : delegates) {
                delegate.bleDatabaseDidDelete(device);
            }
            }
        });
    }

    @NonNull
    @Override
    public PayloadSharingData payloadSharingData(@NonNull final BLEDevice peer) {
        final RSSI rssi = peer.rssi();
        if (null == rssi) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // Get other devices that were seen recently by this device
        final List<BLEDevice> unknownDevices = new ArrayList<>();
        final List<BLEDevice> knownDevices = new ArrayList<>();
        for (BLEDevice device : database.values()) {
            // Device was seen recently
            if (device.timeIntervalSinceLastUpdate().value >= BLESensorConfiguration.payloadSharingExpiryTimeInterval.value) {
                continue;
            }
            // Device has payload
            if (null == device.payloadData()) {
                continue;
            }
            // Device is iOS or receive only (Samsung J6)
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.receiveOnly())) {
                continue;
            }
            // Device is HERALD
            if (null == device.signalCharacteristic()) {
                continue;
            }
            // Payload is not the peer itself
            //noinspection ConstantConditions
            if (null != peer.payloadData() && (Arrays.equals(device.payloadData().value, peer.payloadData().value))) {
                continue;
            }
            // Payload is new to peer
            if (peer.payloadSharingData.contains(device.payloadData())) {
                knownDevices.add(device);
            } else {
                unknownDevices.add(device);
            }
        }
        // Most recently seen unknown devices first
        final List<BLEDevice> devices = new ArrayList<>();
        Collections.sort(unknownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(@NonNull BLEDevice d0, @NonNull BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        Collections.sort(knownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(@NonNull BLEDevice d0, @NonNull BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        devices.addAll(unknownDevices);
        devices.addAll(knownDevices);
        if (0 == devices.size()) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // Limit how much to share to avoid oversized data transfers over BLE
        // (512 bytes limit according to spec, 510 with response, iOS requires response)
        final Set<PayloadData> sharedPayloads = new HashSet<>(devices.size());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (BLEDevice device : devices) {
            final PayloadData payloadData = device.payloadData();
            if (null == payloadData) {
                continue;
            }
            // Eliminate duplicates (this happens when the same device has changed address but the old version has not expired yet)
            if (sharedPayloads.contains(payloadData)) {
                continue;
            }
            // Limit payload sharing by BLE transfer limit
            if (payloadData.value.length + byteArrayOutputStream.toByteArray().length > 510) {
                break;
            }
            try {
                byteArrayOutputStream.write(payloadData.value);
                peer.payloadSharingData.add(payloadData);
                sharedPayloads.add(payloadData);
            } catch (Throwable e) {
                logger.fault("Failed to append payload sharing data", e);
            }
        }
        final Data data = new Data(byteArrayOutputStream.toByteArray());
        return new PayloadSharingData(rssi, data);
    }

    // MARK:- BLEDeviceDelegate

    @Override
    public void device(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute didUpdate) {
        queue.execute(new Runnable() {
            @Override
            public void run() {
                logger.debug("update (device={},attribute={})", device.identifier, didUpdate.name());
                for (BLEDatabaseDelegate delegate : delegates) {
                    delegate.bleDatabaseDidUpdate(device, didUpdate);
                }
            }
        });
    }
}
