package org.c19x.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

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
    /// Android Bluetooth device object for interacting with this device.
    private BluetoothDevice peripheral;
    /// Bluetooth device connection state.
    private BLEDeviceState state = BLEDeviceState.disconnected;
    /// Bluetooth GATT handle.
    public BluetoothGatt gatt = null;
    /// Bluetooth device connection state last update timestamp, this is used to disconnect long connections.
    private Date stateLastUpdatedAt = new Date(0);
    /// Device operating system, this is necessary for selecting different interaction procedures for each platform.
    private BLEDeviceOperatingSystem operatingSystem = BLEDeviceOperatingSystem.unknown;
    /// Payload data acquired from the device via payloadCharacteristic read, e.g. C19X beacon code or Sonar encrypted identifier
    private PayloadData payloadData;
    /// Payload data last update timestamp, this is used to determine what needs to be shared with peers.
    private Date payloadDataLastUpdatedAt = new Date(0);
    /// Payload data already shared with this peer
    protected List<PayloadData> payloadSharingData = new ArrayList<>();
    /// Payload data shared by this peer
    private List<PayloadData> payloadSharingDataReceived;
    /// Payload sharing last update timestamp, this is used to throttle read payload sharing calls
    private Date payloadSharingDataLastUpdatedAt = new Date(0);
    /// Most recent RSSI measurement taken by readRSSI or didDiscover.
    private RSSI rssi;
    /// Transmit power data where available (only provided by Android devices)
    private BLE_TxPower txPower;
    /// Bluetooth GATT connection
    protected BluetoothGatt bluetoothGatt;
    /// Write back timestamp, this is used to prioritise peers for write back
    private Date lastWriteBackAt = new Date(0);
    /// Write payload timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWritePayloadAt = null;
    /// Write RSSI timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWriteRssiAt = null;
    /// Write payload sharing timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWritePayloadSharingAt = null;


    /// Time interval since last attribute value update, this is used to identify devices that may have expired and should be removed from the database.
    public TimeInterval timeIntervalSinceLastUpdate() {
        return new TimeInterval((new Date().getTime() - lastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval since last payload sharing value update, this is used to throttle read payload sharing calls
    public TimeInterval timeIntervalSinceLastPayloadShared() {
        return new TimeInterval((new Date().getTime() - payloadSharingDataLastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval since last state update, this is used to terminate long connections
    public TimeInterval timeIntervalSinceLastStateUpdate() {
        return new TimeInterval((new Date().getTime() - stateLastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval since last write back, this is used to prioritise peers for write back
    public TimeInterval timeIntervalSinceLastWriteBack() {
        return new TimeInterval((new Date().getTime() - lastWriteBackAt.getTime()) / 1000);
    }

    /// Time interval since last write payload, this is used to prioritise peers for write back
    public TimeInterval timeIntervalSinceLastWritePayload() {
        return new TimeInterval((new Date().getTime() - (lastWritePayloadAt == null ? 0 : lastWritePayloadAt.getTime())) / 1000);
    }

    /// Time interval since last write rssi, this is used to prioritise peers for write back
    public TimeInterval timeIntervalSinceLastWriteRssi() {
        return new TimeInterval((new Date().getTime() - (lastWriteRssiAt == null ? 0 : lastWriteRssiAt.getTime())) / 1000);
    }

    /// Time interval since last write payload sharing, this is used to prioritise peers for write back
    public TimeInterval timeIntervalSinceLastWritePayloadSharing() {
        return new TimeInterval((new Date().getTime() - (lastWritePayloadSharingAt == null ? 0 : lastWritePayloadSharingAt.getTime())) / 1000);
    }

    public String description() {
        return "BLEDevice[id=" + identifier + ",os=" + operatingSystem + "]";
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

    public BLEDeviceState state() {
        return state;
    }

    public void state(BLEDeviceState state) {
        this.state = state;
//        lastUpdatedAt = new Date();
        delegate.device(this, BLEDeviceAttribute.state);
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

    public List<PayloadData> payloadSharingData() {
        return payloadSharingDataReceived;
    }

    public void payloadSharingData(List<PayloadData> payloadSharingData) {
        this.payloadSharingDataReceived = payloadSharingData;
        lastUpdatedAt = new Date();
        payloadSharingDataLastUpdatedAt = lastUpdatedAt;
        delegate.device(this, BLEDeviceAttribute.payloadSharingData);
    }

    public RSSI rssi() {
        return rssi;
    }

    public void rssi(RSSI rssi) {
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

    public void writePayload(boolean success) {
        lastUpdatedAt = new Date();
        if (success) {
            lastWritePayloadAt = lastUpdatedAt;
            lastWriteBackAt = lastUpdatedAt;
        }
    }

    public void writeRssi(boolean success) {
        lastUpdatedAt = new Date();
        if (success) {
            lastWriteRssiAt = lastUpdatedAt;
            lastWriteBackAt = lastUpdatedAt;
        }
    }

    public void writePayloadSharing(boolean success) {
        lastUpdatedAt = new Date();
        if (success) {
            lastWritePayloadSharingAt = lastUpdatedAt;
            lastWriteBackAt = lastUpdatedAt;
        }
    }

    public String toString() {
        return description();
    }
}
