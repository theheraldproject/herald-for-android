//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UInt16Tests {

    @Test
    public void testEncodeDecode() {
        for (int i=0; i<Short.MAX_VALUE; i++) {
            assertEquals(i, UInt16.decode(UInt16.encode(i)));
            assertEquals(i, new UInt16(i).value);
        }
        assertEquals(0, UInt16.decode(UInt16.encode(-1)));
        assertEquals(Short.MAX_VALUE, UInt16.decode(UInt16.encode(Short.MAX_VALUE + 1)));
    }
}
