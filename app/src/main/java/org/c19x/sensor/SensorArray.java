package org.c19x.sensor;

import android.content.Context;

import org.c19x.sensor.ble.ConcreteBLESensor;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.ContactLog;
import org.c19x.sensor.data.DetectionLog;
import org.c19x.sensor.data.RScriptLog;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.PayloadTimestamp;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private List<Sensor> sensorArray = new ArrayList<>();

    private final static int payloadPrefixLength = 6;
    private final String payloadString, payloadPrefix;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";


    public SensorArray(Context context, PayloadDataSupplier payloadDataSupplier) {
        logger.debug("init");
        sensorArray.add(new ConcreteBLESensor(context, payloadDataSupplier));

        // Loggers
        payloadString = payloadDataSupplier.payload(new PayloadTimestamp()).description();
        payloadPrefix = payloadString.substring(0, payloadPrefixLength);
        add(new ContactLog("contacts.csv"));
        add(new RScriptLog("rScriptLog.csv"));
        add(new DetectionLog("detection.csv", payloadString, payloadPrefixLength));

        logger.info("DEVICE (payloadPrefix={},description={})", payloadPrefix, deviceDescription);
    }

    public final String payloadPrefix() {
        return payloadPrefix;
    }

    @Override
    public void add(final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        logger.debug("start");
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        logger.debug("stop");
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }
}
