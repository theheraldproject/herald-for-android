package io.heraldprox.herald.sensor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;
import io.heraldprox.herald.sensor.datatype.WGS84PointLocationReference;

public class ContactLogTests {

    @Test
    public void logFile() {
        final TextFile logFile = new TextFileBuffer();
        final ContactLog contactLog = new ContactLog(logFile);
        // Just header
        assertEquals("time,sensor,id,detect,read,measure,share,visit,detectHerald,delete,data\n", logFile.contentsOf());

        contactLog.sensor(SensorType.BLE, new TargetIdentifier("A"));
        logFile.flush();
        assertEquals(2, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",BLE,A,1,,,,,,,\n"));

        contactLog.sensor(SensorType.BLE, new PayloadData("aaaaaaaa"), new TargetIdentifier("B"));
        logFile.flush();
        assertEquals(3, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",BLE,B,,2,,,,,,aaaa\n"));

        contactLog.sensor(SensorType.BLE, Arrays.asList(new PayloadData("bbbbbbbb")), new TargetIdentifier("C"));
        logFile.flush();
        assertEquals(4, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",BLE,C,,,,4,,,,bbbb\n"));

        contactLog.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -10d), new TargetIdentifier("D"));
        logFile.flush();
        assertEquals(5, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",BLE,D,,,3,,,,,RSSI:-10.0\n"));

        contactLog.sensor(SensorType.BLE, new Location(new WGS84PointLocationReference(1d,2d, 3d), new Date(1), new Date(2)));
        logFile.flush();
        assertEquals(6, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",BLE,,,,,,5,,,\"WGS84(lat=1.0,lon=2.0,alt=3.0):[from=1970-01-01 00:00:01,to=1970-01-01 00:00:02]\"\n"));
    }
}
