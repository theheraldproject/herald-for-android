//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.vmware.squire.sensor.ble.ConcreteBLESensor;
import com.vmware.squire.sensor.data.BatteryLog;
import com.vmware.squire.sensor.data.ConcreteSensorLogger;
import com.vmware.squire.sensor.data.ContactLog;
import com.vmware.squire.sensor.data.DetectionLog;
import com.vmware.squire.sensor.data.SensorLogger;
import com.vmware.squire.sensor.data.StatisticsLog;
import com.vmware.squire.sensor.datatype.PayloadData;
import com.vmware.squire.sensor.datatype.PayloadTimestamp;
import com.vmware.squire.sensor.payload.PayloadDataSupplier;
import com.vmware.squire.sensor.service.ForegroundService;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final Context context;
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";


    public SensorArray(Context context, PayloadDataSupplier payloadDataSupplier) {
        this.context = context;
        // Ensure logger has been initialised (should have happened in AppDelegate already)
        ConcreteSensorLogger.context(context);
        logger.debug("init");

        // Start foreground service to enable background scan
        final Intent intent = new Intent(context, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Define sensor array
        sensorArray.add(new ConcreteBLESensor(context, payloadDataSupplier));

        // Loggers
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
        add(new ContactLog(context, "contacts.csv"));
        add(new StatisticsLog(context, "statistics.csv", payloadData));
        add(new DetectionLog(context,"detection.csv", payloadData));
        new BatteryLog(context, "battery.csv");

        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), deviceDescription);
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
