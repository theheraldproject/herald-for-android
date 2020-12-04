//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.app;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.vmware.herald.sensor.Sensor;
import com.vmware.herald.sensor.SensorArray;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.SensorState;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.PayloadDataSupplier;
import com.vmware.herald.sensor.payload.sonar.SonarPayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

public class AppDelegate extends Application implements SensorDelegate {
    private final static String tag = AppDelegate.class.getName();
    private static AppDelegate appDelegate = null;

    // Sensor for proximity detection
    private Sensor sensor = null;

    /// Generate unique and consistent device identifier for testing detection and tracking
    private int identifier() {
        final String text = Build.MODEL + ":" + Build.BRAND;
        return text.hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appDelegate = this;
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

    // MARK:- SensorDelegate for logging proximity detection events

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        Log.i(tag, sensor.name() + ",didDetect=" + didDetect);
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didReceive=" + didReceive.data.base64EncodedString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        final List<String> payloads = new ArrayList<>(didShare.size());
        for (PayloadData payloadData : didShare) {
            payloads.add(payloadData.shortName());
        }
        Log.i(tag, sensor.name() + ",didShare=" + payloads.toString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        Log.i(tag, sensor.name() + ",didVisit=" + ((null == didVisit) ? "" : didVisit.description()));
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget + ",withPayload=" + withPayload.shortName());
    }

    @Override
    public void sensor(SensorType sensor, SensorState didUpdateState) {
        Log.i(tag, sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }
}
