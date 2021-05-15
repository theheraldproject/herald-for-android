//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;

/// Foreground service for enabling continuous BLE operation in background
public class ForegroundService extends Service {
    public static final String ACTION_START = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP = "ACTION_STOP_FOREGROUND_SERVICE";
    private final SensorLogger logger = new ConcreteSensorLogger("App", "ForegroundService");

    @Override
    public void onCreate() {
        logger.debug("onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand");

        if (null != intent) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_START:
                    this.startForegroundService();
                    break;
                case ACTION_STOP:
                    this.stopForegroundService();
                    break;
            }
        }

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

    private void startForegroundService() {
        logger.debug("starting foreground service");
        final NotificationService notificationService = NotificationService.shared(getApplication());
        startForeground(notificationService.getForegroundServiceNotificationId(), notificationService.getForegroundServiceNotification());
    }

    private void stopForegroundService() {
        logger.debug("stopping foreground service");
        stopForeground(true);
        stopSelf();
    }
}