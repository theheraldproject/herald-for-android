//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateTests {

    @Test
    public void secondsSinceUnixEpoch() {
        assertEquals(0, new Date(0).secondsSinceUnixEpoch());
        assertEquals(1, new Date(1).secondsSinceUnixEpoch());
    }

    @Test
    public void getTime() {
        assertEquals(0, new Date(0).getTime());
        assertEquals(1000, new Date(1).getTime());
    }

    @Test
    public void testToString() {
        System.err.println(new Date(0));
    }

    @Test
    public void beforeOrEqual() {
        assertTrue(new Date(1).beforeOrEqual(new Date(2)));
        assertTrue(new Date(2).beforeOrEqual(new Date(2)));
        assertFalse(new Date(3).beforeOrEqual(new Date(2)));
    }

    @Test
    public void afterOrEqual() {
        assertTrue(new Date(3).afterOrEqual(new Date(2)));
        assertTrue(new Date(2).afterOrEqual(new Date(2)));
        assertFalse(new Date(1).afterOrEqual(new Date(2)));
    }
}
