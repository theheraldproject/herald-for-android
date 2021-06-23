//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImmediateSendDataTests {

    @Test
    public void testInit() {
        final Data data = new Data((byte) 3,7);
        final ImmediateSendData immediateSendData = new ImmediateSendData(data);
        assertEquals(data, immediateSendData.data);
    }
}
