//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanRecord;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.Device;
import io.heraldprox.herald.sensor.datatype.Calibration;
import io.heraldprox.herald.sensor.datatype.CalibrationMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PseudoDeviceAddress;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

public class BLEDevice extends Device {
    // Pseudo device address for tracking Android devices that change address constantly.
    @Nullable
    private PseudoDeviceAddress pseudoDeviceAddress = null;
    // Delegate for listening to attribute updates events.
    @NonNull
    private final BLEDeviceDelegate delegate;
    // Android Bluetooth device object for interacting with this device.
    @Nullable
    private BluetoothDevice peripheral = null;
    // Bluetooth device connection state.
    @NonNull
    private BLEDeviceState state = BLEDeviceState.disconnected;
    // Device operating system, this is necessary for selecting different interaction procedures for each platform.
    @NonNull
    private BLEDeviceOperatingSystem operatingSystem = BLEDeviceOperatingSystem.unknown;
    // Payload data acquired from the device via payloadCharacteristic read, e.g. C19X beacon code or Sonar encrypted identifier
    @Nullable
    private PayloadData payloadData = null;
    @Nullable
    private Date lastPayloadDataUpdate = null;
    // Immediate Send data to send next
    @Nullable
    private Data immediateSendData = null;
    // Most recent RSSI measurement taken by readRSSI or didDiscover.
    @Nullable
    private RSSI rssi = null;
    // Transmit power data where available (only provided by Android devices)
    @Nullable
    private BLE_TxPower txPower = null;
    // Is device receive only?
    private boolean receiveOnly = false;
    // Ignore logic
    @Nullable
    private TimeInterval ignoreForDuration = null;
    @Nullable
    private Date ignoreUntil = null;
    @Nullable
    private ScanRecord scanRecord = null;

    // BLE characteristics
    @Nullable
    private BluetoothGattCharacteristic signalCharacteristic = null;
    @Nullable
    private BluetoothGattCharacteristic payloadCharacteristic = null;
    @Nullable
    private BluetoothGattCharacteristic legacyPayloadCharacteristic = null;
    @Nullable
    protected byte[] signalCharacteristicWriteValue = null;
    @Nullable
    protected Queue<byte[]> signalCharacteristicWriteQueue = null;

    @Nullable
    private BluetoothGattCharacteristic modelCharacteristic = null;
    @Nullable
    private String model = null;
    @Nullable
    private BluetoothGattCharacteristic deviceNameCharacteristic = null;
    @Nullable
    private String deviceName = null;

    // Track connection timestamps
    @SuppressWarnings("FieldCanBeLocal")
    @Nullable
    private Date lastDiscoveredAt = null;
    @Nullable
    private Date lastConnectedAt = null;

    // Payload data already shared with this peer
    protected final List<PayloadData> payloadSharingData = new ArrayList<>();

    // Track write timestamps
    @Nullable
    private Date lastWritePayloadAt = null;
    @Nullable
    private Date lastWriteRssiAt = null;
    @Nullable
    private Date lastWritePayloadSharingAt = null;

    @NonNull
    public TimeInterval timeIntervalSinceConnected() {
        if (state() != BLEDeviceState.connected) {
            return TimeInterval.zero;
        }
        if (null == lastConnectedAt) {
            return TimeInterval.zero;
        }
        return new TimeInterval((new Date().getTime() - lastConnectedAt.getTime()) / 1000);
    }

