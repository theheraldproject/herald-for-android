package org.c19x.sensor.data;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConcreteSensorLogger implements SensorLogger {
    private final String subsystem, category;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final TextFile logFile = new TextFile("log.txt");

    public ConcreteSensorLogger(String subsystem, String category) {
        this.subsystem = subsystem;
        this.category = category;
    }

    public void log(SensorLoggerLevel level, String message, final Object... values) {
        outputLog(level, tag(subsystem, category), message, values);
    }

    public void debug(String message, final Object... values) {
        log(SensorLoggerLevel.debug, message, values);
    }

    public void info(String message, final Object... values) {
        log(SensorLoggerLevel.info, message, values);
    }

    public void fault(String message, final Object... values) {
        log(SensorLoggerLevel.fault, message, values);
    }

    private final static String tag(String subsystem, String category) {
        return subsystem + "::" + category;
    }

    private final static void outputLog(final SensorLoggerLevel level, final String tag, final String message, final Object... values) {
        final Throwable throwable = getThrowable(values);
        switch (level) {
            case debug: {
                if (throwable == null) {
                    Log.d(tag, render(message, values));
                } else {
                    Log.d(tag, render(message, values), throwable);
                }
                break;
            }
            case info: {
                if (throwable == null) {
                    Log.i(tag, render(message, values));
                } else {
                    Log.i(tag, render(message, values), throwable);
                }
                break;
            }
            case fault: {
                if (throwable == null) {
                    Log.w(tag, render(message, values));
                } else {
                    Log.w(tag, render(message, values), throwable);
                }
                break;
            }
        }
    }

    private final static void outputStream(final SensorLoggerLevel level, final String subsystem, final String category, final String message, final Object... values) {
        final Throwable throwable = getThrowable(values);
        final String timestamp = dateFormatter.format(new Date());
        final String csvMessage = render(message, values).replace('\"', '\'');
        final String quotedMessage = (message.contains(",") ? "\"" + csvMessage + "\"" : csvMessage);
        final String entry = timestamp + "," + level + "," + subsystem + "," + category + "," + quotedMessage;
        logFile.write(entry);
    }


    private final static Throwable getThrowable(final Object... values) {
        if (values.length > 0 && values[values.length - 1] instanceof Throwable) {
            return (Throwable) values[values.length - 1];
        } else {
            return null;
        }
    }

    private final static String render(final String message, final Object... values) {
        if (values.length == 0) {
            return message;
        } else {
            final StringBuilder stringBuilder = new StringBuilder();

            int valueIndex = 0;
            int start = 0;
            int end = message.indexOf("{}");
            while (end > 0) {
                stringBuilder.append(message.substring(start, end));
                if (values.length > valueIndex) {
                    if (values[valueIndex] == null) {
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
