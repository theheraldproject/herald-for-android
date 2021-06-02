//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class ProximityTests {

    @Test
    public void testInitNull() {
        assertNull(new Proximity(null, null, null).unit);
        assertNull(new Proximity(null, null, null).value);
        assertNull(new Proximity(null, null, null).calibration);
        assertNotNull(new Proximity(null, null, null).description());
        assertEquals(new Proximity(null, null, null).toString(), new Proximity(null, null, null).description());

        assertNotNull(new Proximity(null, null, new Calibration(null, null)).calibration);
        assertNull(new Proximity(null, null, new Calibration(null, null)).calibration.unit);
        assertNull(new Proximity(null, null, new Calibration(null, null)).calibration.value);
        assertNotNull(new Proximity(null, null, null).description());
        assertEquals(new Proximity(null, null, null).toString(), new Proximity(null, null, null).description());
    }

    @Test
    public void testInit() {
        assertEquals(ProximityMeasurementUnit.RSSI, new Proximity(ProximityMeasurementUnit.RSSI, null, null).unit);
        assertEquals(ProximityMeasurementUnit.RTT, new Proximity(ProximityMeasurementUnit.RTT, null, null).unit);
        assertEquals(new Double(3), new Proximity(ProximityMeasurementUnit.RSSI, 3d, null).value);
        assertEquals(new Double(3), new Proximity(ProximityMeasurementUnit.RSSI, 3d).value);
        assertNull(new Proximity(ProximityMeasurementUnit.RSSI, 3d).calibration);
        assertNotNull(new Proximity(ProximityMeasurementUnit.RSSI, 3d).toString());
        assertNotNull(new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)).toString());
    }

    @Test
    public void testEquals() {
        assertEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d), new Proximity(ProximityMeasurementUnit.RSSI, 3d));
        assertEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d).hashCode(), new Proximity(ProximityMeasurementUnit.RSSI, 3d).hashCode());
        assertNotEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d), new Proximity(ProximityMeasurementUnit.RSSI, 4d));
        assertNotEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d), new Proximity(ProximityMeasurementUnit.RTT, 3d));

        assertEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)), new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)));
        assertEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)).hashCode(), new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)).hashCode());
        assertNotEquals(new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 5d)), new Proximity(ProximityMeasurementUnit.RSSI, 3d, new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 6d)));
    }
}