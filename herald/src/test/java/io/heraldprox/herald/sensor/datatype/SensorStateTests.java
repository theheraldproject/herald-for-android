//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SensorStateTests {

    @Test
    public void testEncodeDecode() {
        for (SensorState value : SensorState.values()) {
            assertEquals(value, SensorState.valueOf(value.name()));
        }
    }
}
