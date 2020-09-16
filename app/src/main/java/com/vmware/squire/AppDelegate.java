//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire;

import android.app.Application;
import android.os.Build;

import com.vmware.squire.sensor.payload.PayloadDataSupplier;
import com.vmware.squire.sensor.Sensor;
import com.vmware.squire.sensor.SensorArray;
import com.vmware.squire.sensor.SensorDelegate;
import com.vmware.squire.sensor.data.ConcreteSensorLogger;
import com.vmware.squire.sensor.data.SensorLogger;
import com.vmware.squire.sensor.datatype.Location;
import com.vmware.squire.sensor.datatype.PayloadData;
import com.vmware.squire.sensor.datatype.Proximity;
import com.vmware.squire.sensor.datatype.SensorState;
import com.vmware.squire.sensor.datatype.SensorType;
import com.vmware.squire.sensor.datatype.TargetIdentifier;
import com.vmware.squire.sensor.payload.sonar.SonarPayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

public class AppDelegate extends Application implements SensorDelegate {
    public final static int appIcon = R.drawable.virus;
    private static AppDelegate appDelegate;

    // Logger must be initialised after context has been established
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
        sensor.stop();
        super.onTerminate();
    }

    /// Get app delegate
    public static AppDelegate getAppDelegate() {
        return appDelegate;
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
