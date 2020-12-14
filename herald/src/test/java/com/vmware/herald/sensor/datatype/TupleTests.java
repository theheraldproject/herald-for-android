//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TupleTests {

    @Test
    public void testInit() {
        assertEquals("A", new Tuple<>("A","B").a);
        assertEquals("B", new Tuple<>("A","B").b);
        assertNotNull(new Tuple<>("A","B").toString());
        assertNotNull(new Tuple<String,String>(null,null).toString());
    }
}
