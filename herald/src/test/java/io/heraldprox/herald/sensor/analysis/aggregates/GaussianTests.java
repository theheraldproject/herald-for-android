//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.aggregates;

import org.junit.Test;

import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Int8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class GaussianTests {

    @Test
    public void testEmpty() {
        // Empty model should return null for mean
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertNull(gaussian.reduce());
    }

    @Test
    public void testRuns() {
        // One-pass algorithm returns 1 for runs
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertEquals(1, gaussian.runs());
    }

    @Test
    public void testMapReduce() {
        final Gaussian<Int8> gaussian = new Gaussian<>();
        // Sample = 1,2,2,2,3
        gaussian.map(new Sample<>(new Int8(1)));
        gaussian.map(new Sample<>(new Int8(2)));
        gaussian.map(new Sample<>(new Int8(2)));
        gaussian.map(new Sample<>(new Int8(2)));
        gaussian.map(new Sample<>(new Int8(3)));
        // Sample statistics
        assertEquals(2, gaussian.reduce(), Double.MIN_VALUE);
        assertEquals(2, gaussian.model().mean(), Double.MIN_VALUE);
        assertEquals(0.5, gaussian.model().variance(), 0.001);
        assertEquals(0.707, gaussian.model().standardDeviation(), 0.001);
        assertEquals(1, gaussian.model().min(), Double.MIN_VALUE);
        assertEquals(3, gaussian.model().max(), Double.MIN_VALUE);
    }

    @Test
    public void testBeginRun() {
        // Empty model should return null for mean
        final Gaussian<Int8> gaussian = new Gaussian<>();
        assertNull(gaussian.reduce());
        // Run > 1 should bypass inputs, so no change to output
        gaussian.beginRun(2);
        gaussian.map(new Sample<>(new Int8(10)));
        assertNull(gaussian.reduce());
    }

    @Test
    public void testReset() {
        // Model with one sample should return sample for mean
        final Gaussian<Int8> gaussian = new Gaussian<>();
        gaussian.map(new Sample<>(new Int8(10)));
        assertEquals(10, gaussian.reduce(), Double.MIN_VALUE);
        // Model should be empty after reset and returns null for mean
        gaussian.reset();
        assertNull(gaussian.reduce());
    }
}
