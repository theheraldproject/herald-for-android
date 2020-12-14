//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImmediateSendDataTests {

    @Test
    public void testInit() {
        final Data data = new Data((byte) 3,7);
        final ImmediateSendData immediateSendData = new ImmediateSendData(data);
        assertEquals(data, immediateSendData.data);
    }
}
