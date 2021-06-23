//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SignalCharacteristicDataTypeTests {

    @Test
    public void testEncodeDecode() {
        for (SignalCharacteristicDataType value : SignalCharacteristicDataType.values()) {
            assertEquals(value, SignalCharacteristicDataType.valueOf(value.name()));
        }
    }
}
