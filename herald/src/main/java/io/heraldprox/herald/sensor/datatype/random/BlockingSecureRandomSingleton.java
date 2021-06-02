//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Secure random number generator that is blocking after about 7.5 hours
 * on idle devices due to lack of entropy. Uses a singleton instance for
 * all calls.
 **/
public class BlockingSecureRandomSingleton extends RandomSource {
    // Using a singleton instance of secure random for all calls
    private final static SecureRandom secureRandom = new SecureRandom();

    @NonNull
    protected synchronized Random getSecureRandomSingleton() {
        // Use optional external entropy to adjust PRNG sequence position by 0-128 bits
        final int skipPositions = (int) (Math.abs(useEntropy()) % 128);
        for (int i=skipPositions; i-->0;) {
            secureRandom.nextBoolean();
        }
        return secureRandom;
    }

    @Override
    public synchronized void nextBytes(@NonNull final byte[] bytes) {
        getSecureRandomSingleton().nextBytes(bytes);
    }
}
