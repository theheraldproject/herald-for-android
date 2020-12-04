//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    }
}