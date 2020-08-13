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
    /// Device operating system last update timestamp, this is used to switch .ignore to .unknown at regular intervals
    // such that Apple devices not advertising the sensor service don't waste too much processing time, yet not completely
    // ignored forever, as the sensor service may become available later.
    private Date operatingSystemLastUpdatedAt = new Date(0);
    /// Device is receive only, this is necessary for filtering payload sharing data
    private boolean receiveOnly = false;
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
    /// RSSI last update timestamp, this is used to track last advertised at without relying on didDiscover
    private Date rssiLastUpdatedAt = new Date(0);
    /// Transmit power data where available (only provided by Android devices)
    private BLE_TxPower txPower;
    /// Write back timestamp, this is used to prioritise peers for write back
    private Date lastWriteBackAt = new Date(0);
    /// Write payload timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWritePayloadAt = null;
    /// Write RSSI timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWriteRssiAt = null;
    /// Write payload sharing timestamp, this is used to prioritise next write action for transmit only devices
    protected Date lastWritePayloadSharingAt = null;
    /// Track discovered at timestamp, used by taskConnect to prioritise connection when device runs out of concurrent connection capacity
    protected Date lastDiscoveredAt = new Date(0);
    /// Track connect request at timestamp, used by taskConnect to prioritise connection when device runs out of concurrent connection capacity
    protected Date lastConnectRequestedAt = new Date(0);
    /// Track connected at timestamp, used by taskConnect to prioritise connection when device runs out of concurrent connection capacity
    private Date lastConnectedAt = null;
    /// Track disconnected at timestamp, used by taskConnect to prioritise connection when device runs out of concurrent connection capacity
    private Date lastDisconnectedAt = null;

    /// Last advert timestamp, inferred from payloadDataLastUpdatedAt, payloadSharingDataLastUpdatedAt, rssiLastUpdatedAt
    public Date lastAdvertAt() {
        long max = createdAt.getTime();
        max = Math.max(max, lastDiscoveredAt.getTime());
        max = Math.max(max, payloadDataLastUpdatedAt.getTime());
        max = Math.max(max, payloadSharingDataLastUpdatedAt.getTime());
        max = Math.max(max, rssiLastUpdatedAt.getTime());
        return new Date(max);
    }

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

    /// Time interval since last operating system update, this is used to invalidate ignored Apple devices
    public TimeInterval timeIntervalSinceLastOperatingSystemUpdate() {
        return new TimeInterval((new Date().getTime() - operatingSystemLastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval since last advert detected, this is used to detect concurrent connection quota and prioritise disconnections
    public TimeInterval timeIntervalSinceLastAdvert() {
        return new TimeInterval((new Date().getTime() - lastAdvertAt().getTime()) / 1000);
    }

    /// Time interval between last connection request, this is used to priortise disconnections
    public TimeInterval timeIntervalSinceLastConnectRequestedAt() {
        return new TimeInterval((new Date().getTime() - lastConnectRequestedAt.getTime()) / 1000);
    }

    /// Time interval between last connected at, this is used to estimate last period of continuous tracking, to priortise disconnections
    public TimeInterval timeIntervalSinceLastConnectedAt() {
        if (lastConnectedAt == null) {
            return new TimeInterval(0);
        }
        return new TimeInterval((new Date().getTime() - lastConnectedAt.getTime()) / 1000);
    }

    /// Time interval between last disconnected at, this is used to estimate last period of continuous tracking, to priortise disconnections
    public TimeInterval timeIntervalSinceLastDisconnectedAt() {
        if (lastDisconnectedAt == null) {
            return new TimeInterval((new Date().getTime() - createdAt.getTime()) / 1000);
        }
        return new TimeInterval((new Date().getTime() - lastDisconnectedAt.getTime()) / 1000);
    }

    /// Time interval between last connected at and last advert, this is used to estimate last period of continuous tracking (iOS only), to priortise disconnections
    public TimeInterval timeIntervalBetweenLastConnectedAndLastAdvert() {
        if (lastConnectedAt == null || lastAdvertAt().getTime() <= lastConnectedAt.getTime()) {
            return new TimeInterval(0);
        }
        return new TimeInterval((lastAdvertAt().getTime() - lastConnectedAt.getTime()) / 1000);
    }

    /// Time interval between last payload data updated at and last advert, this is used to estimate last period of continuous tracking (Android only), to priortise disconnections
    public TimeInterval timeIntervalBetweenLastPayloadDataUpdateAndLastAdvert() {
        if (payloadDataLastUpdatedAt().getTime() == 0 || lastAdvertAt().getTime() <= payloadDataLastUpdatedAt.getTime()) {
            return new TimeInterval(0);
        }
        return new TimeInterval((lastAdvertAt().getTime() - payloadDataLastUpdatedAt.getTime()) / 1000);
    }


    public String description() {
        return "BLEDevice[id=" + identifier + ",os=" + operatingSystem + ",goal=" + goal().name() + "]";
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
        delegate.device(this, BLEDeviceAttribute.state);
    }

    public BLEDeviceOperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public void operatingSystem(BLEDeviceOperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        lastUpdatedAt = new Date();
        operatingSystemLastUpdatedAt = lastUpdatedAt;
        delegate.device(this, BLEDeviceAttribute.operatingSystem);
    }

    public boolean receiveOnly() {
        return receiveOnly;
    }

    public void receiveOnly(boolean receiveOnly) {
        this.receiveOnly = receiveOnly;
        lastUpdatedAt = new Date();
    }

    public PayloadData payloadData() {
        return payloadData;
    }

    public void payloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
        lastUpdatedAt = new Date();
        payloadDataLastUpdatedAt = lastUpdatedAt;
        delegate.device(this, BLEDeviceAttribute.payloadData);
    }

    public Date payloadDataLastUpdatedAt() {
        return payloadDataLastUpdatedAt;
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
        rssiLastUpdatedAt = lastUpdatedAt;
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

    public Date lastConnectedAt() {
        return lastConnectedAt;
    }

    public void lastConnectedAt(Date time) {
        lastConnectedAt = time;
        // Reset lastDisconnectedAt
        lastDisconnectedAt = null;
    }

    public Date lastDisconnectedAt() {
        return lastDisconnectedAt;
    }

    public void lastDisconnectedAt(Date time) {
        lastDisconnectedAt = time;
    }

    public void disconnect() {
        if (gatt != null) {
            try {
                gatt.close();
            } catch (Throwable e) {
            }
            gatt = null;
        }
        lastDisconnectedAt(new Date());
        state(BLEDeviceState.disconnected);
    }

    public BLEDeviceGoal goal() {
        if (operatingSystem == BLEDeviceOperatingSystem.unknown) {
            return BLEDeviceGoal.operatingSystem;
        } else if (payloadData() == null) {
            return BLEDeviceGoal.payload;
        } else if (timeIntervalSinceLastPayloadShared().value > BLESensorConfiguration.payloadSharingTimeInterval.value) {
            return BLEDeviceGoal.payloadSharing;
        } else {
            return BLEDeviceGoal.rssi;
        }
    }

    public String toString() {
        return description();
    }
}
