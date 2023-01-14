package io.heraldprox.herald.sensor.data;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.PayloadData;

public class ConcretePayloadDataFormatterTests {

    @Test
    public void shortFormat() {
        assertEquals("", new ConcretePayloadDataFormatter().shortFormat(new PayloadData()));
        assertEquals("", new ConcretePayloadDataFormatter().shortFormat(new PayloadData("a")));
        assertEquals("aaaa", new ConcretePayloadDataFormatter().shortFormat(new PayloadData("aaaa")));
        assertEquals("aaaa", new ConcretePayloadDataFormatter().shortFormat(new PayloadData("aaaaaaaa")));
    }
}
