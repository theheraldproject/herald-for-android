//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.InertiaLocationReference;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.SensorType;

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
    private final SensorManager sensorManager;
    private final Sensor hardwareSensor;
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            try {
                final Date timestamp = new Date();
                final double x = event.values[0];
                final double y = event.values[1];
                final double z = event.values[2];
                final InertiaLocationReference inertiaLocationReference = new InertiaLocationReference(x, y, z);
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

    public ConcreteInertiaSensor(final Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.hardwareSensor = (sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        if (sensorManager == null) {
            logger.fault("init, sensor manager unavailable");
        }
        // Get hardware sensor
        if (hardwareSensor == null) {
            logger.fault("init, inertia sensor unavailable");
        }
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        // Get sensor manager
        if (sensorManager == null) {
            logger.fault("start, sensor manager unavailable");
            return;
        }
        // Get hardware sensor
        if (hardwareSensor == null) {
            logger.fault("start, inertia sensor unavailable");
            return;
        }
        // Register listener
        logger.debug("start");
        sensorManager.unregisterListener(sensorEventListener);
        sensorManager.registerListener(sensorEventListener, hardwareSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void stop() {
        if (sensorManager == null) {
            logger.fault("stop, sensor manager unavailable");
            return;
        }
        // Unregister listener
        logger.debug("stop");
        sensorManager.unregisterListener(sensorEventListener);
    }
}