    /**
     * Time interval since last attribute value update, this is used to identify devices
     * that may have expired and should be removed from the database. This is also used by
     * immediateSendAll to choose targets
     * @return Time interval since last attribute value update
     */
    @NonNull
    public TimeInterval timeIntervalSinceLastUpdate() {
        //noinspection ConstantConditions
        if (null == lastUpdatedAt) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastUpdatedAt.getTime()) / 1000);
    }

    @NonNull
    public String description() {
        return "BLEDevice[" +
                "id=" + identifier +
                ",os=" + operatingSystem +
                ",payload=" + payloadData() +
                (pseudoDeviceAddress() != null ? ",address=" + pseudoDeviceAddress() : "") +
                (deviceName() != null ? ",name=" + deviceName() : "") +
                (model() != null ? ",model=" + model() : "") +
                "]";
    }

    public BLEDevice(@NonNull final TargetIdentifier identifier, @NonNull final BLEDeviceDelegate delegate) {
        super(identifier);
        this.delegate = delegate;
    }

    @Nullable
    public PseudoDeviceAddress pseudoDeviceAddress() {
        return pseudoDeviceAddress;
    }

    public void pseudoDeviceAddress(@Nullable final PseudoDeviceAddress pseudoDeviceAddress) {
        if (null == this.pseudoDeviceAddress || !this.pseudoDeviceAddress.equals(pseudoDeviceAddress)) {
            this.pseudoDeviceAddress = pseudoDeviceAddress;
            lastUpdatedAt = new Date();
        }
    }

    @Nullable
    public BluetoothDevice peripheral() {
        return peripheral;
    }

    public void peripheral(@Nullable final BluetoothDevice peripheral) {
        if (this.peripheral != peripheral) {
            this.peripheral = peripheral;
            lastUpdatedAt = new Date();
        }
    }

    @NonNull
    public BLEDeviceState state() {
        return state;
    }

    public void state(@NonNull final BLEDeviceState state) {
        this.state = state;
        lastUpdatedAt = new Date();
        if (state == BLEDeviceState.connected) {
            lastConnectedAt = lastUpdatedAt;
        }
        delegate.device(this, BLEDeviceAttribute.state);
    }

    @NonNull
    public BLEDeviceOperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public void operatingSystem(@NonNull final BLEDeviceOperatingSystem operatingSystem) {
        lastUpdatedAt = new Date();
        // Set ignore timer
        if (operatingSystem == BLEDeviceOperatingSystem.ignore) {
            if (null == ignoreForDuration) {
                ignoreForDuration = TimeInterval.minute;
            } else if (ignoreForDuration.value < TimeInterval.minutes(3).value) {
                ignoreForDuration = new TimeInterval(Math.round(ignoreForDuration.value * 1.2));
            }
            ignoreUntil = new Date(lastUpdatedAt.getTime() + ignoreForDuration.millis());
        } else {
            ignoreUntil = null;
        }
        // Reset ignore for duration and request count if operating system has been confirmed
        if (operatingSystem == BLEDeviceOperatingSystem.ios || operatingSystem == BLEDeviceOperatingSystem.android) {
            ignoreForDuration = null;
        }
        // Set operating system
        if (this.operatingSystem != operatingSystem) {
            this.operatingSystem = operatingSystem;
            delegate.device(this, BLEDeviceAttribute.operatingSystem);
        }
    }

    /**
     * Should this device be ignored for now.
     * @return True if device should be ignored for now, false otherwise
     */
    public boolean ignore() {
        if (null == ignoreUntil) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (new Date().getTime() < ignoreUntil.getTime()) {
            return true;
        }
        return false;
    }

    @Nullable
    public PayloadData payloadData() {
        return payloadData;
    }

    public void payloadData(@Nullable final PayloadData payloadData) {
        this.payloadData = payloadData;
        lastPayloadDataUpdate = new Date();
        lastUpdatedAt = lastPayloadDataUpdate;
        delegate.device(this, BLEDeviceAttribute.payloadData);
    }

    @NonNull
    public TimeInterval timeIntervalSinceLastPayloadDataUpdate() {
        if (null == lastPayloadDataUpdate) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastPayloadDataUpdate.getTime()) / 1000);
    }

    public void immediateSendData(@Nullable final Data immediateSendData) {
        this.immediateSendData = immediateSendData;
    }

    @Nullable
    public Data immediateSendData() {
        return immediateSendData;
    }

    @Nullable
    public RSSI rssi() {
        return rssi;
    }

    public void rssi(@Nullable final RSSI rssi) {
        this.rssi = rssi;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.rssi);
    }

    public void legacyPayloadCharacteristic(@Nullable final BluetoothGattCharacteristic characteristic) {
        this.legacyPayloadCharacteristic = characteristic;
    }

    @Nullable
    public BluetoothGattCharacteristic legacyPayloadCharacteristic() {
        return legacyPayloadCharacteristic;
    }

    @Nullable
    public BLE_TxPower txPower() {
        return txPower;
    }

    public void txPower(@Nullable final BLE_TxPower txPower) {
        lastUpdatedAt = new Date();
        // Only update if TxPower has changed
        if (this.txPower == txPower) {
            return;
        }
        if (null != this.txPower && null != txPower && this.txPower.value == txPower.value) {
            return;
        }
        // Update only when TxPower has changed
        this.txPower = txPower;
        delegate.device(this, BLEDeviceAttribute.txPower);
    }

    @Nullable
    public Calibration calibration() {
        if (null == txPower) {
            return null;
        }
        return new Calibration(CalibrationMeasurementUnit.BLETransmitPower, (double) txPower.value);
    }

    public boolean receiveOnly() {
        return receiveOnly;
    }

    public void receiveOnly(boolean receiveOnly) {
        this.receiveOnly = receiveOnly;
        lastUpdatedAt = new Date();
    }

    public void invalidateCharacteristics() {
        signalCharacteristic = null;
        payloadCharacteristic = null;
        modelCharacteristic = null;
        deviceNameCharacteristic = null;
        legacyPayloadCharacteristic = null;
    }

    @Nullable
    public BluetoothGattCharacteristic signalCharacteristic() {
        return signalCharacteristic;
    }

    public void signalCharacteristic(@Nullable final BluetoothGattCharacteristic characteristic) {
        this.signalCharacteristic = characteristic;
        lastUpdatedAt = new Date();
    }

    @Nullable
    public BluetoothGattCharacteristic payloadCharacteristic() {
        return payloadCharacteristic;
    }

    public void payloadCharacteristic(@Nullable final BluetoothGattCharacteristic characteristic) {
        this.payloadCharacteristic = characteristic;
        lastUpdatedAt = new Date();
    }

    public boolean supportsModelCharacteristic() { return null != modelCharacteristic; }

    @Nullable
    public BluetoothGattCharacteristic modelCharacteristic() { return modelCharacteristic; }

    public void modelCharacteristic(@Nullable final BluetoothGattCharacteristic modelCharacteristic) {
        this.modelCharacteristic = modelCharacteristic;
        lastUpdatedAt = new Date();
    }

    public boolean supportsDeviceNameCharacteristic() { return null != deviceNameCharacteristic; }

    @Nullable
    public BluetoothGattCharacteristic deviceNameCharacteristic() { return deviceNameCharacteristic; }

    public void deviceNameCharacteristic(@Nullable final BluetoothGattCharacteristic deviceNameCharacteristic) {
        this.deviceNameCharacteristic = deviceNameCharacteristic;
        lastUpdatedAt = new Date();
    }

    @Nullable
    public String deviceName() { return deviceName; }

    public void deviceName(@Nullable final String deviceName) {
        this.deviceName = deviceName;
        lastUpdatedAt = new Date();
    }

    @Nullable
    public String model() { return model; }

    public void model(@Nullable final String model) {
        this.model = model;
        lastUpdatedAt = new Date();
    }

    public void registerDiscovery() {
        lastDiscoveredAt = new Date();
        lastUpdatedAt = lastDiscoveredAt;
    }

    public void registerWritePayload() {
        lastUpdatedAt = new Date();
        lastWritePayloadAt = lastUpdatedAt;
    }

    @NonNull
    public TimeInterval timeIntervalSinceLastWritePayload() {
        if (null == lastWritePayloadAt) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadAt.getTime()) / 1000);
    }

    public void registerWriteRssi() {
        lastUpdatedAt = new Date();
        lastWriteRssiAt = lastUpdatedAt;
    }

    @NonNull
    public TimeInterval timeIntervalSinceLastWriteRssi() {
        if (null == lastWriteRssiAt) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWriteRssiAt.getTime()) / 1000);
    }

    public void registerWritePayloadSharing() {
        lastUpdatedAt = new Date();
        lastWritePayloadSharingAt = lastUpdatedAt;
    }

    @NonNull
    public TimeInterval timeIntervalSinceLastWritePayloadSharing() {
        if (null == lastWritePayloadSharingAt) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadSharingAt.getTime()) / 1000);
    }

    @NonNull
    public TimeInterval timeIntervalUntilIgnoreExpires() {
        if (null == ignoreUntil) {
            return TimeInterval.zero;
        }
        if (Long.MAX_VALUE == ignoreUntil.getTime()) {
            return TimeInterval.never;
        }
        return new TimeInterval((ignoreUntil.getTime() - new Date().getTime()) / 1000);
    }

    public boolean protocolIsOpenTrace() {
        return null != legacyPayloadCharacteristic && null == signalCharacteristic;
    }

    public boolean protocolIsHerald() {
        return null != signalCharacteristic && null != payloadCharacteristic;
    }

    public void scanRecord(@Nullable final ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }

    @Nullable
    public ScanRecord scanRecord() {
        return scanRecord;
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
