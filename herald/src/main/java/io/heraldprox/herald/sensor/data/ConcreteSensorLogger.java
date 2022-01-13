//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;
import android.util.Log;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ConcreteSensorLogger implements SensorLogger, Resettable {
    private final String subsystem, category;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    /** Will not leak. When Logger leaves memory, context reference will be removed. */
    @Nullable
    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    @Nullable
    private static TextFile logFile = null;

    public ConcreteSensorLogger(@NonNull final String subsystem, @NonNull final String category) {
        this.subsystem = subsystem;
        this.category = category;
    }

    @Override
    public synchronized void reset() {
        logFile.reset();
    }

    public static void context(@Nullable final Context context) {
        if (context != null && context != ConcreteSensorLogger.context) {
            ConcreteSensorLogger.context = context;
            logFile(new TextFile(context, "log.txt"));
        }
    }

    public static void logFile(@NonNull final TextFile logFile) {
        ConcreteSensorLogger.logFile = logFile;
    }

    private boolean suppress(@NonNull final SensorLoggerLevel level) {
        if (BLESensorConfiguration.logLevel == SensorLoggerLevel.off) {
            return true;
        }
        switch (level) {
            case debug:
                return (BLESensorConfiguration.logLevel == SensorLoggerLevel.info || BLESensorConfiguration.logLevel == SensorLoggerLevel.fault);
            case info:
                return (BLESensorConfiguration.logLevel == SensorLoggerLevel.fault);
            default:
                return false;
        }
    }

    private void log(@NonNull final SensorLoggerLevel level, @NonNull final String message, final Object... values) {
        if (!suppress(level)) {
            // android.util.Log is unavailable during test, this will throw error.
            try {
                outputLog(level, tag(subsystem, category), message, values);
            } catch (Throwable ignored) {
            }
            outputStream(level, subsystem, category, message, values);
        }
    }

    public void debug(@NonNull final String message, final Object... values) {
        log(SensorLoggerLevel.debug, message, values);
    }

    public void info(@NonNull final String message, final Object... values) {
        log(SensorLoggerLevel.info, message, values);
    }

    public void fault(@NonNull final String message, final Object... values) {
        log(SensorLoggerLevel.fault, message, values);
    }

    @NonNull
    private static String tag(@NonNull final String subsystem, @NonNull final String category) {
        return subsystem + "::" + category;
    }

    private static void outputLog(@NonNull final SensorLoggerLevel level, @NonNull final String tag, @NonNull final String message, final Object... values) {
        final Throwable throwable = getThrowable(values);
        switch (level) {
            case debug: {
                if (null == throwable) {
                    Log.d(tag, render(message, values));
                } else {
                    Log.d(tag, render(message, values), throwable);
                }
                break;
            }
            case info: {
                if (null == throwable) {
                    Log.i(tag, render(message, values));
                } else {
                    Log.i(tag, render(message, values), throwable);
                }
                break;
            }
            case fault: {
                if (null == throwable) {
                    Log.w(tag, render(message, values));
                } else {
                    Log.w(tag, render(message, values), throwable);
                }
                break;
            }
        }
    }

    private static void outputStream(@NonNull final SensorLoggerLevel level, @NonNull final String subsystem, @NonNull final String category, @NonNull final String message, final Object... values) {
        if (null == logFile) {
            return;
        }
        final String timestamp = dateFormatter.format(new Date());
        final String csvMessage = render(message, values).replace('\"', '\'');
        final String quotedMessage = (message.contains(",") ? "\"" + csvMessage + "\"" : csvMessage);
        final String entry = timestamp + "," + level + "," + subsystem + "," + category + "," + quotedMessage;
        logFile.write(entry);
    }


    @Nullable
    private static Throwable getThrowable(@NonNull final Object... values) {
        if (values.length > 0 && values[values.length - 1] instanceof Throwable) {
            return (Throwable) values[values.length - 1];
        } else {
            return null;
        }
    }

    @NonNull
    private static String render(@NonNull final String message, @NonNull final Object... values) {
        if (0 == values.length) {
            return message;
        } else {
            final StringBuilder stringBuilder = new StringBuilder();

            int valueIndex = 0;
            int start = 0;
            int end = message.indexOf("{}");
            while (end > 0) {
                stringBuilder.append(message.substring(start, end));
                if (values.length > valueIndex) {
                    if (null == values[valueIndex]) {
                        stringBuilder.append("NULL");
                    } else {
                        stringBuilder.append(values[valueIndex].toString());
                    }
                }
                valueIndex++;
                start = end + 2;
                end = message.indexOf("{}", start);
            }
            stringBuilder.append(message.substring(start));

            return stringBuilder.toString();
        }
    }

}
