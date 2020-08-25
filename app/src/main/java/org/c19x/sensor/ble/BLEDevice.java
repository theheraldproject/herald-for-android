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
import java.util.Queue;

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
    /// Device operating system, this is necessary for selecting different interaction procedures for each platform.
    private BLEDeviceOperatingSystem operatingSystem = BLEDeviceOperatingSystem.unknown;
    /// Payload data acquired from the device via payloadCharacteristic read, e.g. C19X beacon code or Sonar encrypted identifier
    private PayloadData payloadData;
    /// Payload data last update timestamp, this is used to determine what needs to be shared with peers.
    private Date payloadDataLastUpdatedAt = null;
    /// Most recent RSSI measurement taken by readRSSI or didDiscover.
    private RSSI rssi;
    /// RSSI last update timestamp, this is used to track last advertised at without relying on didDiscover
    private Date rssiLastUpdatedAt = null;
    /// Transmit power data where available (only provided by Android devices)
    private BLE_TxPower txPower;
    /// Is device receive only?
    private boolean receiveOnly = false;
    /// Ignore logic
    private TimeInterval ignoreForDuration = null;
    private Date ignoreUntil = null;

    /// BLE characteristics
    private BluetoothGattCharacteristic signalCharacteristic = null;
    private BluetoothGattCharacteristic payloadCharacteristic = null;
    protected byte[] signalCharacteristicWriteValue = null;
    protected Queue<byte[]> signalCharacteristicWriteQueue = null;

    /// Track connection timestamps
    private Date lastDiscoveredAt = null;
    private Date lastConnectRequestedAt = null;
    private Date lastConnectedAt = null;
    private Date lastDisconnectedAt = null;

    /// Payload data already shared with this peer
    protected final List<PayloadData> payloadSharingData = new ArrayList<>();

    /// Track write timestamps
    private Date lastWritePayloadAt = null;
    private Date lastWriteRssiAt = null;
    private Date lastWritePayloadSharingAt = null;

    /// Last advert timestamp, inferred from payloadDataLastUpdatedAt, rssiLastUpdatedAt
    public Date lastAdvertAt() {
        long max = createdAt.getTime();
        if (lastDiscoveredAt != null) {
            max = Math.max(max, lastDiscoveredAt.getTime());
        }
        if (payloadDataLastUpdatedAt != null) {
            max = Math.max(max, payloadDataLastUpdatedAt.getTime());
        }
        if (rssiLastUpdatedAt != null) {
            max = Math.max(max, rssiLastUpdatedAt.getTime());
        }
        return new Date(max);
    }

    public TimeInterval timeIntervalSinceConnected() {
        if (state() != BLEDeviceState.connected) {
            return TimeInterval.zero;
        }
        if (lastConnectedAt == null) {
            return TimeInterval.zero;
        }
        return new TimeInterval((new Date().getTime() - lastConnectedAt.getTime()) / 1000);
    }

    public TimeInterval upTime() {
        if (state() == BLEDeviceState.disconnected) {
            return TimeInterval.zero;
        }
        if (payloadDataLastUpdatedAt == null) {
            return TimeInterval.zero;
        }
        return new TimeInterval((lastAdvertAt().getTime() - payloadDataLastUpdatedAt.getTime()) / 1000);
    }

    public TimeInterval downTime() {
        if (state() == BLEDeviceState.connected) {
            return TimeInterval.zero;
        }
        if (lastDisconnectedAt == null) {
            return new TimeInterval((new Date().getTime() - createdAt.getTime()) / 1000);
        }
        return new TimeInterval((new Date().getTime() - lastDisconnectedAt.getTime()) / 1000);
    }

    /// Time interval since last attribute value update, this is used to identify devices that may have expired and should be removed from the database.
    public TimeInterval timeIntervalSinceLastUpdate() {
        return new TimeInterval((new Date().getTime() - lastUpdatedAt.getTime()) / 1000);
    }

    /// Time interval between last connection request, this is used to priortise disconnections
    public TimeInterval timeIntervalSinceLastConnectRequestedAt() {
        if (lastConnectRequestedAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastConnectRequestedAt.getTime()) / 1000);
    }

    public String description() {
        return "BLEDevice[id=" + identifier + ",os=" + operatingSystem + ",payload=" + payloadData() + "]";
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
        if (this.peripheral != peripheral) {
            this.peripheral = peripheral;
            lastUpdatedAt = new Date();
            delegate.device(this, BLEDeviceAttribute.peripheral);
        }
    }

    public BLEDeviceState state() {
        return state;
    }

    public void state(BLEDeviceState state) {
        this.state = state;
        lastUpdatedAt = new Date();
        if (state == BLEDeviceState.connecting) {
            lastConnectRequestedAt = lastUpdatedAt;
        } else if (state == BLEDeviceState.connected) {
            lastConnectedAt = lastUpdatedAt;
        } else if (state == BLEDeviceState.disconnected) {
            lastDisconnectedAt = lastUpdatedAt;
        }
        delegate.device(this, BLEDeviceAttribute.state);
    }

    public BLEDeviceOperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public void operatingSystem(BLEDeviceOperatingSystem operatingSystem) {
        lastUpdatedAt = new Date();
        // Set ignore timer
        if (operatingSystem == BLEDeviceOperatingSystem.ignore) {
            if (ignoreForDuration == null) {
                ignoreForDuration = TimeInterval.minute;
            } else if (ignoreForDuration.value < TimeInterval.minutes(3).value) {
                ignoreForDuration = new TimeInterval(Math.round(ignoreForDuration.value * 1.2));
            }
            ignoreUntil = new Date(lastUpdatedAt.getTime() + ignoreForDuration.millis());
        } else {
            ignoreForDuration = null;
            ignoreUntil = null;
        }
        if (this.operatingSystem != operatingSystem) {
            this.operatingSystem = operatingSystem;
            delegate.device(this, BLEDeviceAttribute.operatingSystem);
        }
    }

    /// Should ignore this device for now.
    public boolean ignore() {
        if (ignoreUntil == null) {
            return false;
        }
        if (new Date().getTime() < ignoreUntil.getTime()) {
            return true;
        }
        return false;
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
    }

    public BluetoothGattCharacteristic signalCharacteristic() {
        return signalCharacteristic;
    }

    public void signalCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.signalCharacteristic = characteristic;
        lastUpdatedAt = new Date();
    }

    public BluetoothGattCharacteristic payloadCharacteristic() {
        return payloadCharacteristic;
    }

    public void payloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.payloadCharacteristic = characteristic;
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

    public TimeInterval timeIntervalSinceLastWritePayload() {
        if (lastWritePayloadAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadAt.getTime()) / 1000);
    }

    public void registerWriteRssi() {
        lastUpdatedAt = new Date();
        lastWriteRssiAt = lastUpdatedAt;
    }

    public TimeInterval timeIntervalSinceLastWriteRssi() {
        if (lastWriteRssiAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWriteRssiAt.getTime()) / 1000);
    }

    public void registerWritePayloadSharing() {
        lastUpdatedAt = new Date();
        lastWritePayloadSharingAt = lastUpdatedAt;
    }

    public TimeInterval timeIntervalSinceLastWritePayloadSharing() {
        if (lastWritePayloadSharingAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadSharingAt.getTime()) / 1000);
    }

    public String toString() {
        return description();
    }
}
