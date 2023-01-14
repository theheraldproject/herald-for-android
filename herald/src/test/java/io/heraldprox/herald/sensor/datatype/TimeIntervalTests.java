//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TimeIntervalTests {

    @Test
    public void testEquals() {
        assertEquals(TimeInterval.zero, new TimeInterval(0));
        assertEquals(TimeInterval.never, new TimeInterval(Long.MAX_VALUE));
        assertEquals(TimeInterval.minute, new TimeInterval(60));
        assertEquals(TimeInterval.minute, TimeInterval.seconds(60));
        assertEquals(TimeInterval.minute, TimeInterval.minutes(1));
    }

    @Test
    public void testHash() {
        assertEquals(TimeInterval.zero.hashCode(), new TimeInterval(0).hashCode());
        assertEquals(TimeInterval.never.hashCode(), new TimeInterval(Long.MAX_VALUE).hashCode());
        assertEquals(TimeInterval.minute.hashCode(), new TimeInterval(60).hashCode());
        assertEquals(TimeInterval.minute.hashCode(), TimeInterval.seconds(60).hashCode());
        assertEquals(TimeInterval.minute.hashCode(), TimeInterval.minutes(1).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("0", new TimeInterval(0).toString());
        assertEquals(TimeInterval.never.toString(), new TimeInterval(Long.MAX_VALUE).toString());
        assertEquals("60", new TimeInterval(60).toString());
        assertEquals("60", TimeInterval.seconds(60).toString());
        assertEquals("60", TimeInterval.minutes(1).toString());
        assertEquals("120", TimeInterval.minutes(2).toString());
        assertEquals("86400", TimeInterval.days(1).toString());
    }

    @Test
    public void testMillis() {
        assertEquals(0, TimeInterval.zero.millis());
        assertEquals(1000, TimeInterval.seconds(1).millis());
    }

    @Test
    public void testInit() {
        assertEquals(0, new TimeInterval(new Date(0)).value);
        assertEquals(1, new TimeInterval(new Date(1000)).value);

        assertEquals(1, new TimeInterval(new Date(1000), new Date(2000)).value);
    }
}
