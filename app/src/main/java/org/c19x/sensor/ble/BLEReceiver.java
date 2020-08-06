package org.c19x.sensor.ble;

import org.c19x.sensor.Sensor;
import org.c19x.sensor.SensorDelegate;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Beacon receiver scans for peripherals with fixed service UUID.
 */
public interface BLEReceiver extends Sensor {
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
}
