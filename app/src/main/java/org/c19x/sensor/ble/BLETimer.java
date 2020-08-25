package org.c19x.sensor.ble;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.Callback;
import org.c19x.sensor.datatype.Sample;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

///

/**
 * Steady one second timer for controlling BLE operations. Having a reliable timer for starting
 * and stopping scans is fundamental for reliable detection and tracking. Methods that have been
 * tested and failed included :
 * 1. Handler.postDelayed loop backed by MainLooper
 * - Actual delay time can drift to 10+ minutes for a 4 second request.
 * 2. Handler.postDelayed loop backed by dedicated looper backed by dedicated HandlerThread
 * - Slightly better than option (1) but actual delay time can still drift to several minutes for a 4 second request.
 * 3. Timer scheduled task loop
 * - Timer can drift
 * <p>
 * Test impact of power management by ...
 * <p>
 * 1. Run app on device
 * 2. Keep one device connected via USB only
 * 3. Put app in background mode and lock device
 * 4. Go to terminal
 * 5. cd  ~/Library/Android/sdk/platform-tools
 * <p>
 * Test DOZE mode
 * 1. ./adb shell dumpsys battery unplug
 * 2. Expect "powerSource=battery" on log
 * 3. ./adb shell dumpsys deviceidle force-idle
 * 4. Expect "idle=true" on log
 * <p>
 * Exit DOZE mode
 * 1. ./adb shell dumpsys deviceidle unforce
 * 2. ./adb shell dumpsys battery reset
 * 3. Expect "idle=false" and "powerSource=usb/ac" on log
 * <p>
 * Test APP STANDBY mode
 * 1. ./adb shell dumpsys battery unplug
 * 2. Expect "powerSource=battery"
 * 3. ./adb shell am set-inactive org.c19x.sensor true
 * <p>
 * Exit APP STANDBY mode
 * 1. ./adb shell am set-inactive org.c19x.sensor false
 * 2. ./adb shell am get-inactive org.c19x.sensor
 * 3. Expect "idle=false" on terminal
 * 4. ./adb shell dumpsys battery reset
 * 5. Expect "powerSource=usb/ac" on log
 */
public class BLETimer {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLETimer");
    private final Context context;
    private final Sample sample = new Sample();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Thread timerThread;
    private final PowerManager powerManager;
    private final PowerManager.WakeLock wakeLock;
    private final AtomicLong now = new AtomicLong(0);
    private Runnable runnable = null;

    public BLETimer(Context context) {
        this.context = context;
        powerManager = (PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sensor:BLETimer");
        wakeLock.acquire();
        timerThread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    now.set(System.currentTimeMillis());
                    final long elapsed = now.get() - last;
                    if (elapsed >= 1000) {
                        if (last != 0) {
                            runTimerTask(elapsed);
                        }
                        last = now.get();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (Throwable e) {
                    }
                }
            }
        });
        timerThread.setPriority(Thread.MAX_PRIORITY);
        timerThread.setName("Sensor.BLETimer");
        timerThread.start();
    }

    @Override
    protected void finalize() {
        wakeLock.release();
    }

    public void timerTask(final Callback<Long> timerTask) {
        runnable = new Runnable() {
            @Override
            public void run() {
                timerTask.accept(now.get());
            }
        };
    }

    private void runTimerTask(final long elapsed) {
        sample.add(elapsed);
//        final String idleMode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Boolean.toString(powerManager.isDeviceIdleMode()) : "N/A");
//        logger.debug("timer (elapsed={},count={},mean={},sd={},min={},max={},idle={},powerSaving={},powerSource={})",
//                elapsed, sample.count(), round(sample.mean()), round(sample.standardDeviation()),
//                round(sample.min()), round(sample.max()),
//                idleMode, powerManager.isPowerSaveMode(), powerSource());
        if (runnable == null) {
            return;
        }
        executorService.execute(runnable);
    }

    private static String round(Double value) {
        if (value == null) {
            return "-";
        } else {
            return Long.toString(Math.round(value));
        }
    }

    private String powerSource() {
        final Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "ac";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "usb";
            default:
                return "battery";
        }
    }
}
