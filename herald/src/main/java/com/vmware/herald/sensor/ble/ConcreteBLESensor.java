//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble;

import android.content.Context;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.BluetoothState;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.ProximityMeasurementUnit;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.SensorState;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.PayloadDataSupplier;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLESensor implements BLESensor, BLEDatabaseDelegate, BluetoothStateManagerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLESensor");
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final BLETransmitter transmitter;
    private final BLEReceiver receiver;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    // Record payload data to enable de-duplication
    private final Map<PayloadData, Date> didReadPayloadData = new ConcurrentHashMap<>();

    public ConcreteBLESensor(Context context, PayloadDataSupplier payloadDataSupplier) {
        final BluetoothStateManager bluetoothStateManager = new ConcreteBluetoothStateManager(context);
        final BLEDatabase database = new ConcreteBLEDatabase();
        final BLETimer timer = new BLETimer(context);
        bluetoothStateManager.delegates.add(this);
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

    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        return receiver.immediateSend(data, targetIdentifier);
    }

    public boolean immediateSendAll(Data data) {
        return receiver.immediateSendAll(data);
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
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi.value, device.calibration());
                logger.debug("didMeasure (device={},payloadData={},proximity={})", device, device.payloadData(), proximity.description());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier);
                        }
                    }
                });
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier, payloadData);
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
                // De-duplicate payload in recent time
                if (BLESensorConfiguration.filterDuplicatePayloadData != TimeInterval.never) {
                    final long removePayloadDataBefore = new Date().getTime() - BLESensorConfiguration.filterDuplicatePayloadData.millis();
                    for (Map.Entry<PayloadData, Date> entry : didReadPayloadData.entrySet()) {
                        if (entry.getValue().getTime() < removePayloadDataBefore) {
                            didReadPayloadData.remove(entry.getKey());
                        }
                    }
                    final Date lastReportedAt = didReadPayloadData.get(payloadData);
                    if (lastReportedAt != null) {
                        logger.debug("didRead, filtered duplicate (device={},payloadData={},lastReportedAt={})", device, device.payloadData().shortName(), lastReportedAt);
                        return;
                    }
                    didReadPayloadData.put(payloadData, new Date());
                }
                // Notify delegates
                logger.debug("didRead (device={},payloadData={})", device, device.payloadData().shortName());
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

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
        logger.debug("didUpdateState (state={})", didUpdateState);
        SensorState sensorState = SensorState.off;
        if (didUpdateState == BluetoothState.poweredOn) {
            sensorState = SensorState.on;
        } else if (didUpdateState == BluetoothState.unsupported) {
            sensorState = SensorState.unavailable;
        }
        for (SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.BLE, sensorState);
        }
    }
}
