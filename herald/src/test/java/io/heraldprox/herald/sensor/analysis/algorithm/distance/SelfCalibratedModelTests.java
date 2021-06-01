//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithm.distance;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.SelfCalibratedModel;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Distance;
import io.heraldprox.herald.sensor.datatype.RSSI;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("ConstantConditions")
public class SelfCalibratedModelTests {

    @Test
    public void test_uncalibrated() {
        final SelfCalibratedModel<RSSI> model = new SelfCalibratedModel<>(
                new Distance(0.2), new Distance(1),
                TimeInterval.zero, TimeInterval.hours(12), null);
        model.map(new Sample<>(0, new RSSI(-10)));
        assertEquals(model.reduce(), 0.2, 0.1);

        model.reset();
        model.map(new Sample<>(0, new RSSI(-54)));
        assertEquals(model.reduce(), 1.0, 0.1);

        model.reset();
        model.map(new Sample<>(0, new RSSI(-99)));
        assertEquals(model.reduce(), 1.8, 0.1);
    }

    @Test
    public void test_calibrated_range() {
        for (int minRssi=-99; minRssi<-10; minRssi++) {
            for (int maxRssi=minRssi+4; maxRssi<-10; maxRssi++) {
                System.err.println("test_calibrated_range["+minRssi + "," + maxRssi+"]");
                test_calibrated_range(minRssi, maxRssi);
            }
        }
    }

    public void test_calibrated_range(final int minRssi, final int maxRssi) {
        final int midRssi = minRssi + (maxRssi - minRssi) / 2;
        final int quarterRssi = minRssi + (maxRssi - minRssi) * 3 / 4;
        final SelfCalibratedModel<RSSI> model = new SelfCalibratedModel<>(
                new Distance(0.2), new Distance(1),
                TimeInterval.zero, TimeInterval.hours(12), null);
        for (int rssi=minRssi; rssi<=maxRssi; rssi++) {
            model.histogram.add(rssi);
        }
        model.update();

        model.map(new Sample<>(0, new RSSI(maxRssi)));
        assertEquals(0.2, model.reduce(), 0.1);

        model.reset();
        model.map(new Sample<>(0, new RSSI(quarterRssi)));
        assertEquals(0.6, model.reduce(), 0.2);

        model.reset();
        model.map(new Sample<>(0, new RSSI(midRssi)));
        assertEquals(1.0, model.reduce(), 0.1);
    }
}
