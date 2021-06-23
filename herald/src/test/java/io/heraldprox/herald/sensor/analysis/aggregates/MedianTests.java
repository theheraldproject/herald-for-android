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
public class MedianTests {

    @Test
    public void testEmpty() {
        final Median<Int8> median = new Median<>();
        assertNull(median.reduce());
    }

    @Test
    public void testRuns() {
        final Median<Int8> median = new Median<>();
        assertEquals(1, median.runs());
    }

    @Test
    public void testMapReduce() {
        final Median<Int8> median = new Median<>();
        // Median [1] is 1
        median.map(new Sample<>(new Int8(1)));
        assertEquals(1, median.reduce(), Double.MIN_VALUE);
        // Median [1,2] is 1.5, collection with even number of elements reports mean of the two centre values
        median.map(new Sample<>(new Int8(2)));
        assertEquals(1.5, median.reduce(), Double.MIN_VALUE);
        // Median [1,2,3] is 2, collection with odd number of elements reports centre value
        median.map(new Sample<>(new Int8(3)));
        assertEquals(2, median.reduce(), Double.MIN_VALUE);
        // Median [1,2,3,4] is 2.5, collection with even number of elements reports mean of the two centre values
        median.map(new Sample<>(new Int8(4)));
        assertEquals(2.5, median.reduce(), Double.MIN_VALUE);
        // Median [1,2,2,3,4] is 2
        median.map(new Sample<>(new Int8(2)));
        assertEquals(2, median.reduce(), Double.MIN_VALUE);
        // Median [1,2,2,3,4,4] is 2.5
        median.map(new Sample<>(new Int8(4)));
        assertEquals(2.5, median.reduce(), Double.MIN_VALUE);
    }

    @Test
    public void testBeginRun() {
        final Median<Int8> median = new Median<>();
        assertNull(median.reduce());
        // Run > 1 should bypass inputs, so no change to output
        median.beginRun(2);
        median.map(new Sample<>(new Int8(10)));
        assertNull(median.reduce());
    }

    @Test
    public void testReset() {
        final Median<Int8> median = new Median<>();
        median.map(new Sample<>(new Int8(10)));
        assertEquals(10, median.reduce(), Double.MIN_VALUE);
        median.reset();
        assertNull(median.reduce());
    }
}
