package org.c19x.sensor.ble;

import android.bluetooth.BluetoothDevice;

import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadSharingData;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.TargetIdentifier;

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
    public void add(BLEDatabaseDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public BLEDevice device(BluetoothDevice bluetoothDevice) {
        final TargetIdentifier identifier = new TargetIdentifier(bluetoothDevice);
        BLEDevice device = database.get(identifier);
        if (device == null) {
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

    @Override
    public BLEDevice device(PayloadData payloadData) {
        BLEDevice device = null;
        for (BLEDevice candidate : database.values()) {
            if (payloadData.equals(candidate.payloadData())) {
                device = candidate;
                break;
            }
        }
        if (device == null) {
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

    @Override
    public List<BLEDevice> devices() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void delete(final TargetIdentifier identifier) {
        final BLEDevice device = database.remove(identifier);
        if (device != null) {
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    logger.debug("delete (device={})", identifier);
                    for (final BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidDelete(device);
                    }
                }
            });
        }
    }

    @Override
    public PayloadSharingData payloadSharingData(final BLEDevice peer) {
        final RSSI rssi = peer.rssi();
        if (rssi == null) {
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
            if (device.payloadData() == null) {
                continue;
            }
            // Device is iOS or receive only (Samsung J6)
            if (!(device.operatingSystem() == BLEDeviceOperatingSystem.ios || device.receiveOnly())) {
                continue;
            }
            // Payload is not the peer itself
            if (peer.payloadData() != null && (Arrays.equals(device.payloadData().value, peer.payloadData().value))) {
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
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        Collections.sort(knownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        devices.addAll(unknownDevices);
        devices.addAll(knownDevices);
        if (devices.size() == 0) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // Limit how much to share to avoid oversized data transfers over BLE
        // (512 bytes limit according to spec, 510 with response, iOS requires response)
        final List<TargetIdentifier> identifiers = new ArrayList<>();
        final Set<PayloadData> sharedPayloads = new HashSet<>(devices.size());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (BLEDevice device : devices) {
            final PayloadData payloadData = device.payloadData();
            if (payloadData == null) {
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
                identifiers.add(device.identifier);
                peer.payloadSharingData.add(payloadData);
                sharedPayloads.add(payloadData);
            } catch (Throwable e) {
            }
        }
        final Data data = new Data(byteArrayOutputStream.toByteArray());
        return new PayloadSharingData(rssi, data);
    }

    // MARK:- BLEDeviceDelegate

    @Override
    public void device(final BLEDevice device, final BLEDeviceAttribute didUpdate) {
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
