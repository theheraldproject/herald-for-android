package org.c19x.sensor;

import org.c19x.sensor.data.ConcreteSensorLogger;
import org.c19x.sensor.data.SensorLogger;

import java.util.ArrayList;
import java.util.List;

/// Sensor array for combining multiple detection and tracking methods.
public class SensorArray implements Sensor {
    private SensorLogger logger = new ConcreteSensorLogger("Sensor", "SensorArray");
    private List<Sensor> sensorArray = new ArrayList<>();

    public SensorArray() {
        logger.debug("init");
//        sensorArray.append(ConcreteGPSSensor(rangeForBeacon:UUID(uuidString:BLESensorConfiguration.serviceUUID.uuidString)))
//        sensorArray.append(ConcreteBLESensor(payloadDataSupplier))
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
