package org.c19x.sensor.data;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/// CSV battery log for post event analysis and visualisation
public class BatteryLog {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BatteryLog");
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Context context;
    private final TextFile textFile;

    public BatteryLog(Context context, String filename, final long frequency) {
        this.context = context;
        textFile = new TextFile(filename);
        if (textFile.empty()) {
            textFile.write("time,source,level");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        update();
                    } catch (Throwable e) {
                        logger.fault("Update failed", e);
                    }
                    try {
                        Thread.sleep(frequency);
                    } catch (Throwable e) {
                        logger.fault("Timer interrupted", e);
                    }
                }
            }
        }).start();
    }

    private void update() {
        final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent batteryStatus = context.registerReceiver(null, intentFilter);
        final int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        final float batteryLevel = level * 100 / (float) scale;

        final String powerSource = (isCharging ? "external" : "battery");
        final String timestamp = dateFormatter.format(new Date());
        textFile.write(timestamp + "," + powerSource + "," + Float.toString(batteryLevel));
        logger.debug("update (powerSource={},batteryLevel={})", powerSource, batteryLevel);
    }
}
