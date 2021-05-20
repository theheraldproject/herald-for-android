//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import java.security.MessageDigest;
import java.util.Arrays;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.Int64;

/**
 * Source of random data. Override and implement nextBytes() method to provide
 * custom random source that is application specific.
 */
public abstract class RandomSource {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.RandomSource");
    /** Entropy gathered from external sources via addEntropy() */
    private final RingBuffer entropy = new RingBuffer(2048);
    private long lastUseEntropyTimestamp = System.nanoTime();

    // MARK: - Entropy gathering and usage

    /**
     * Contribute entropy from external source, e.g. unpredictable time intervals
     *
     * @param value entropy skip ahead value in bits
     **/
    public synchronized void addEntropy(final long value) {
        entropy.push(value);
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
        final Data entropyData = new Data(value.getBytes());
        entropyData.append(new Int64(System.nanoTime()));
        entropy.push(hash(entropyData));
    }

    /**
     * Spend entropy from external source. Entropy is cleared upon use.
     *
     * @return Entropy gathered by addEntropy()
     */
    protected synchronized long useEntropy() {
        // Hash function may fail if SHA-256 is not available, or there is no entropy data.
        Data hash = entropy.hash();
        if (hash == null || hash.value.length < 8) {
            // Revert to elapsed time alone as entropy source. Hash function will always succeed.
            final Data data = new Data();
            data.append(new Int64(lastUseEntropyTimestamp ^ System.nanoTime()));
            hash = hash(data);
        }
        lastUseEntropyTimestamp = System.nanoTime();
        entropy.clear();
        return hash.uint64(0).value;
    }

    /**
     * Spend entropy by transferring available entropy data to sink. Entropy is cleared upon use.
     * @param sink Target data buffer for transferring entropy data.
     */
    protected synchronized void useEntropy(final Data sink) {
        sink.append(entropy.get());
        entropy.clear();
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
     * Cryptographic hash function : SHA256
     * Reverts to non-cryptographic Java hash function in the unlikely event that
     * SHA256 is not supported on the system. This should never happen as SHA256
     * must be supported in all Java implementations.
     */
    protected final static Data hash(final Data data) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(data.value);
            return new Data(hash);
        } catch (Throwable e) {
            // This should never happen as every implementation of Java should
            // support MD5, SHA1, and SHA256 as a minimum.
            logger.fault("SHA-256 unavailable, emulating with non-cryptographic hash function", e);
            // Revert to build-in hash function to emulate 256-bit (32-byte) hash
            final Data hash = new Data();
            hash.append(new Int32(Arrays.hashCode(data.value)));
            while (hash.value.length < 32) {
                hash.append(new Int32(Arrays.hashCode(hash.value)));
            }
            return hash;
        }
    }
}
