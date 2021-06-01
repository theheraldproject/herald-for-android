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

@SuppressWarnings("ConstantConditions")
public class VarianceTests {

    @Test
    public void testEmpty() {
        final Variance<Int8> variance = new Variance<>();
        assertNull(variance.reduce());
    }

    @Test
    public void testRuns() {
        final Variance<Int8> variance = new Variance<>();
        assertEquals(2, variance.runs());
    }

    @Test
    public void testMapReduce() {
        final Variance<Int8> variance = new Variance<>();
        int sum = 0, count = 0;
        for (int i=0; i<10; i++) {
            variance.map(new Sample<>(new Int8(i)));
            assertNull(variance.reduce());
            sum += i;
            count++;
        }
        final double mean = sum / (double) count;
        // Run 2
        variance.beginRun(2);
        double sumSquaredDelta = (0 - mean) * (0 - mean);
        count = 1;
        variance.map(new Sample<>(new Int8(0)));
        for (int i=1; i<10; i++) {
            variance.map(new Sample<>(new Int8(i)));
            assertNotNull(variance.reduce());
            sumSquaredDelta += ((i - mean) * (i - mean));
            count++;
            assertEquals(sumSquaredDelta / (count - 1), variance.reduce(), Double.MIN_VALUE);
        }
    }

    @Test
    public void testBeginRun() {
        final Variance<Int8> variance = new Variance<>();
        // Run 1 calculates mean, should not produce output
        assertNull(variance.reduce());
        variance.map(new Sample<>(new Int8(1)));
        assertNull(variance.reduce());
        variance.map(new Sample<>(new Int8(1)));
        assertNull(variance.reduce());
        // Run 2 calculates variance, should produce output after 2 samples
        variance.beginRun(2);
        assertNull(variance.reduce());
        variance.map(new Sample<>(new Int8(1)));
        assertNull(variance.reduce());
        variance.map(new Sample<>(new Int8(1)));
        assertNotNull(variance.reduce());
        assertEquals(0, variance.reduce(), Double.MIN_VALUE);
    }

    @Test
    public void testReset() {
        final Variance<Int8> variance = new Variance<>();
        // Run 1 calculates mean, should not produce output
        variance.map(new Sample<>(new Int8(1)));
        variance.map(new Sample<>(new Int8(1)));
        assertNull(variance.reduce());
        // Run 2 calculates variance, should produce output after 2 samples
        variance.beginRun(2);
        variance.map(new Sample<>(new Int8(1)));
        variance.map(new Sample<>(new Int8(1)));
        assertNotNull(variance.reduce());
        assertEquals(0, variance.reduce(), Double.MIN_VALUE);
        // Reset
        variance.reset();
        assertNull(variance.reduce());
    }
}
