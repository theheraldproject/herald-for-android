//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithm.distance;

import org.junit.Test;

import io.heraldprox.herald.sensor.analysis.algorithms.distance.FowlerBasic;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.Int8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class FowlerBasicTests {

    @Test
    public void testEmpty() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(1,1);
        assertNull(f.reduce());
    }

    @Test
    public void testZeroCoefficient() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(1,0);
        assertNull(f.reduce());
        f.map(new Sample<>(new Int8(1)));
        assertNull(f.reduce());
    }

    @Test
    public void testRuns() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(1,1);
        assertEquals(1, f.runs());
    }

    @Test
    public void testMapReduce() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(0,1);
        f.map(new Sample<>(new Int8(1)));
        assertEquals(Math.pow(10, 1), f.reduce(), Double.MIN_VALUE);
        // Mode = 2
        f.map(new Sample<>(new Int8(2)));
        assertEquals(Math.pow(10, 2), f.reduce(), Double.MIN_VALUE);
        // Mode = 3
        f.map(new Sample<>(new Int8(3)));
        assertEquals(Math.pow(10, 3), f.reduce(), Double.MIN_VALUE);
        // Mode = 2
        f.map(new Sample<>(new Int8(2)));
        assertEquals(Math.pow(10, 2), f.reduce(), Double.MIN_VALUE);
        // Mode = 3
        f.map(new Sample<>(new Int8(3)));
        assertEquals(Math.pow(10, 3), f.reduce(), Double.MIN_VALUE);
    }

    @Test
    public void testBeginRun() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(0,1);
        assertNull(f.reduce());
        // Run > 1 should bypass inputs, so no change to output
        f.beginRun(2);
        f.map(new Sample<>(new Int8(10)));
        assertNull(f.reduce());
    }

    @Test
    public void testReset() {
        final FowlerBasic<Int8> f = new FowlerBasic<>(0,1);
        f.map(new Sample<>(new Int8(2)));
        assertEquals(Math.pow(10, 2), f.reduce(), Double.MIN_VALUE);
        f.reset();
        assertNull(f.reduce());
    }
}
