package com.vmware.herald.sensor.datatype;

import org.junit.Test;

public class PayloadDataTests {

    @Test
    public void testShortName() throws Exception {
        for (int i=0; i<600; i++) {
            final PayloadData payloadData = new PayloadData((byte) 0, i);
            System.err.println(i + " -> " + payloadData.shortName());
        }
    }

}
