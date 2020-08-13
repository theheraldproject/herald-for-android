package org.c19x.sensor.ble;

import android.bluetooth.BluetoothDevice;

import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLEDatabase implements BLEDatabase, BLEDeviceDelegate {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEDatabase");
    private List<BLEDatabaseDelegate> delegates = new ArrayList<>();
    private Map<TargetIdentifier, BLEDevice> database = new ConcurrentHashMap<>();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();

    @Override
    public void add(BLEDatabaseDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public synchronized BLEDevice device(final TargetIdentifier identifier) {
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
        return device;
    }

    @Override
    public synchronized BLEDevice device(BluetoothDevice bluetoothDevice) {
        final TargetIdentifier identifier = new TargetIdentifier(bluetoothDevice);
        final BLEDevice device = device(identifier);
        device.peripheral(bluetoothDevice);
        return device;
    }

    @Override
    public BLEDevice device(PayloadData payload) {
        for (BLEDevice device : devices()) {
            final PayloadData existingPayloadData = device.payloadData();
            if (existingPayloadData != null && existingPayloadData.equals(payload)) {
                return device;
            }
        }
        // Create temporary UUID, the taskRemoveDuplicatePeripherals function
        // will delete this when a direct connection to the peripheral has been
        // established
        final TargetIdentifier identifier = new TargetIdentifier(UUID.randomUUID().toString());
        final BLEDevice placeholder = device(identifier);
        placeholder.payloadData(payload);
        return placeholder;
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

    // MARK:- BLEDeviceDelegate

    @Override
    public void device(final BLEDevice device, final BLEDeviceAttribute didUpdate) {
        queue.execute(new Runnable() {
            @Override
            public void run() {
                //logger.debug("update (device={},attribute={})", device.identifier, didUpdate.name());
                for (BLEDatabaseDelegate delegate : delegates) {
                    delegate.bleDatabaseDidUpdate(device, didUpdate);
                }
            }
        });
    }
}
