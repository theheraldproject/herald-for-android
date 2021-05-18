//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import java.security.MessageDigest;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Source of random data. Override and implement nextBytes() method to provide
 * custom random source that is application specific.
 */
public abstract class RandomSource {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.RandomSource");
    /** Entropy gathered from external sources via addEntropy() */
    private long entropy = (long) (Math.random() * Long.MAX_VALUE);

    /**
     * Contribute entropy from external source, e.g. unpredictable time intervals
     *
     * @param value entropy skip ahead value in bits
     **/
    public synchronized void addEntropy(final long value) {
        entropy ^= value;
    }

    /**
     * Collect entropy from external sources. Use detected BLE MAC addresses as an entropy source
     * as these addresses are mostly, if not all, derived from disparate SecureRandom instances.
     *
     * @param value BLE MAC address of target device
     */
    public synchronized void addEntropy(final String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        // Use cryptographic hash function to separate entropy material from source value
        final Data hashOfValue = sha256(new Data(value.getBytes()));
        // Truncate hash to derive entropy, using system-wide random instance to select index and
        // also add entropy to the singleton random instance as detections are truly random events.
        final int index = (int) (Math.abs(Double.doubleToLongBits(Math.random() * 0xFFFFFFFFFFFFFFFFl)) % (hashOfValue.value.length - 8));
        final long entropy = hashOfValue.int64(index).value;
        logger.debug("Added entropy (value={})", entropy);
        // Accumulate entropy
        addEntropy(entropy);
    }

    /**
     * Spend entropy from external source. Entropy is replaced by random value.
     *
     * @return Entropy gathered by addEntropy()
     */
    protected synchronized long useEntropy() {
        final long useEntropy = entropy;
        entropy = Double.doubleToLongBits(Math.random() * 0xFFFFFFFFFFFFFFFFl);
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

    /**
     * Cryptographic hash function (SHA256) for separating the random value from
     * its random source through hashing and truncation.
     */
    protected final static Data sha256(final Data data) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(data.value);
            return new Data(hash);
        } catch (Throwable e) {
            logger.fault("SHA-256 unavailable", e);
            return data;
        }
    }
}
