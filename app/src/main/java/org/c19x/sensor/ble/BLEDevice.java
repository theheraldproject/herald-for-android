package org.c19x.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.TargetIdentifier;
import org.c19x.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BLEDevice {
    /// Device registratiion timestamp
    public final Date createdAt;
    /// Last time anything changed, e.g. attribute update
    public Date lastUpdatedAt;
    /// Ephemeral device identifier, e.g. peripheral identifier UUID
    public final TargetIdentifier identifier;
    /// Delegate for listening to attribute updates events.
    private final BLEDeviceDelegate delegate;
    /// CoreBluetooth peripheral object for interacting with this device.
    private BluetoothDevice peripheral;
    /// Service characteristic for signalling between BLE devices, e.g. to keep awake
    private BluetoothGattCharacteristic signalCharacteristic;
    /// Service characteristic for reading payload data, e.g. C19X beacon code or Sonar encrypted identifier
    private BluetoothGattCharacteristic payloadCharacteristic;
    /// Service characteristic for reading payload sharing data, e.g. C19X beacon code or Sonar encrypted identifier recently acquired by this device
    private BluetoothGattCharacteristic payloadSharingCharacteristic;
    /// Device operating system, this is necessary for selecting different interaction procedures for each platform.
    private BLEDeviceOperatingSystem operatingSystem = BLEDeviceOperatingSystem.unknown;
    /// Payload data acquired from the device via payloadCharacteristic read, e.g. C19X beacon code or Sonar encrypted identifier
    private PayloadData payloadData;
    /// Payload data last update timestamp, this is used to determine what needs to be shared with peers.
    private Date payloadDataLastUpdatedAt = new Date(0);
    /// Payload data already shared with this peer
    public List<PayloadData> payloadSharingData = new ArrayList<>();
    /// Payload sharing last update timestamp, , this is used to throttle read payload sharing calls
    private Date payloadSharingDataLastUpdatedAt = new Date(0);
    /// Most recent RSSI measurement taken by readRSSI or didDiscover.
    private RSSI rssi;
    /// Transmit power data where available (only provided by Android devices)
    private BLE_TxPower txPower;

    /// Time interval since last attribute value update, this is used to identify devices that may have expired and should be removed from the database.
    public TimeInterval timeIntervalSinceLastUpdate() {
        return new TimeInterval((new Date().getTime() - lastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval since last payload sharing value update, this is used to throttle read payload sharing calls
    public TimeInterval timeIntervalSinceLastPayloadShared() {
        return new TimeInterval((new Date().getTime() - payloadSharingDataLastUpdatedAt.getTime()) / 1000);
    }

    public String description() {
        return "BLEDevice[id=" + identifier + ",lastUpdatedAt=" + lastUpdatedAt + ",peripheral=" + (peripheral == null ? "-" : "T") + ",os=" + operatingSystem + "]";
    }

    public BLEDevice(TargetIdentifier identifier, BLEDeviceDelegate delegate) {
        this.createdAt = new Date();
        this.identifier = identifier;
        this.delegate = delegate;
        this.lastUpdatedAt = createdAt;
    }

    public BluetoothDevice peripheral() {
        return peripheral;
    }

    public void peripheral(BluetoothDevice peripheral) {
        this.peripheral = peripheral;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.peripheral);
    }

    public BluetoothGattCharacteristic signalCharacteristic() {
        return signalCharacteristic;
    }

    public void signalCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.signalCharacteristic = characteristic;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.signalCharacteristic);
    }

    public BluetoothGattCharacteristic payloadCharacteristic() {
        return payloadCharacteristic;
    }

    public void payloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.payloadCharacteristic = characteristic;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.payloadCharacteristic);
    }

    public BluetoothGattCharacteristic payloadSharingCharacteristic() {
        return payloadSharingCharacteristic;
    }

    public void payloadSharingCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.payloadSharingCharacteristic = characteristic;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.payloadSharingCharacteristic);
    }

    public BLEDeviceOperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public void operatingSystem(BLEDeviceOperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.operatingSystem);
    }

    public PayloadData payloadData() {
        return payloadData;
    }

    public void payloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.payloadData);
    }

    public RSSI rssi() {
        return rssi;
    }

    public void payloadData(RSSI payloadData) {
        this.rssi = rssi;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.rssi);
    }

    public BLE_TxPower txPower() {
        return txPower;
    }

    public void txPower(BLE_TxPower txPower) {
        this.txPower = txPower;
        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.txPower);
    }

    public String toString() {
        return description();
    }
}
