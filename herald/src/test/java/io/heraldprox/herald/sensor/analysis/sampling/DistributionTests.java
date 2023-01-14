//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import io.heraldprox.herald.sensor.datatype.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DistributionTests {

    @Test
    public void testSample() {
        final Sample<Integer> s1 = new Sample<>(new Date(1), 1);
        final Sample<Integer> s1Copy = new Sample<>(new Date(1), 1);
        final Sample<Integer> s2 = new Sample<>(new Date(2), 2);

        assertEquals(s1.taken(), s1Copy.taken());
        assertEquals(s1.value(), s1Copy.value());
        assertNotEquals(s1.taken(), s2.taken());
        assertNotEquals(s1.value(), s2.value());
    }
}
