//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.TimeInterval;

/**
 * CSV battery log for post event analysis and visualisation
 */
public class BatteryLog extends SensorDelegateLogger {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BatteryLog");
    private final static TimeInterval updateInterval = TimeInterval.seconds(30);

    public BatteryLog(@NonNull final Context context, @NonNull final String filename) {
        super(context, filename);
        writeHeader();
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
                        Thread.sleep(updateInterval.millis());
                    } catch (Throwable e) {
                        logger.fault("Timer interrupted", e);
                    }
                }
            }
        }).start();
    }

    private void writeHeader() {
        if (empty()) {
            write("time,source,level");
        }
    }

    private void update() {
        if (null == context) {
            return;
        }
        final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent batteryStatus = context.registerReceiver(null, intentFilter);
        if (null == batteryStatus) {
            return;
        }
        final int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        final float batteryLevel = level * 100 / (float) scale;

        final String powerSource = (isCharging ? "external" : "battery");
        writeHeader();
        write(timestamp() + "," + powerSource + "," + batteryLevel);
        logger.debug("update (powerSource={},batteryLevel={})", powerSource, batteryLevel);
    }
}
