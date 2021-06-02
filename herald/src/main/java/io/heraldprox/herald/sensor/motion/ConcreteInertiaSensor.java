//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    @NonNull
    private final SensorManager sensorManager;
    @Nullable
    private final Sensor hardwareSensor;
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(@NonNull final SensorEvent event) {
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
            }
        }

        @Override
        public void onAccuracyChanged(@NonNull final Sensor sensor, final int accuracy) {
        }
    };

    public ConcreteInertiaSensor(@NonNull final Context context) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.hardwareSensor = (null == sensorManager ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        if (null == sensorManager) {
            logger.fault("init, sensor manager unavailable");
        }
        // Get hardware sensor
        if (null == hardwareSensor) {
            logger.fault("init, inertia sensor unavailable");
        }
    }

    @Override
    public void add(@NonNull final SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        // Get sensor manager
        //noinspection ConstantConditions
        if (null == sensorManager) {
            logger.fault("start, sensor manager unavailable");
            return;
        }
        // Get hardware sensor
        if (null == hardwareSensor) {
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
        //noinspection ConstantConditions
        if (null == sensorManager) {
            logger.fault("stop, sensor manager unavailable");
            return;
        }
        // Unregister listener
        logger.debug("stop");
        sensorManager.unregisterListener(sensorEventListener);
    }
}
