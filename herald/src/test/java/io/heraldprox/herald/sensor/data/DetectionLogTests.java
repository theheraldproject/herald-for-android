package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import java.util.Arrays;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

public class DetectionLogTests {

    @Test
    public void logFile() {
        final TextFile logFile = new TextFileBuffer();
        final DetectionLog detectionLog = new DetectionLog(logFile, new PayloadData("aaaaaaaa"));
        assertEquals("NULL,Android,0,aaaa\n", logFile.contentsOf());

        detectionLog.sensor(SensorType.BLE, new PayloadData("bbbbbbbb"), new TargetIdentifier("B"));
        assertEquals("NULL,Android,0,aaaa,bbbb\n", logFile.contentsOf());

        detectionLog.sensor(SensorType.BLE, new PayloadData("dddddddd"), new TargetIdentifier("D"));
        assertEquals("NULL,Android,0,aaaa,bbbb,dddd\n", logFile.contentsOf());
        // Detected payloads should be in fixed order
        detectionLog.sensor(SensorType.BLE, new PayloadData("cccccccc"), new TargetIdentifier("C"));
        assertEquals("NULL,Android,0,aaaa,bbbb,cccc,dddd\n", logFile.contentsOf());
        // Shared payloads
        detectionLog.sensor(SensorType.BLE, Arrays.asList(new PayloadData("eeee"), new PayloadData("ffff")), new TargetIdentifier("G"));
        assertEquals("NULL,Android,0,aaaa,bbbb,cccc,dddd,eeee,ffff\n", logFile.contentsOf());
        // Update state triggers rewrite
        detectionLog.sensor(SensorType.BLE, SensorState.on);
        assertEquals("NULL,Android,0,aaaa,bbbb,cccc,dddd,eeee,ffff\n", logFile.contentsOf());
    }
}
