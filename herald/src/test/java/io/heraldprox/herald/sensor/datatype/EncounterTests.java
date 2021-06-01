//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Date;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class EncounterTests {

    @Test
    public void testEncodeDecodeNull() {
        final Encounter expected = new Encounter(null, null, null);
        final Encounter actual = new Encounter(expected.csvString());
        assertEquals(expected.timestamp, actual.timestamp);
        assertEquals(expected.proximity, actual.proximity);
        assertEquals(0, actual.payload.value.length);
        assertFalse(expected.isValid());
        assertFalse(actual.isValid());
    }

    @Test
    public void testEncodeDecodeWithoutTimestamp() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, random.nextDouble());
            final byte[] data = new byte[i];
            random.nextBytes(data);
            final PayloadData payloadData = new PayloadData(data);
            final Encounter expected = new Encounter(proximity, payloadData);
            final Encounter actual = new Encounter(expected.csvString());
            assertEquals(expected.timestamp.getTime() / 1000, actual.timestamp.getTime() / 1000);
            assertEquals(expected.proximity, actual.proximity);
            assertEquals(expected.payload, actual.payload);
            assertTrue(expected.isValid());
            assertTrue(actual.isValid());
        }
    }

    @Test
    public void testEncodeDecodeWithoutCalibration() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, random.nextDouble());
            final byte[] data = new byte[i];
            random.nextBytes(data);
            final PayloadData payloadData = new PayloadData(data);
            final Date timestamp = new Date((random.nextLong() % 1000) * 1000);
            final Encounter expected = new Encounter(proximity, payloadData, timestamp);
            final Encounter actual = new Encounter(expected.csvString());
            assertEquals(expected.timestamp.getTime(), actual.timestamp.getTime());
            assertEquals(expected.proximity, actual.proximity);
            assertEquals(expected.payload, actual.payload);
            assertTrue(expected.isValid());
            assertTrue(actual.isValid());
        }
    }

    @Test
    public void testEncodeDecodeWithCalibration() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final Calibration calibration = new Calibration(CalibrationMeasurementUnit.BLETransmitPower, random.nextDouble());
            final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, random.nextDouble(), calibration);
            final byte[] data = new byte[i];
            random.nextBytes(data);
            final PayloadData payloadData = new PayloadData(data);
            final Date timestamp = new Date(Math.abs(random.nextLong() % 1000) * 1000);
            final Encounter expected = new Encounter(proximity, payloadData, timestamp);
            final Encounter actual = new Encounter(expected.csvString());
            assertEquals(expected.timestamp.getTime(), actual.timestamp.getTime());
            assertEquals(expected.proximity, actual.proximity);
            assertEquals(expected.payload, actual.payload);
            assertTrue(expected.isValid());
            assertTrue(actual.isValid());
        }
    }

    @Test
    public void testInvalidEmptyInput() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<10; i++) {
            final Encounter encounter = new Encounter(stringBuilder.toString());
            assertFalse(encounter.isValid());
            stringBuilder.append(',');
        }
    }

    @Test
    public void testInvalidInput() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("XXXX");
        for (int i=0; i<10; i++) {
            final Encounter encounter = new Encounter(stringBuilder.toString());
            assertFalse(encounter.isValid());
            stringBuilder.append(",XXXX");
        }
    }
}
