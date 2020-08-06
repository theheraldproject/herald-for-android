package org.c19x.sensor.ble;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.c19x.sensor.PayloadDataSupplier;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.BluetoothState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLEReceiver implements BLEReceiver, BluetoothStateManagerDelegate {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEReceiver");
    private final ConcreteBLEReceiver self = this;
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final PayloadDataSupplier payloadDataSupplier;
    private final BLEDatabase database;
    private final Handler handler;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();

    /**
     * Receiver starts automatically when Bluetooth is enabled.
     */
    public ConcreteBLEReceiver(Context context, BluetoothStateManager bluetoothStateManager, PayloadDataSupplier payloadDataSupplier, BLEDatabase database) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadDataSupplier = payloadDataSupplier;
        this.database = database;
        this.handler = new Handler(Looper.getMainLooper());
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {

    }
}
