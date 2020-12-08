//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UInt8Tests {

    @Test
    public void testEncodeDecode() {
        for (int i=0; i<Byte.MAX_VALUE; i++) {
            assertEquals(i, UInt8.decode(UInt8.encode(i)));
            assertEquals(i, new UInt8(i).value);
        }
        assertEquals(0, UInt8.decode(UInt8.encode(-1)));
        assertEquals(127, UInt8.decode(UInt8.encode(128)));
    }
}
