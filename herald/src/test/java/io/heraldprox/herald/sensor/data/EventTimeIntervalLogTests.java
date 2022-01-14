package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

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

public class EventTimeIntervalLogTests {

    @Test
    public void detect() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.detect);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Link B to "bbbb"
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        // Sample 1 - no output
        log.sensor(SensorType.BLE, new TargetIdentifier("B"));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.BLE, new TargetIdentifier("B"));
        // Sample 3 - output
        log.sensor(SensorType.BLE, new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\ndetect,aaaa,bbbb,2,"));
    }

    @Test
    public void measure() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.measure);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Link B to "bbbb"
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        // Sample 1 - no output
        log.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -10d), new TargetIdentifier("B"));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -10d), new TargetIdentifier("B"));
        // Sample 3 - output
        log.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -10d), new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\nmeasure,aaaa,bbbb,2,"));
    }

    @Test
    public void read() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.read);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Sample 1 - no output
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        // Sample 3 - output
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\nread,aaaa,bbbb,2,"));
    }

    @Test
    public void share() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.share);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Link B to "bbbb"
        log.sensor(SensorType.BLE, new PayloadData("bbbb"), new TargetIdentifier("B"));
        // Sample 1 - no output
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        // Sample 3 - output
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\nshare,aaaa,bbbb,2,"));
    }

    @Test
    public void sharedPeer() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.sharedPeer);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Sample 1 - no output
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        // Sample 3 - output
        log.sensor(SensorType.BLE, Arrays.asList(new PayloadData("cccc")), new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\nsharedPeer,aaaa,cccc,2,"));
    }

    @Test
    public void visit() {
        final TextFile logFile = new TextFileBuffer();
        final EventTimeIntervalLog log = new EventTimeIntervalLog(logFile, new PayloadData("aaaa"), EventTimeIntervalLog.EventType.visit);
        logFile.flush();
        assertEquals("event,central,peripheral,count,mean,sd,min,max\n", logFile.contentsOf());
        // Sample 1 - no output
        log.sensor(SensorType.GPS, new Location(new WGS84PointLocationReference(0d,0d,0d), new Date(0), new Date(1)));
        // Sample 2 - no output, no SD
        log.sensor(SensorType.GPS, new Location(new WGS84PointLocationReference(0d,0d,0d), new Date(0), new Date(1)));
        // Sample 3 - output
        log.sensor(SensorType.GPS, new Location(new WGS84PointLocationReference(0d,0d,0d), new Date(0), new Date(1)));
        assertTrue(logFile.contentsOf().startsWith("event,central,peripheral,count,mean,sd,min,max\nvisit,aaaa,aaaa,2,"));
    }
}
