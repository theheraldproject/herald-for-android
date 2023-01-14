package io.heraldprox.herald.sensor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatteryLogTests {

    @Test
    public void logFile() throws Exception {
        final TextFile logFile = new TextFileBuffer();
        final BatteryLog batteryLog = new BatteryLog(logFile);
        // Just header
        assertEquals("time,source,level\n", logFile.contentsOf());
        // Measurement 1
        batteryLog.append("battery", 50);
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",battery,50.0\n"));
        // Measurement 2
        batteryLog.append("power", 60);
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",power,60.0\n"));
    }
}
