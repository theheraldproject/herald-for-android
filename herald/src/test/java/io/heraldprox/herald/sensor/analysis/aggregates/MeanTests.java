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
public class MeanTests {

    @Test
    public void testEmpty() {
        final Mean<Int8> mean = new Mean<>();
        assertNull(mean.reduce());
    }

    @Test
    public void testRuns() {
        final Mean<Int8> mean = new Mean<>();
        assertEquals(1, mean.runs());
    }

    @Test
    public void testMapReduce() {
        final Mean<Int8> mean = new Mean<>();
        int sum = 0, count = 0;
        for (int i=0; i<10; i++) {
            mean.map(new Sample<>(new Int8(i)));
            assertNotNull(mean.reduce());
            sum += i;
            count++;
            assertEquals(sum/(double) count, mean.reduce(), Double.MIN_VALUE);
        }
    }

    @Test
    public void testBeginRun() {
        final Mean<Int8> mean = new Mean<>();
        assertNull(mean.reduce());
        // Run > 1 should bypass inputs, so no change to output
        mean.beginRun(2);
        mean.map(new Sample<>(new Int8(10)));
        assertNull(mean.reduce());
    }

    @Test
    public void testReset() {
        final Mean<Int8> mean = new Mean<>();
        mean.map(new Sample<>(new Int8(10)));
        assertEquals(10, mean.reduce(), Double.MIN_VALUE);
        mean.reset();
        assertNull(mean.reduce());
    }
}
