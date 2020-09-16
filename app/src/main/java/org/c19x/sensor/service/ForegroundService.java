//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package org.c19x.sensor.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.c19x.AppDelegate;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.Tuple;

/// Foreground service for enabling continuous BLE operation in background
public class ForegroundService extends Service {
    private final SensorLogger logger = new ConcreteSensorLogger("App", "ForegroundService");

    @Override
    public void onCreate() {
        logger.debug("onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand");
        Tuple<Integer, Notification> notification = AppDelegate.getAppDelegate().notificationService().notification();
        if (notification.b == null) {
            notification = AppDelegate.getAppDelegate().notificationService().notification("Contact Tracing", "Sensor is working");
        }
        startForeground(notification.a, notification.b);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        logger.debug("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}