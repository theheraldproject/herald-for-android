package org.c19x;

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

import org.c19x.sensor.datatype.Triple;
import org.c19x.sensor.datatype.Tuple;

public class AppDelegate extends Application {
    private static AppDelegate appDelegate;
    private static Context context;
    // Notifications
    private final int notificationChannelId = "C19XNotificationChannel".hashCode();
    private Triple<String, String, Notification> notificationContent = new Triple<>(null, null, null);

    @Override
    public void onCreate() {
        super.onCreate();
        appDelegate = this;
        context = getApplicationContext();
        createNotificationChannel();

        final Intent intent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onTerminate() {
        final Intent intent = new Intent(this, ForegroundService.class);
        stopService(intent);
        super.onTerminate();
    }

    public static AppDelegate getAppDelegate() {
        return appDelegate;
    }

    public static Context getContext() {
        return context;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel("C19XNotificationChannel", "C19X", importance);
            channel.setDescription("C19X notifications");
            final NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Tuple<Integer, Notification> notification() {
        return new Tuple<>(notificationChannelId, notificationContent.c);
    }

    public Tuple<Integer, Notification> notification(final String title, final String body) {
        if (title != null && body != null) {
            final String existingTitle = notificationContent.a;
            final String existingBody = notificationContent.b;
            if (!title.equals(existingTitle) || !body.equals(existingBody)) {
                createNotificationChannel();
                final Intent intent = new Intent(getApplicationContext(), AppDelegate.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "C19XNotificationChannel")
                        //.setSmallIcon(R.drawable.virus)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                final Notification notification = builder.build();
                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(notificationChannelId, notification);
                notificationContent = new Triple<>(title, body, notification);
                return new Tuple<>(notificationChannelId, notification);
            }
        } else {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.deleteNotificationChannel("C19XNotificationChannel");
            notificationContent = new Triple<>(null, null, null);
        }
        return new Tuple<>(notificationChannelId, null);
    }

}
