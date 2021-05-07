//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
}
