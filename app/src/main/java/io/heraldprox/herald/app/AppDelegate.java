//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.app;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import io.heraldprox.herald.sensor.Sensor;
import io.heraldprox.herald.sensor.SensorArray;
import io.heraldprox.herald.sensor.SensorDelegate;
import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;
import io.heraldprox.herald.sensor.data.BatteryLog;
import io.heraldprox.herald.sensor.data.ContactLog;
import io.heraldprox.herald.sensor.data.DetectionLog;
import io.heraldprox.herald.sensor.data.EventTimeIntervalLog;
import io.heraldprox.herald.sensor.data.SensorDelegateLogger;
import io.heraldprox.herald.sensor.data.StatisticsLog;
import io.heraldprox.herald.sensor.datatype.ImmediateSendData;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.payload.test.TestPayloadDataSupplier;
import io.heraldprox.herald.sensor.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

public class AppDelegate extends Application implements SensorDelegate {
    private final static String tag = AppDelegate.class.getName();
    private final static String NOTIFICATION_CHANNEL_ID = "HERALD_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private static AppDelegate appDelegate = null;

    // Test automation, set to null to disable automation.
    // Set to "http://serverAddress:port" to enable automation.
    private final static String automatedTestServer = null;
    @Nullable
    public AutomatedTestClient automatedTestClient = null;

    // Sensor for proximity detection
    private SensorArray sensor = null;

    // Generate unique and consistent device identifier for testing detection and tracking
    private int identifier() {
        final String text = Build.MODEL + ":" + Build.BRAND;
        return text.hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appDelegate = this;
        // Initialise foreground service to keep application running in background
        this.createNotificationChannel();
        NotificationService.shared(this).startForegroundService(this.getForegroundNotification(), NOTIFICATION_ID);
        // Initialise sensor array for given payload data supplier
        final PayloadDataSupplier payloadDataSupplier = new TestPayloadDataSupplier(identifier());
        sensor = new SensorArray(getApplicationContext(), payloadDataSupplier);
        // Add appDelegate as listener for detection events for logging and start sensor
        sensor.add(this);
        // Sensor will start and stop with UI switch (default ON) and bluetooth state
        // Or remotely controlled by test server.
        if (null != automatedTestServer) {
            automatedTestClient = new AutomatedTestClient(automatedTestServer, this, sensor, TimeInterval.minute);
            sensor.add(automatedTestClient);
        }
        // Efficacy Loggers
        if (BuildConfig.DEBUG) {
            final PayloadData payloadData = sensor.payloadData();
            final List<SensorDelegateLogger> sensorDelegateLoggers = new ArrayList<>();
            sensorDelegateLoggers.add(new ContactLog(this, "contacts.csv"));
            sensorDelegateLoggers.add(new StatisticsLog(this, "statistics.csv",payloadData));
            sensorDelegateLoggers.add(new DetectionLog(this,"detection.csv", payloadData));
            sensorDelegateLoggers.add(new BatteryLog(this, "battery.csv"));
            if (BLESensorConfiguration.payloadDataUpdateTimeInterval != TimeInterval.never ||
                (BLESensorConfiguration.interopOpenTraceEnabled && BLESensorConfiguration.interopOpenTracePayloadDataUpdateTimeInterval != TimeInterval.never)) {
                sensorDelegateLoggers.add(new EventTimeIntervalLog(this, "statistics_didRead.csv", payloadData, EventTimeIntervalLog.EventType.read));
            }
            for (final SensorDelegateLogger sensorDelegateLogger : sensorDelegateLoggers) {
                sensor.add(sensorDelegateLogger);
                if (null != automatedTestClient) {
                    automatedTestClient.add(sensorDelegateLogger);
                }
            }
        }
    }

    @Override
    public void onTerminate() {
        sensor.stop();
        super.onTerminate();
    }

    /**
     * Get app delegate after onCreate() has been called.
     * @return App delegate, or null if onCreate() hasn't been called yet.
     */
    public static AppDelegate getAppDelegate() {
        return appDelegate;
    }

    /**
     * Get sensor array after onCreate() has been called.
     * @return Sensor array, or null if onCreate() hasn't been called yet.
     */
    public Sensor sensor() {
        return sensor;
    }

    // MARK:- SensorDelegate for logging proximity detection events

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull TargetIdentifier didDetect) {
        Log.i(tag, sensor.name() + ",didDetect=" + didDetect);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull PayloadData didRead, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didRead=" + didRead.shortName() + ",fromTarget=" + fromTarget);
        parsePayload("didRead", sensor, didRead, fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull ImmediateSendData didReceive, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didReceive=" + didReceive.data.base64EncodedString() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull List<PayloadData> didShare, @NonNull TargetIdentifier fromTarget) {
        final List<String> payloads = new ArrayList<>(didShare.size());
        for (PayloadData payloadData : didShare) {
            payloads.add(payloadData.shortName());
        }
        Log.i(tag, sensor.name() + ",didShare=" + payloads.toString() + ",fromTarget=" + fromTarget);
        for (PayloadData payloadData : didShare) {
            parsePayload("didShare", sensor, payloadData, fromTarget);
        }
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget) {
        Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget);
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Location didVisit) {
        Log.i(tag, sensor.name() + ",didVisit=" + ((null == didVisit) ? "" : didVisit.description()));
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull Proximity didMeasure, @NonNull TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        Log.i(tag, sensor.name() + ",didMeasure=" + didMeasure.description() + ",fromTarget=" + fromTarget + ",withPayload=" + withPayload.shortName());
    }

    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        Log.i(tag, sensor.name() + ",didUpdateState=" + didUpdateState.name());
    }

    private void parsePayload(String source, SensorType sensor, PayloadData payloadData, TargetIdentifier fromTarget) {
        String service = "herald";
        String parsedPayload = payloadData.shortName();
        if (payloadData instanceof LegacyPayloadData) {
            final LegacyPayloadData legacyPayloadData = (LegacyPayloadData) payloadData;
            if (legacyPayloadData.service == null) {
                service = "null";
                parsedPayload = payloadData.hexEncodedString();
            } else if (legacyPayloadData.service == BLESensorConfiguration.interopOpenTraceServiceUUID) {
                service = "opentrace";
                parsedPayload = new String(legacyPayloadData.value);
            } else if (legacyPayloadData.service == BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID) {
                service = "advert";
                parsedPayload = payloadData.hexEncodedString();
            } else {
                service = "unknown|" + legacyPayloadData.service.toString();
                parsedPayload = payloadData.hexEncodedString();
            }
        }
        Log.i(tag, sensor.name() + ",didParse=" + service + ",fromTarget=" + fromTarget + ",payload=" + payloadData.shortName() + ",parsedPayload=" + parsedPayload);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    this.getString(R.string.notification_channel_name), importance);

            channel.setDescription(this.getString(R.string.notification_channel_description));

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getForegroundNotification() {
        final Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(io.heraldprox.herald.R.drawable.ic_notification)
                .setContentTitle(this.getString(R.string.notification_content_title))
                .setContentText(this.getString(R.string.notification_content_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        final Notification notification = builder.build();
        return notification;
    }
}
