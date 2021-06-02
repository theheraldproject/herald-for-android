//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Secure random number generator that is blocking after about 4.5 hours
 * on idle devices due to lack of entropy. Uses a new instance for every
 * call.
 **/
public class BlockingSecureRandom extends RandomSource {

    @NonNull
    protected synchronized Random getSecureRandom() {
        // Get new instance of secure random based on a new seed
        final SecureRandom random = new SecureRandom();
        // Use optional external entropy to adjust PRNG sequence position by 0-128 bits
        final int skipPositions = (int) (Math.abs(useEntropy()) % 128);
        for (int i=skipPositions; i-->0;) {
            random.nextBoolean();
        }
        return random;
    }

    @Override
    public void nextBytes(@NonNull final byte[] bytes) {
        getSecureRandom().nextBytes(bytes);
    }
}
