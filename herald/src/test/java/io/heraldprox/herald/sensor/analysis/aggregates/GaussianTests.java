//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import org.junit.Test;

import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Int8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GaussianTests {

    @Test
    public void testEmpty() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertNull(gaussian.reduce());
    }

    @Test
    public void testRuns() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertEquals(1, gaussian.runs());
    }

    @Test
    public void testMapReduce() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        int sum = 0, count = 0;
        for (int i=0; i<10; i++) {
            gaussian.map(new Sample<>(new Int8(i)));
            assertNotNull(gaussian.reduce());
            sum += i;
            count++;
            // One pass algorithm for mean and variance
            assertEquals(sum/(double) count, gaussian.reduce().doubleValue(), Double.MIN_VALUE);
            if (count > 1) {
                final double mean = sum / (double) count;
                double sumSquaredDelta = 0;
                for (int j=0; j<=i; j++) {
                    sumSquaredDelta += ((j - mean) * (j - mean));
                }
                final double variance = sumSquaredDelta / (count - 1);
                assertEquals(variance, gaussian.model().variance(), Double.MIN_VALUE);
            }
        }
        assertEquals(10, gaussian.model().count());
        assertEquals(0, gaussian.model().min(), Double.MIN_VALUE);
        assertEquals(9, gaussian.model().max(), Double.MIN_VALUE);
    }

    @Test
    public void testBeginRun() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertNull(gaussian.reduce());
        // Run > 1 should bypass inputs, so no change to output
        gaussian.beginRun(2);
        gaussian.map(new Sample<>(new Int8(10)));
        assertNull(gaussian.reduce());
    }

    @Test
    public void testReset() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        gaussian.map(new Sample<>(new Int8(10)));
        assertEquals(10, gaussian.reduce().doubleValue(), Double.MIN_VALUE);
        gaussian.reset();
        assertNull(gaussian.reduce());
    }
}
