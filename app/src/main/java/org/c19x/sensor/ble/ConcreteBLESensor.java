package org.c19x.sensor.ble;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import org.c19x.sensor.PayloadDataSupplier;
import org.c19x.sensor.Sensor;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.ProximityMeasurementUnit;
import org.c19x.sensor.datatype.RSSI;
import org.c19x.sensor.datatype.SensorType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLESensor implements Sensor, BLEDatabaseDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLESensor");
    private final Context context;
    private final List<SensorDelegate> delegates = new ArrayList<>();
    private final BLEDatabase database = new ConcreteBLEDatabase();
    private final BluetoothStateManager bluetoothStateManager;
    private final BLETransmitter transmitter;
    private final BLEReceiver receiver;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();

    public ConcreteBLESensor(Context context, PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
        bluetoothStateManager = new ConcreteBluetoothStateManager(context);
        transmitter = new ConcreteBLETransmitter(context, bluetoothStateManager, payloadDataSupplier, database);
        receiver = new ConcreteBLEReceiver(context, bluetoothStateManager, payloadDataSupplier, database, transmitter);
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
        logger.debug("didDetect (device={})", device.identifier);
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
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, new Double(rssi.value));
                logger.debug("didMeasure (device={},proximity={})", device.identifier, proximity.description());
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
                logger.debug("didRead (device={},payloadData={})", device.identifier, payloadData.shortName());
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
            case payloadSharingData: {
                final List<PayloadData> payloadSharingData = device.payloadSharingData();
                if (payloadSharingData == null) {
                    return;
                }
                logger.debug("didShare (device={},payloadSharingData={})", device.identifier, payloadSharingData);
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, payloadSharingData, device.identifier);
                        }
                    }
                });
                break;
            }
            default: {
                return;
            }
        }
    }

    @Override
    public void bleDatabaseDidDelete(BLEDevice device) {
        logger.debug("didDelete (device={})", device.identifier);
    }

    /**
     * Check permissions for BLESensor. Add this to Activity.
     *
     * @param activity
     */
    public final static void checkPermissions(final Activity activity) {
        final String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        final String backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
        if (ActivityCompat.checkSelfPermission(activity, locationPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{locationPermission}, 0);
        }
        if (ActivityCompat.checkSelfPermission(activity, backgroundLocationPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{backgroundLocationPermission}, 0);
        }
    }

}
