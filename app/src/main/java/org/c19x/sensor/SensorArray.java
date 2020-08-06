package org.c19x.sensor;

import android.content.Context;

import org.c19x.sensor.ble.ConcreteBLESensor;
import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.ContactLog;
import org.c19x.sensor.data.SensorLogger;
import org.c19x.sensor.datatype.PayloadTimestamp;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private List<Sensor> sensorArray = new ArrayList<>();

    public SensorArray(Context context, PayloadDataSupplier payloadDataSupplier) {
        logger.debug("init");
//        sensorArray.append(ConcreteGPSSensor(rangeForBeacon:UUID(uuidString:BLESensorConfiguration.serviceUUID.uuidString)))
        sensorArray.add(new ConcreteBLESensor(context, payloadDataSupplier));

        // Loggers
        final String payloadString = payloadDataSupplier.payload(new PayloadTimestamp()).description();
        add(new ContactLog("contacts.csv"));
//        add(delegate: RScriptLog(filename: "rScriptLog.csv"))
//        add(delegate: DetectionLog(filename: "detection.csv", payloadString: payloadString, prefixLength: 6))
        logger.info("DEVICE ID (payloadPrefix={})", payloadString.substring(0, 6));
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
