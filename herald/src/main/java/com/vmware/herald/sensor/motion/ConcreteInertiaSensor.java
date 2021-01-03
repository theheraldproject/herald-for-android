//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.InertiaLocationReference;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.SensorType;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteInertiaSensor implements InertiaSensor {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Motion.ConcreteInertiaSensor");
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Context context;
    private final double threshold;
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION) {
                return;
            }
            try {
                final Date timestamp = new Date();
                final double x = event.values[0];
                final double y = event.values[1];
                final double z = event.values[2];
                final InertiaLocationReference inertiaLocationReference = new InertiaLocationReference(x, y, z);
                if (inertiaLocationReference.magnitude < threshold) {
                    return;
                }
                final Location didVisit = new Location(inertiaLocationReference, timestamp, timestamp);
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (final SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.ACCELEROMETER, didVisit);
                        }
                    }
                });
            } catch (Throwable e) {
                logger.fault("onSensorChanged failed to get sensor data", e);
                return;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private SensorManager sensorManager;
    private Sensor hardwareSensor;

    public ConcreteInertiaSensor(final Context context, final double threshold) {
        this.context = context;
        this.threshold = threshold;
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        // Get sensor manager
        if (sensorManager == null) {
            this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager == null) {
            logger.fault("start, sensor manager unavailable");
            return;
        }
        // Get hardware sensor
        if (hardwareSensor == null) {
            hardwareSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
        if (hardwareSensor == null) {
            logger.fault("start, inertia sensor unavailable");
            return;
        }
        // Register listener
        sensorManager.unregisterListener(sensorEventListener);
        sensorManager.registerListener(sensorEventListener, hardwareSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void stop() {
        if (sensorManager == null) {
            logger.fault("stop, sensor manager unavailable");
            return;
        }
        sensorManager.unregisterListener(sensorEventListener);
    }
}
