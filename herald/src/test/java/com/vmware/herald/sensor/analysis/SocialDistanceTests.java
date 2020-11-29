//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis;

import com.vmware.herald.sensor.datatype.Encounter;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.ProximityMeasurementUnit;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SocialDistanceTests {
    private final static SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testScoreByProximity() throws Exception {
        final SocialDistance socialDistance = new SocialDistance();
        assertEquals(socialDistance.scoreByProximity(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 0, Double.MIN_VALUE);

        // Close enough to count
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 0d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")));
        assertEquals(socialDistance.scoreByProximity(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 1/60d, Double.MIN_VALUE);

        // Too far away to count
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -66d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByProximity(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 1/60d, Double.MIN_VALUE);

        // Close enough to count but 0% contribution
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -65d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByProximity(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 1/60d, Double.MIN_VALUE);

        // Close enough to count at 100% contribution
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 0d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByProximity(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 2/60d, Double.MIN_VALUE);
    }

    @Test
    public void testScoreByTarget() throws Exception {
        final SocialDistance socialDistance = new SocialDistance();
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), 0, Double.MIN_VALUE);

        // Close enough to count
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 0d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")));
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), (1/6d)/60d, Double.MIN_VALUE);

        // Too far away to count
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -66d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), (1/6d)/60d, Double.MIN_VALUE);

        // Close enough to count
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -65d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), (2/6d)/60d, Double.MIN_VALUE);

        // Close enough to count but same device
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -56d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), (2/6d)/60d, Double.MIN_VALUE);

        // Close enough to count and new device
        socialDistance.append(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, -56d), new PayloadData((byte) 1, 1), f.parse("2020-09-24 00:01:00")));
        assertEquals(socialDistance.scoreByTarget(f.parse("2020-09-24 00:00:00"), f.parse("2020-09-24 01:00:00")), (3/6d)/60d, Double.MIN_VALUE);
    }
}
