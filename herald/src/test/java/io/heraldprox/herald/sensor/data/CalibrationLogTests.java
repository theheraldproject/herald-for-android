package io.heraldprox.herald.sensor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.InertiaLocationReference;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class CalibrationLogTests {

    @Test
    public void logFile() throws Exception {
        final TextFile logFile = new TextFileBuffer();
        final CalibrationLog calibrationLog = new CalibrationLog(logFile);
        // Just header
        assertEquals("time,payload,rssi,x,y,z\n", logFile.contentsOf());
        // Measurement from accelerometer
        calibrationLog.sensor(SensorType.ACCELEROMETER, new Location(new InertiaLocationReference(1d,2d,3d), new Date(0), new Date(1)));
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",,,1.0,2.0,3.0\n"));
        // Measurement from BLE sensor
        calibrationLog.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -1d), new TargetIdentifier("A"), new PayloadData("aaaaaaaa"));
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",aaaa,-1.0,,,\n"));
    }

}
