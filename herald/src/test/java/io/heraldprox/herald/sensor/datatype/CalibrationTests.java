//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("ConstantConditions")
public class CalibrationTests {

    @Test
    public void testDescription() {
        assertNotNull(new Calibration(null, null).description());
        assertNotNull(new Calibration(null, 0d).description());
        assertNotNull(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, null).description());
        assertNotNull(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 0d).description());
    }

    @Test
    public void testToString() {
        assertEquals(new Calibration(null, null).toString(), new Calibration(null, null).description());
        assertEquals(new Calibration(null, 0d).toString(), new Calibration(null, 0d).description());
        assertEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, null).toString(), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, null).description());
        assertEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 0d).toString(), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 0d).description());
    }

    @Test
    public void testEquals() {
        assertEquals(new Calibration(null, null), new Calibration(null, null));
        assertNotEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, null), new Calibration(null, null));
        assertNotEquals(new Calibration(null, 1d), new Calibration(null, null));
        assertEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d));
        assertNotEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 2d));
    }

    @Test
    public void testHash() {
        assertEquals(new Calibration(null, null).hashCode(), new Calibration(null, null).hashCode());
        assertNotEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, null).hashCode(), new Calibration(null, null).hashCode());
        assertNotEquals(new Calibration(null, 1d).hashCode(), new Calibration(null, null).hashCode());
        assertEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d).hashCode(), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d).hashCode());
        assertNotEquals(new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 1d).hashCode(), new Calibration(CalibrationMeasurementUnit.BLETransmitPower, 2d).hashCode());
    }
}
