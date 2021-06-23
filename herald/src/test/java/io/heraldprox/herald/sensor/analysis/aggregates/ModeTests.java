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
public class ModeTests {

    @Test
    public void testEmpty() {
        final Mode<Int8> mode = new Mode<>();
        assertNull(mode.reduce());
    }

    @Test
    public void testRuns() {
        final Mode<Int8> mode = new Mode<>();
        assertEquals(1, mode.runs());
    }

    @Test
    public void testMapReduce() {
        final Mode<Int8> mode = new Mode<>();
        mode.map(new Sample<>(new Int8(1)));
        assertEquals(1, mode.reduce(), Double.MIN_VALUE);
        // Mode can be 1 or 2, but implementation selects largest value for deterministic output
        mode.map(new Sample<>(new Int8(2)));
        assertEquals(2, mode.reduce(), Double.MIN_VALUE);
        // Mode can be 1, 2, or 3, but implementation selects largest value for deterministic output
        mode.map(new Sample<>(new Int8(3)));
        assertEquals(3, mode.reduce(), Double.MIN_VALUE);
        // Mode is 2, as 2 has highest frequency of occurrence
        mode.map(new Sample<>(new Int8(2)));
        assertEquals(2, mode.reduce(), Double.MIN_VALUE);
        // Mode is 2 or 3, as 2 and 3 have highest frequency of occurrence, but implementation selects largest value for deterministic output
        mode.map(new Sample<>(new Int8(3)));
        assertEquals(3, mode.reduce(), Double.MIN_VALUE);
    }

    @Test
    public void testBeginRun() {
        final Mode<Int8> mode = new Mode<>();
        assertNull(mode.reduce());
        // Run > 1 should bypass inputs, so no change to output
        mode.beginRun(2);
        mode.map(new Sample<>(new Int8(10)));
        assertNull(mode.reduce());
    }

    @Test
    public void testReset() {
        final Mode<Int8> mode = new Mode<>();
        mode.map(new Sample<>(new Int8(10)));
        assertEquals(10, mode.reduce(), Double.MIN_VALUE);
        mode.reset();
        assertNull(mode.reduce());
    }
}
