package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import java.util.Arrays;

import io.heraldprox.herald.sensor.datatype.Date;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class StatisticsLogTests {

    @Test
    public void add() {
        final TextFile logFile = new TextFileBuffer();
        final StatisticsLog statisticsLog = new StatisticsLog(logFile, new PayloadData("zzzz"));
        logFile.flush();
        assertEquals("payload,count,mean,sd,min,max\n", logFile.contentsOf());

        // Sample 1 does not trigger write as lack of duration
        statisticsLog.add(new PayloadData("aaaa").shortName(), new Date(0));
        assertEquals("payload,count,mean,sd,min,max\n", logFile.contentsOf());
        // Sample 2 does not trigger write as lack of sd
        statisticsLog.add(new PayloadData("aaaa").shortName(), new Date(1));
        assertEquals("payload,count,mean,sd,min,max\n", logFile.contentsOf());
        // Sample 3 triggers write for distribution with 2 durations
        statisticsLog.add(new PayloadData("aaaa").shortName(), new Date(2));
        assertEquals("payload,count,mean,sd,min,max\naaaa,2,1.0,0.0,1.0,1.0\n", logFile.contentsOf());
    }

    @Test
    public void delegate() {
        final TextFile logFile = new TextFileBuffer();
        final StatisticsLog statisticsLog = new StatisticsLog(logFile, new PayloadData("zzzz"));
        logFile.flush();
        assertEquals("payload,count,mean,sd,min,max\n", logFile.contentsOf());
        // Payload read
        statisticsLog.sensor(SensorType.BLE, new PayloadData("aaaa"), new TargetIdentifier("A"));
        // Direct RSSI measurement
        statisticsLog.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -10d), new TargetIdentifier("A"));
        statisticsLog.sensor(SensorType.BLE, new Proximity(ProximityMeasurementUnit.RSSI, -20d), new TargetIdentifier("A"));
        // Payload sharing
        statisticsLog.sensor(SensorType.BLE, Arrays.asList(new PayloadData("aaaa")), new TargetIdentifier("B"));
        assertTrue(logFile.contentsOf().startsWith("payload,count,mean,sd,min,max\naaaa,3,"));
    }
}
