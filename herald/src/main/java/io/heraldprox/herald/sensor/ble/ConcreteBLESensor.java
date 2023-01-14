//  Copyright 2020-2022 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.BluetoothState;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.protocol.ConcreteGPDMPProtocolStack;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer1BluetoothLEIncoming;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer1BluetoothLEManager;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer1BluetoothLEOutgoing;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer2BluetoothLEIncoming;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteBLESensor implements BLESensor, BLEDatabaseDelegate, BluetoothStateManagerDelegate, GPDMPLayer1BluetoothLEManager, GPDMPLayer1BluetoothLEOutgoing, GPDMPLayer1BluetoothLEIncoming {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLESensor");
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    @NonNull
    private final BluetoothStateManager bluetoothStateManager;
    @NonNull
    private final BLETimer timer;
    @NonNull
    private final BLETransmitter transmitter;
    @NonNull
    private final BLEReceiver receiver;
    private GPDMPLayer2BluetoothLEIncoming gpdmpIncoming = null;
    private final BLEDatabase database = new ConcreteBLEDatabase();
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    // Record payload data to enable de-duplication
    private final Map<PayloadData, Date> didReadPayloadData = new ConcurrentHashMap<>();

    public ConcreteBLESensor(@NonNull final Context context, @NonNull final PayloadDataSupplier payloadDataSupplier) {
        bluetoothStateManager = new ConcreteBluetoothStateManager(context);
        timer = new BLETimer(context);
        bluetoothStateManager.delegates.add(this);
        transmitter = new ConcreteBLETransmitter(context, bluetoothStateManager, timer, payloadDataSupplier, database, this);
        receiver = new ConcreteBLEReceiver(context, bluetoothStateManager, timer, database, transmitter, payloadDataSupplier);
        database.add(this);
    }

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        delegates.add(delegate);
        transmitter.add(delegate);
        receiver.add(delegate);
    }

    @Override
    public void start() {
        logger.debug("start");
        transmitter.start();
        receiver.start();
    }

    @Override
    public void stop() {
        logger.debug("stop");
        transmitter.stop();
        receiver.stop();
    }

    public boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier) {
        return receiver.immediateSend(data, targetIdentifier);
    }

    public boolean immediateSendAll(@NonNull final Data data) {
        return receiver.immediateSendAll(data);
    }

    // MARK:- BLEDatabaseDelegate

    @Override
    public void bleDatabaseDidCreate(@NonNull final BLEDevice device) {
        logger.debug("didDetect (device={},payloadData={})", device.identifier, device.payloadData());
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                for (final SensorDelegate delegate : delegates) {
                    delegate.sensor(SensorType.BLE, device.identifier);
                }
            }
        });
    }

    @Override
    public void bleDatabaseDidUpdate(@NonNull final BLEDevice device, @NonNull final BLEDeviceAttribute attribute) {
        switch (attribute) {
            case rssi: {
                final RSSI rssi = device.rssi();
                if (null == rssi) {
                    return;
                }
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, rssi.value, device.calibration());
                logger.debug("didMeasure (device={},payloadData={},proximity={})", device, device.payloadData(), proximity.description());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (final SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier);
                        }
                    }
                });
                final PayloadData payloadData = device.payloadData();
                if (null == payloadData) {
                    return;
                }
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (final SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier, payloadData);
                        }
                    }
                });
                break;
            }
            case payloadData: {
                final PayloadData payloadData = device.payloadData();
                if (null == payloadData) {
                    return;
                }
                // De-duplicate payload in recent time
                if (BLESensorConfiguration.filterDuplicatePayloadData != TimeInterval.never) {
                    final long removePayloadDataBefore = new Date().getTime() - BLESensorConfiguration.filterDuplicatePayloadData.millis();
                    for (final Map.Entry<PayloadData, Date> entry : didReadPayloadData.entrySet()) {
                        if (entry.getValue().getTime() < removePayloadDataBefore) {
                            didReadPayloadData.remove(entry.getKey());
                        }
                    }
                    final Date lastReportedAt = didReadPayloadData.get(payloadData);
                    if (null != lastReportedAt) {
                        logger.debug("didRead, filtered duplicate (device={},payloadData={},lastReportedAt={})", device, payloadData.shortName(), lastReportedAt);
                        return;
                    }
                    didReadPayloadData.put(payloadData, new Date());
                }
                // Notify delegates
                logger.debug("didRead (device={},payloadData={})", device, payloadData.shortName());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (final SensorDelegate delegate : delegates) {
                            // Confirm it's a Herald Payload device (didDeleteOrDetect)
                            delegate.sensor(SensorType.BLE, true, device.identifier);
                            // Now share that payload
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
    public void bleDatabaseDidDelete(@NonNull final BLEDevice device) {
        logger.debug("didDelete (device={})", device.identifier);
        for (SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.BLE, false, device.identifier);
        }
    }

    // MARK:- BluetoothStateManagerDelegate

    @Override
    public void bluetoothStateManager(@NonNull final BluetoothState didUpdateState) {
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

    // MARK:- GPDMP support

    /**
     * Sets this Bluetooth layer (and thus our managed ConcreteBLEReceiver) as GPDMP Layer1.
     * @param stack The full GPDMP messaging stack to configure for Herald Bluetooth transport.
     */
    public void injectGPDMPLayers(ConcreteGPDMPProtocolStack stack) {
        stack.replaceLayer1(this);
    }

    @Override
    public void setIncoming(GPDMPLayer2BluetoothLEIncoming in) {
        gpdmpIncoming = in;
    }

    @Override
    public GPDMPLayer1BluetoothLEOutgoing getOutgoingInterface() {
        return this;
    }

    @Override
    public void outgoing(TargetIdentifier sendTo, PayloadData data, UUID gpdmpMessageTransportRequestId) {
        // Send message via receiver (immediate send for now, secured payload characteristic later)
        // TODO replace this mechanism with the Secured Payload characteristic
        receiver.immediateSend(data,sendTo);
    }

    @Override
    /**
     * GPDMP Layer 1 Incoming. Used internally to Herald only, although included in Herald
     * GPDMP API for convenience.
     */
    public void incoming(TargetIdentifier from, PayloadData data) {
        // Receive raw data from the ConcreteBLETransmitter
        // We just pass this onto the layer 2, if set
        if (null != gpdmpIncoming) {
            gpdmpIncoming.incoming(from,data);
        }
    }
}
