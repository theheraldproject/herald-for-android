//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis;

import io.heraldprox.herald.sensor.datatype.Encounter;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.ProximityMeasurementUnit;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("ConstantConditions")
public class InteractionsTests {
    private final static SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testReduceByTarget() throws Exception {
        final List<Encounter> encounters = new ArrayList<>();
        assertEquals(Interactions.reduceByTarget(encounters).size(), 0);

        final PayloadData pd1 = new PayloadData((byte) 0, 1);
        final PayloadData pd2 = new PayloadData((byte) 1, 1);

        // Single encounter of pd1 at RSSI=1
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), pd1, f.parse("2020-09-24 00:00:00")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 1);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).duration.value, 1, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).proximity.mean(), 1, Double.MIN_VALUE);

        // Encounter pd1 again 4 seconds later
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), pd1, f.parse("2020-09-24 00:00:04")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 1);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).duration.value, 5, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).proximity.mean(), 1, Double.MIN_VALUE); // (1 + 4) / (1 + 4)

        // Encounter pd1 again 5 seconds later
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 3d), pd1, f.parse("2020-09-24 00:00:09")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 1);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).duration.value, 10, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).proximity.mean(), 2, Double.MIN_VALUE); // (1 + 4 + (5 * 3)) / (1 + 4 + 5)

        // Encounter pd1 again 31 seconds later, new encounter, so no change
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), pd1, f.parse("2020-09-24 00:00:40")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 1);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).duration.value, 10, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).proximity.mean(), 2, Double.MIN_VALUE); // (1 + 4 + (5 * 3)) / (1 + 4 + 5)

        // Encounter pd1 again 10 seconds later
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 4d), pd1, f.parse("2020-09-24 00:00:50")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 1);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).duration.value, 20, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd1).proximity.mean(), 3, Double.MIN_VALUE); // (1 + 4 + (5 * 3) + (10 * 4)) / (1 + 4 + 5 + 10)

        // Encounter pd2 for the first time
        encounters.add(new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 5d), pd2, f.parse("2020-09-24 00:01:00")));
        assertEquals(Interactions.reduceByTarget(encounters).size(), 2);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd2).duration.value, 1, Double.MIN_VALUE);
        assertEquals(Interactions.reduceByTarget(encounters).get(pd2).proximity.mean(), 5, Double.MIN_VALUE); // (1 + 4 + (5 * 3) + (10 * 4)) / (1 + 4 + 5 + 10)
    }

    @Test
    public void testReduceByProximity() throws Exception {
        assertEquals(Interactions.reduceByProximity(new ArrayList<Encounter>()).size(), 0);

        // One encounter at RSSI=1 with one device -> [1:1]
        //noinspection ArraysAsListWithZeroOrOneArgument
        final List<Encounter> encounters1 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00"))
        );
        assertEquals(Interactions.reduceByProximity(encounters1).size(), 1);
        assertEquals(Interactions.reduceByProximity(encounters1).get(1d).value, 1);

        // Two encounters at RSSI=1,2 with one device -> [1:1,2:1]
        final List<Encounter> encounters2 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:01"))
        );
        assertEquals(Interactions.reduceByProximity(encounters2).size(), 2);
        assertEquals(Interactions.reduceByProximity(encounters2).get(1d).value, 1);
        assertEquals(Interactions.reduceByProximity(encounters2).get(2d).value, 1);

        // Two encounters at RSSI=1,1 with one device -> [1:30]
        final List<Encounter> encounters3 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:30"))
        );
        assertEquals(Interactions.reduceByProximity(encounters3).size(), 1);
        assertEquals(Interactions.reduceByProximity(encounters3).get(1d).value, 31);

        // Two encounters at RSSI=1,2 with one device -> [1:1,2:30]
        final List<Encounter> encounters4 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:30"))
        );
        assertEquals(Interactions.reduceByProximity(encounters4).size(), 2);
        assertEquals(Interactions.reduceByProximity(encounters4).get(1d).value, 1);
        assertEquals(Interactions.reduceByProximity(encounters4).get(2d).value, 30);

        // Two encounters at RSSI=1,1 with two devices -> [1:2]
        final List<Encounter> encounters5 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 1, 1), f.parse("2020-09-24 00:00:30"))
        );
        assertEquals(Interactions.reduceByProximity(encounters5).size(), 1);
        assertEquals(Interactions.reduceByProximity(encounters5).get(1d).value, 2);

        // Two encounters at RSSI=1,2 with two devices -> [1:1,2:1]
        final List<Encounter> encounters6 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), new PayloadData((byte) 1, 1), f.parse("2020-09-24 00:00:00"))
        );
        assertEquals(Interactions.reduceByProximity(encounters6).size(), 2);
        assertEquals(Interactions.reduceByProximity(encounters6).get(1d).value, 1);
        assertEquals(Interactions.reduceByProximity(encounters6).get(2d).value, 1);
    }

    @Test
    public void testReduceByTime() throws Exception {
        assertEquals(Interactions.reduceByTime(new ArrayList<Encounter>()).size(), 0);

        // One encounter at RSSI=1 with one device -> [(2020-09-24T00:00:00+0000,[0:[1]])]
        //noinspection ArraysAsListWithZeroOrOneArgument
        final List<Encounter> encounters1 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00"))
        );
        assertEquals(Interactions.reduceByTime(encounters1).size(), 1);
        assertEquals(Interactions.reduceByTime(encounters1).get(0).context.values().iterator().next().size(), 1);
        assertEquals(Interactions.reduceByTime(encounters1).get(0).context.values().iterator().next().get(0).value, 1d, Double.MIN_VALUE);

        // Two encounters at RSSI=1,2 with one device -> [(2020-09-24T00:00:00+0000,[0:[1,2]])]
        final List<Encounter> encounters2 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:01"))
        );
        assertEquals(Interactions.reduceByTime(encounters2).size(), 1);
        assertEquals(Interactions.reduceByTime(encounters2).get(0).context.values().iterator().next().size(), 2);

        // Two encounters at RSSI=1,1 with one device -> [(2020-09-24T00:00:00+0000,[0:[1,1]])]
        final List<Encounter> encounters3 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:30"))
        );
        assertEquals(Interactions.reduceByTime(encounters3).size(), 1);
        assertEquals(Interactions.reduceByTime(encounters3).get(0).context.values().iterator().next().size(), 2);

        // Two encounters at RSSI=1,2 with one device -> [(2020-09-24T00:00:00+0000,[0:[1,2]])]
        final List<Encounter> encounters4 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 2d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:30"))
        );
        assertEquals(Interactions.reduceByTime(encounters4).size(), 1);
        assertEquals(Interactions.reduceByTime(encounters4).get(0).context.values().iterator().next().size(), 2);

        // Two encounters at RSSI=1,1 with two devices -> [(2020-09-24T00:00:00+0000,[0:[1],1:[1]])]
        final List<Encounter> encounters5 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 1, 1), f.parse("2020-09-24 00:00:00"))
        );
        assertEquals(Interactions.reduceByTime(encounters5).size(), 1);
        assertEquals(Interactions.reduceByTime(encounters5).get(0).context.size(), 2);
        assertEquals(Interactions.reduceByTime(encounters5).get(0).context.values().iterator().next().size(), 1);

        // Two encounters at RSSI=1,1 with one device -> [(2020-09-24T00:00:00+0000,[0:[1]]),(2020-09-24T00:01:00+0000,[0:[1]])]
        final List<Encounter> encounters6 = Arrays.asList(
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:00:00")),
                new Encounter(new Proximity(ProximityMeasurementUnit.RSSI, 1d), new PayloadData((byte) 0, 1), f.parse("2020-09-24 00:01:15"))
        );
        assertEquals(Interactions.reduceByTime(encounters6).size(), 2);
        assertEquals(Interactions.reduceByTime(encounters6).get(0).context.size(), 1);
        assertEquals(Interactions.reduceByTime(encounters6).get(1).context.size(), 1);
    }
}
