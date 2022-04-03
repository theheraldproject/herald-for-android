package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;

public class ConcreteSensorLoggerTests {

    @Test
    public void logLevelDebug() {
        BLESensorConfiguration.logLevel = SensorLoggerLevel.debug;
        final TextFile logFile = new TextFileBuffer();
        ConcreteSensorLogger.logFile(logFile);
        final SensorLogger logger = new ConcreteSensorLogger("ConcreteSensorLoggerTests", "logLevelDebug");
        assertEquals(0, logFile.lines().size());
        logger.debug("debug");
        logFile.flush();
        assertEquals(1, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",debug,ConcreteSensorLoggerTests,logLevelDebug,debug\n"));
        logger.info("info");
        logFile.flush();
        assertEquals(2, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",info,ConcreteSensorLoggerTests,logLevelDebug,info\n"));
        logger.fault("fault");
        logFile.flush();
        assertEquals(3, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",fault,ConcreteSensorLoggerTests,logLevelDebug,fault\n"));
    }

    @Test
    public void logLevelInfo() {
        BLESensorConfiguration.logLevel = SensorLoggerLevel.info;
        final TextFile logFile = new TextFileBuffer();
        ConcreteSensorLogger.logFile(logFile);
        final SensorLogger logger = new ConcreteSensorLogger("ConcreteSensorLoggerTests", "logLevelInfo");
        assertEquals(0, logFile.lines().size());
        logger.debug("debug");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
        logger.info("info");
        logFile.flush();
        assertEquals(1, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",info,ConcreteSensorLoggerTests,logLevelInfo,info\n"));
        logger.fault("fault");
        logFile.flush();
        assertEquals(2, logFile.lines().size());
        assertTrue(logFile.contentsOf().endsWith(",fault,ConcreteSensorLoggerTests,logLevelInfo,fault\n"));
    }

    @Test
    public void logLevelFault() {
        BLESensorConfiguration.logLevel = SensorLoggerLevel.fault;
        final TextFile logFile = new TextFileBuffer();
        ConcreteSensorLogger.logFile(logFile);
        final SensorLogger logger = new ConcreteSensorLogger("ConcreteSensorLoggerTests", "logLevelFault");
        assertEquals(0, logFile.lines().size());
        logger.debug("debug");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
        logger.info("info");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
        logger.fault("fault");
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",fault,ConcreteSensorLoggerTests,logLevelFault,fault\n"));
    }

    @Test
    public void logLevelOff() {
        BLESensorConfiguration.logLevel = SensorLoggerLevel.off;
        final TextFile logFile = new TextFileBuffer();
        ConcreteSensorLogger.logFile(logFile);
        final SensorLogger logger = new ConcreteSensorLogger("ConcreteSensorLoggerTests", "logLevelOff");
        assertEquals(0, logFile.lines().size());
        logger.reset();
        assertEquals(0, logFile.lines().size());
        logger.debug("debug");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
        logger.info("info");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
        logger.fault("fault");
        logFile.flush();
        assertEquals(0, logFile.lines().size());
    }

    @Test
    public void render() {
        final TextFile logFile = new TextFileBuffer();
        BLESensorConfiguration.logLevel = SensorLoggerLevel.debug;
        ConcreteSensorLogger.logFile(logFile);
        final SensorLogger logger = new ConcreteSensorLogger("ConcreteSensorLoggerTests", "render");
        logger.reset();
        assertEquals("", logFile.contentsOf());
        logger.debug("msg0");
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",debug,ConcreteSensorLoggerTests,render,msg0\n"));
        logger.debug("msg1(p1={})",1);
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",debug,ConcreteSensorLoggerTests,render,msg1(p1=1)\n"));
        logger.debug("msg2(p1={},p2={})",1, "2");
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",debug,ConcreteSensorLoggerTests,render,\"msg2(p1=1,p2=2)\"\n"));
        logger.debug("msg3(p1={},p2={},p3={})",1, "2", null);
        logFile.flush();
        assertTrue(logFile.contentsOf().endsWith(",debug,ConcreteSensorLoggerTests,render,\"msg3(p1=1,p2=2,p3=NULL)\"\n"));
    }
}
