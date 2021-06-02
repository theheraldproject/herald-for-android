//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithm.distance;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.RssiHistogram;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RssiHistogramTests {

    private void printRange(final RssiHistogram rssiHistogram, final int min, final int max) {
        for (int i=min; i<=max; i++) {
            System.out.println(i+","+ rssiHistogram.normalise(i));
        }
    }

    @Test
    public void test_empty() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-32, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-54, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-55, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-77, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), Double.MIN_VALUE);
        assertEquals(-32, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), Double.MIN_VALUE);
        assertEquals(-54, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), Double.MIN_VALUE);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), Double.MIN_VALUE);
        assertEquals(-77, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), Double.MIN_VALUE);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-49, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_zero_variance_10() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        rssiHistogram.add(-10);
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), Double.MIN_VALUE);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_zero_variance_10_x2() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        rssiHistogram.add(-10);
        rssiHistogram.add(-10);
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), Double.MIN_VALUE);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_zero_variance_50() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        rssiHistogram.add(-50);
        rssiHistogram.update();

        // Percentile values
        assertEquals(-50, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), Double.MIN_VALUE);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_zero_variance_50_x2() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        rssiHistogram.add(-50);
        rssiHistogram.update();

        // Percentile values
        assertEquals(-50, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), Double.MIN_VALUE);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_identity() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-98; rssi<=-10; rssi++) {
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-32, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-54, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-55, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-76, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-32, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-54, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-49, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_identity_x2() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-98; rssi<=-10; rssi++) {
            rssiHistogram.add(rssi);
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-32, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-54, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-55, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-76, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-32, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-54, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-100), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-50, rssiHistogram.normalise(-50), Double.MIN_VALUE);
        assertEquals(-49, rssiHistogram.normalise(-49), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_upper_range() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-44; rssi<=-10; rssi++) {
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-18, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-27, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-27, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-36, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-44, rssiHistogram.samplePercentile(0.01), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-30, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-53, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-53, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-46), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-45), Double.MIN_VALUE);
        assertEquals(-96, rssiHistogram.normalise(-44), 0.5);
        assertEquals(-53, rssiHistogram.normalise(-27), 0.5);
        assertEquals(-13, rssiHistogram.normalise(-11), 0.5);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_upper_range_x2() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-44; rssi<=-10; rssi++) {
            rssiHistogram.add(rssi);
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-10, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-18, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-27, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-27, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-36, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-44, rssiHistogram.samplePercentile(0.01), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-30, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-53, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-53, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-46), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.normalise(-45), Double.MIN_VALUE);
        assertEquals(-96, rssiHistogram.normalise(-44), 0.5);
        assertEquals(-53, rssiHistogram.normalise(-27), 0.5);
        assertEquals(-13, rssiHistogram.normalise(-11), 0.5);
        assertEquals(-10, rssiHistogram.normalise(-10), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_lower_range() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-98; rssi<=-45; rssi++) {
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-45, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-58, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-72, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-72, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-85, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-98, rssiHistogram.samplePercentile(0.01), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-31, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-97, rssiHistogram.normalise(-98), 0.5);
        assertEquals(-76, rssiHistogram.normalise(-85), 0.5);
        assertEquals(-55, rssiHistogram.normalise(-72), 0.5);
        assertEquals(-31, rssiHistogram.normalise(-58), 0.5);
        assertEquals(-10, rssiHistogram.normalise(-45), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }

    @Test
    public void test_lower_range_x2() {
        final RssiHistogram rssiHistogram = new RssiHistogram(-99, -10, TimeInterval.zero, null);
        for (int rssi=-98; rssi<=-45; rssi++) {
            rssiHistogram.add(rssi);
            rssiHistogram.add(rssi);
        }
        rssiHistogram.update();

        // Percentile values
        assertEquals(-45, rssiHistogram.samplePercentile(1.00), Double.MIN_VALUE);
        assertEquals(-58, rssiHistogram.samplePercentile(0.75), Double.MIN_VALUE);
        assertEquals(-72, rssiHistogram.samplePercentile(0.50), Double.MIN_VALUE);
        assertEquals(-72, rssiHistogram.samplePercentile(0.49), Double.MIN_VALUE);
        assertEquals(-85, rssiHistogram.samplePercentile(0.25), Double.MIN_VALUE);
        assertEquals(-98, rssiHistogram.samplePercentile(0.01), Double.MIN_VALUE);
        assertEquals(-99, rssiHistogram.samplePercentile(0.00), Double.MIN_VALUE);

        // Normalisation extends value to full range
        assertEquals(-10, rssiHistogram.normalise(rssiHistogram.samplePercentile(1.00)), 0.5);
        assertEquals(-31, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.75)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.50)), 0.5);
        assertEquals(-55, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.49)), 0.5);
        assertEquals(-76, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.25)), 0.5);
        assertEquals(-99, rssiHistogram.normalise(rssiHistogram.samplePercentile(0.00)), 0.5);

        // Out of range values clamped to min and max
        assertEquals(-99, rssiHistogram.normalise(-99), Double.MIN_VALUE);
        assertEquals(-97, rssiHistogram.normalise(-98), 0.5);
        assertEquals(-76, rssiHistogram.normalise(-85), 0.5);
        assertEquals(-55, rssiHistogram.normalise(-72), 0.5);
        assertEquals(-31, rssiHistogram.normalise(-58), 0.5);
        assertEquals(-10, rssiHistogram.normalise(-45), Double.MIN_VALUE);
        assertEquals(-10, rssiHistogram.normalise(-9), Double.MIN_VALUE);
    }
}
