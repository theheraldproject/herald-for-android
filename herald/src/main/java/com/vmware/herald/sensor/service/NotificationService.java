//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.service;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vmware.herald.sensor.datatype.Triple;
import com.vmware.herald.sensor.datatype.Tuple;

/// Notification service for enabling foreground service (notification must be displayed to show app is running in the background).
public class NotificationService {
    private static NotificationService shared = null;
    private static Application application = null;
    private final Context context;
    private int notificationId;
    private Notification notification;

    private NotificationService(final Application application) {
        this.application = application;
        this.context = application.getApplicationContext();
    }

    /// Get shared global instance of notification service
    public final static NotificationService shared(final Application application) {
        if (shared == null) {
            shared = new NotificationService(application);
        }
        return shared;
    }

    /// Start foreground service to enable background scan
    public void startForegroundService(Notification notification, int notificationId) {
        this.notification = notification;
        this.notificationId = notificationId;

        final Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /// Stop current foreground service
    public void stopForegroundService() {
        final Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_STOP);
        context.startService(intent);
    }

    public Notification getForegroundServiceNotification() {
        return this.notification;
    }

    public int getForegroundServiceNotificationId() {
        return this.notificationId;
    }
}
