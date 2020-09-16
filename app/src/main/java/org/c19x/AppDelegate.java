//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import org.c19x.sensor.R;
import org.c19x.sensor.service.ForegroundService;
import org.c19x.sensor.payload.PayloadDataSupplier;
import org.c19x.sensor.Sensor;
import org.c19x.sensor.SensorArray;
import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.Location;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.SensorState;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.TargetIdentifier;
import org.c19x.sensor.payload.sonar.SonarPayloadDataSupplier;
import org.c19x.sensor.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

public class AppDelegate extends Application implements SensorDelegate {
    private static AppDelegate appDelegate;

    // Logger must be initialised after context has been established
    private NotificationService notificationService;
    private SensorLogger logger;
    private Sensor sensor;

    /// Generate unique and consistent device identifier for testing detection and tracking
    private int identifier() {
        final String text = Build.MODEL + ":" + Build.BRAND;
        return text.hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appDelegate = this;
        // Set logger context to enable logging to plain text file
        ConcreteSensorLogger.context(getApplicationContext());
        logger = new ConcreteSensorLogger("Sensor", "AppDelegate");
        // Notification service enables foreground service for running background scan
        notificationService = new NotificationService(this, R.drawable.virus);
        // Start foreground service to enable background scan
        final Intent intent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        // Initialise sensor array for given payload data supplier
        final PayloadDataSupplier payloadDataSupplier = new SonarPayloadDataSupplier(identifier());
        sensor = new SensorArray(getApplicationContext(), payloadDataSupplier);
        // Add appDelegate as listener for detection events for logging and start sensor
        sensor.add(this);
        // Sensor will start and stop with Bluetooth power on / off events
        sensor.start();
    }

    @Override
    public void onTerminate() {
        final Intent intent = new Intent(this, ForegroundService.class);
        stopService(intent);
        super.onTerminate();
    }

    /// Get app delegate
    public static AppDelegate getAppDelegate() {
        return appDelegate;
    }

    /// Get notification service for foreground service (and other app components).
    public NotificationService notificationService() {
        return notificationService;
    }

    /// Get sensor
    public Sensor sensor() {
        return sensor;
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        logger.info(sensor.name() + ",didDetect=" + didDetect);
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        logger.info(sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        final List<String> payloads = new ArrayList<>(didShare.size());
        for (PayloadData payloadData : didShare) {
            payloads.add(payloadData.shortName());
        }
        logger.info(sensor.name() + ",didShare=" + payloads.toString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        logger.info(sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        logger.info(sensor.name() + ",didVisit=" + didVisit.description());
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        logger.info(sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget + ",withPayload=" + withPayload.shortName());
    }

    @Override
    public void sensor(SensorType sensor, SensorState didUpdateState) {
        logger.info(sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }
}
