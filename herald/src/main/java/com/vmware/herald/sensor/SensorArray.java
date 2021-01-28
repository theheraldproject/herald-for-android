//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor;

import android.content.Context;

import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.ble.ConcreteBLESensor;
import com.vmware.herald.sensor.data.CalibrationLog;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.motion.ConcreteInertiaSensor;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final Context context;
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";

    private final ConcreteBLESensor concreteBleSensor;

    public SensorArray(final Context context, PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
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
    }

    /// Immediate send data.
    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        return concreteBleSensor.immediateSend(data,targetIdentifier);
    }

    /// Immediate send to all (connected / recent / nearby)
    public boolean immediateSendAll(Data data) {
        return concreteBleSensor.immediateSendAll(data);
    }

    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }
}
