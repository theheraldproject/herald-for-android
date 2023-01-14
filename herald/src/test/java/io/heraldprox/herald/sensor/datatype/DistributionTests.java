//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DistributionTests {

    @Test
    public void testEmpty() {
        final Distribution distribution = new Distribution();
        assertEquals(0, distribution.count());
        assertNull(distribution.mean());
        assertNull(distribution.variance());
        assertNull(distribution.standardDeviation());
        assertNull(distribution.min());
        assertNull(distribution.max());
    }

    @Test
    public void testInitialiseWithValue() {
        final Distribution distribution = new Distribution(7, 9);
        assertEquals(9, distribution.count());
        assertEquals(7, distribution.mean(), Double.MIN_VALUE);
        assertEquals(0, distribution.variance(), Double.MIN_VALUE);
        assertEquals(0, distribution.standardDeviation(), Double.MIN_VALUE);
        assertEquals(7, distribution.min(), Double.MIN_VALUE);
        assertEquals(7, distribution.max(), Double.MIN_VALUE);
    }

    @Test
    public void testSummaryStatistics() {
        final Distribution distribution = new Distribution();
        // Sample = 1,2,2,2,3
        distribution.add(1);
        distribution.add(2, 3);
        distribution.add(3);
        // Statistics
        assertEquals(5, distribution.count());
        assertEquals(2, distribution.mean(), Double.MIN_VALUE);
        assertEquals(0.5, distribution.variance(), 0.001);
        assertEquals(0.707, distribution.standardDeviation(), 0.001);
        assertEquals(1, distribution.min(), Double.MIN_VALUE);
        assertEquals(3, distribution.max(), Double.MIN_VALUE);
    }

    @Test
    public void testAddDistribution() {
        final Distribution distributionA = new Distribution();
        final Distribution distributionB = new Distribution();
        // Sample = 1,2,2,2,3
        // A = 1,2,2
        distributionA.add(1);
        distributionA.add(2, 2);
        // B = 2,3
        distributionB.add(2);
        distributionB.add(3);
        // A + B
        final Distribution distribution = new Distribution();
        distribution.add(distributionA);
        distribution.add(distributionB);
        // Statistics
        assertEquals(5, distribution.count());
        assertEquals(2, distribution.mean(), Double.MIN_VALUE);
        assertEquals(0.5, distribution.variance(), 0.001);
        assertEquals(0.707, distribution.standardDeviation(), 0.001);
        assertEquals(1, distribution.min(), Double.MIN_VALUE);
        assertEquals(3, distribution.max(), Double.MIN_VALUE);
    }
}
