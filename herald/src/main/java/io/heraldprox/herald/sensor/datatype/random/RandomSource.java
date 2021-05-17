//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Source of random data. Override and implement nextBytes() method to provide
 * custom random source that is application specific.
 */
public abstract class RandomSource {
    /** Entropy gathered from external sources via addEntropy() */
    private long entropy = (long) (Math.random() * Long.MAX_VALUE);

    /**
     * Contribute entropy from external source, e.g. unpredictable time intervals
     *
     * @param value entropy skip ahead value in bits
     **/
    public synchronized void addEntropy(final long value) {
        entropy += value;
    }

    /**
     * Spend entropy from external source. Entropy is replaced by random value.
     *
     * @return Entropy gathered by addEntropy()
     */
    protected synchronized long useEntropy() {
        final long useEntropy = entropy;
        entropy = (long) (Math.random() * Long.MAX_VALUE);
        return useEntropy;
    }

    // MARK:- Random data

    /**
     * Get random bytes from the random source.
     *
     * @param bytes Fill byte array with random data.
     */
    public abstract void nextBytes(final byte[] bytes);

    /**
     * Get random int value from random source.
     *
     * @return Random int value derived from 4 random bytes.
     */
    public int nextInt() {
        final Data data = new Data(new byte[4]);
        nextBytes(data.value);
        return data.int32(0).value;
    }

    /**
     * Get random long value from random source.
     *
     * @return Random long value derived from 8 random bytes.
     */
    public long nextLong() {
        final Data data = new Data(new byte[8]);
        nextBytes(data.value);
        return data.int64(0).value;
    }
}
