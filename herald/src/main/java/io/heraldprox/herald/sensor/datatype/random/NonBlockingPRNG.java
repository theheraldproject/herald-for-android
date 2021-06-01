//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;

import java.util.Random;

/**
 * Non-blocking pseudo random number generator (PRNG) based on Random. Uses a new seed for every
 * call and incorporates entropy from elapsed time since last call, and also optional external
 * entropy. Use this when call time is inherently unpredictable due to external influences.
 */
public class NonBlockingPRNG extends RandomSource {
    private long getRandomLongLastCalledAt = System.nanoTime();

    @NonNull
    private synchronized Random getRandom() {
        // Use unpredictable time between calls to add entropy
        final long timestamp = System.nanoTime();
        final long entropyFromCallTime = (timestamp - getRandomLongLastCalledAt);
        // Skip 0 - 128 values on shared random resource Math.random() to increase search space
        // from Math.random() values to Math.random() seed. Note, other systems are also calling
        // Math.random(), thus the number of skipped values is inherent unpredictable. This has
        // been added to provide assurance that values are skipped even if nothing else is using
        // the Math.random() function.
        final int skipRandomSequence = (int) Math.abs(entropyFromCallTime % 128);
        for (int i=skipRandomSequence; i-->0;) {
            //noinspection ResultOfMethodCallIgnored
            Math.random();
        }
        // Create a new instance of Random with seed from Math.random() to increase search space
        // from value obtained from new Random instance to seed of Math.random().
        final Random random = new Random(Math.round(Math.random() * Long.MAX_VALUE));
        // Skip 256 - 1280 bits on new Random instance to increase search space from new Random
        // instance values to its seed. Using Math.random() to select skip distance to increase
        // search space.
        final int skipInitialBits = 256 + (int) Math.round(Math.random() * 1024);
        for (int i=skipInitialBits; i-->0;) {
            random.nextBoolean();
        }
        // Update timestamp for use in next call
        getRandomLongLastCalledAt = timestamp;
        // Use optional external entropy to adjust PRNG sequence position by 0-128 bits
        final int skipPositions = (int) (Math.abs(useEntropy()) % 128);
        for (int i=skipPositions; i-->0;) {
            random.nextBoolean();
        }
        return random;
    }

    @Override
    public void nextBytes(@NonNull final byte[] bytes) {
        getRandom().nextBytes(bytes);
    }
}
