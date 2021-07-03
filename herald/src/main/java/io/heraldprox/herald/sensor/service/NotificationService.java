//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.service;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Notification service for enabling foreground service (notification must be
 * displayed to show app is running in the background).
 */
public class NotificationService {
    @Nullable
    private static NotificationService shared = null;
    @NonNull
    private final Context context;
    private int notificationId;
    @Nullable
    private Notification notification;

    private NotificationService(@NonNull final Application application) {
        this.context = application.getApplicationContext();
    }

    /**
     * Get shared global instance of notification service
     * @param application
     * @return
     */
    @NonNull
    public static NotificationService shared(@NonNull final Application application) {
        if (null == shared) {
            shared = new NotificationService(application);
        }
        return shared;
    }

    /**
     * Start foreground service to enable background scan
     * @param notification
     * @param notificationId
     */
    public void startForegroundService(@NonNull final Notification notification, final int notificationId) {
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

    /**
     * Stop current foreground service
     */
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
