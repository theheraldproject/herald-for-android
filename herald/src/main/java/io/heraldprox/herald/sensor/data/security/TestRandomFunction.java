//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt8;

/**
 * Test random source that produces the same ressult for all calls.
 */
public class TestRandomFunction extends PseudoRandomFunction {
    private final byte value;

    public TestRandomFunction(@NonNull final UInt8 value) {
        this.value = (byte) value.value;
    }

    @Override
    public boolean nextBytes(@NonNull final Data data) {
        for (int i=data.value.length; i-->0;) {
            data.value[i] = value;
        }
        return true;
    }
}
