package org.c19x.sensor;

import android.content.Context;

import org.c19x.sensor.ble.ConcreteBLESensor;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.ContactLog;
import org.c19x.sensor.data.DetectionLog;
import org.c19x.sensor.data.RScriptLog;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.data.StatisticsLog;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadTimestamp;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";


    public SensorArray(Context context, PayloadDataSupplier payloadDataSupplier) {
        logger.debug("init");
        sensorArray.add(new ConcreteBLESensor(context, payloadDataSupplier));

        // Loggers
        payloadData = payloadDataSupplier.payload(new PayloadTimestamp());
        add(new ContactLog("contacts.csv"));
        add(new StatisticsLog("statistics.csv", payloadData));
        add(new RScriptLog("rScriptLog.csv", payloadData));
        add(new DetectionLog("detection.csv", payloadData));

        logger.info("DEVICE (payload={},description={})", payloadData.shortName(), deviceDescription);
    }

    public final PayloadData payloadData() {
        return payloadData;
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
