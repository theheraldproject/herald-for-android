package org.c19x.sensor.ble;

import android.content.Context;

import org.c19x.sensor.PayloadDataSupplier;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.ProximityMeasurementUnit;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.SensorType;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLESensor implements BLESensor, BLEDatabaseDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLESensor");
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final BLEDatabase database = new ConcreteBLEDatabase();
    private final BluetoothStateManager bluetoothStateManager;
    private final BLETimer timer;
    private final BLETransmitter transmitter;
    private final BLEReceiver receiver;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();

    public ConcreteBLESensor(Context context, PayloadDataSupplier payloadDataSupplier) {
        bluetoothStateManager = new ConcreteBluetoothStateManager(context);
        timer = new BLETimer(context);
        transmitter = new ConcreteBLETransmitter(context, bluetoothStateManager, timer, payloadDataSupplier, database);
        receiver = new ConcreteBLEReceiver(context, bluetoothStateManager, timer, database, transmitter);
        database.add(this);
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
        transmitter.add(delegate);
        receiver.add(delegate);
    }

    @Override
    public void start() {
        logger.debug("start");
        // BLE transmitter and receivers start on powerOn event
        transmitter.start();
        receiver.start();
    }

    @Override
    public void stop() {
        logger.debug("stop");
        // BLE transmitter and receivers stops on powerOff event
        transmitter.stop();
        receiver.stop();
    }

    // MARK:- BLEDatabaseDelegate

    @Override
    public void bleDatabaseDidCreate(final BLEDevice device) {
        logger.debug("didDetect (device={},payloadData={})", device.identifier, device.payloadData());
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                for (SensorDelegate delegate : delegates) {
                    delegate.sensor(SensorType.BLE, device.identifier);
                }
            }
        });
    }

    @Override
    public void bleDatabaseDidUpdate(final BLEDevice device, BLEDeviceAttribute attribute) {
        switch (attribute) {
            case rssi: {
                final RSSI rssi = device.rssi();
                if (rssi == null) {
                    return;
                }
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi.value);
                logger.debug("didMeasure (device={},payloadData={},proximity={})", device, device.payloadData(), proximity.description());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier);
                        }
                    }
                });
                break;
            }
            case payloadData: {
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }
                logger.debug("didRead (device={},payloadData={},payloadData={})", device, device.payloadData(), payloadData.shortName());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, payloadData, device.identifier);
                        }
                    }
                });
                break;
            }
            default: {
            }
        }
    }

    @Override
    public void bleDatabaseDidDelete(BLEDevice device) {
        logger.debug("didDelete (device={})", device.identifier);
    }
}
