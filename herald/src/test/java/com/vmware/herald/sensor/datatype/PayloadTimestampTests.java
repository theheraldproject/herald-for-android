//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PayloadTimestampTests {

    @Test
    public void testInit() {
        final Date date = new Date(1);
        final PayloadTimestamp payloadTimestamp = new PayloadTimestamp(date);
        assertNotNull(payloadTimestamp.value);
        assertEquals(date.getTime(), payloadTimestamp.value.getTime());
        assertNotNull(new PayloadTimestamp().value);
    }
}
