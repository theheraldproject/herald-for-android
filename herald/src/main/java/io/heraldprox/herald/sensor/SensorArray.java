//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;
import io.heraldprox.herald.sensor.ble.ConcreteBLESensor;
import io.heraldprox.herald.sensor.data.CalibrationLog;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.motion.ConcreteInertiaSensor;
import io.heraldprox.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;
import io.heraldprox.herald.sensor.protocol.ConcreteGPDMPProtocolStack;
import io.heraldprox.herald.sensor.protocol.GPDMPLayer7Incoming;
import io.heraldprox.herald.sensor.protocol.GPDMPMessageListenerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sensor array for combining multiple detection and tracking methods.
 */
public class SensorArray implements Sensor {
    private final static AtomicInteger instanceIdentifier = new AtomicInteger(0);
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray[" + instanceIdentifier.getAndIncrement() + "]");
    private final List<Sensor> sensorArray = new ArrayList<>();
    private final List<SensorDelegate> delegates = new ArrayList<>();
    @NonNull
    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";

    @NonNull
    private final ConcreteBLESensor concreteBleSensor;

    private final ConcreteGPDMPProtocolStack gpdmpStack = ConcreteGPDMPProtocolStack.createDefaultBluetoothLEStack();

    public SensorArray(@NonNull final Context context, @NonNull final PayloadDataSupplier payloadDataSupplier) {
        // Ensure logger has been initialised (should have happened in AppDelegate already)
        ConcreteSensorLogger.context(context);
        logger.debug("init");

        // Define sensor array
        concreteBleSensor = new ConcreteBLESensor(context, payloadDataSupplier);
        sensorArray.add(concreteBleSensor);
        // Inertia sensor configured for automated RSSI-distance calibration data capture
        if (BLESensorConfiguration.inertiaSensorEnabled) {
            logger.debug("Inertia sensor enabled");
            sensorArray.add(new ConcreteInertiaSensor(context));
            add(new CalibrationLog(context, "calibration.csv"));
        }
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp(), null);
        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), SensorArray.deviceDescription);

        if (BLESensorConfiguration.gpdmpEnabled) {
            concreteBleSensor.injectGPDMPLayers(gpdmpStack);
        }
    }

    /**
     * Immediate send data.
     * @param data Data to be sent immediately to target device.
     * @param targetIdentifier Identifier of target device.
     * @return True for success, false otherwise.
     */
    public boolean immediateSend(@NonNull final Data data, @NonNull final TargetIdentifier targetIdentifier) {
        return concreteBleSensor.immediateSend(data,targetIdentifier);
    }

    /**
     * Immediate send to all (connected / recent / nearby).
     * @param data Data to be sent immediately to all devices.
     * @return True for success, false otherwise.
     */
    public boolean immediateSendAll(@NonNull final Data data) {
        return concreteBleSensor.immediateSendAll(data);
    }

    /**
     * GPDMP send function
     * @return the sent message UUID
     */
    public UUID gpdmpSend(UUID channelId, Date timeToAccess, Date timeout, UInt16 ttl,
                             UInt16 minTransmissions, UInt16 maxTransmissions,
                             UUID mySenderRecipientId,
                             List<ConcreteExtendedDataSectionV1> sections) {
        return gpdmpStack.getLayer7().sendOutgoing(channelId, timeToAccess, timeout, ttl,
                minTransmissions, maxTransmissions, mySenderRecipientId, sections);
    }

    /**
     * Fetch the GPDMP interface so we can listen for new incoming messages after decoding.
     *
     * @return The Layer7 messaging listener manager that allows the caller to add/remove a message listener
     */
    public GPDMPMessageListenerManager getGPDMPMessageListenerManager() {
        return (GPDMPMessageListenerManager) gpdmpStack.getLayer7();
    }

    @NonNull
    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        delegates.add(delegate);
        for (final Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (final Sensor sensor : sensorArray) {
            sensor.start();
        }
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.ARRAY, SensorState.on);
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.ARRAY, SensorState.off);
        }
    }
}
