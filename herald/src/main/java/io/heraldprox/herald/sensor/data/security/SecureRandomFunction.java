//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.random.NonBlockingSecureRandom;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

public class SecureRandomFunction extends PseudoRandomFunction {
    private final RandomSource randomSource = new NonBlockingSecureRandom();

    @Override
    public boolean nextBytes(@NonNull final Data data) {
        randomSource.nextBytes(data.value);
        return true;
    }
}
