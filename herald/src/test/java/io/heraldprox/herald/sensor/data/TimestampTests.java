package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.Date;

public class TimestampTests {

    @Test
    public void codec() {
        final String string = Timestamp.timestamp(new Date(0));
        final Date date = Timestamp.timestamp(string);
        assertEquals(0, date.secondsSinceUnixEpoch());
    }
}
