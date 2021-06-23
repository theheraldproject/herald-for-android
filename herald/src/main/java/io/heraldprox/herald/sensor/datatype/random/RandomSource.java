//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int16;
import io.heraldprox.herald.sensor.datatype.Int32;
import io.heraldprox.herald.sensor.datatype.Int64;

/**
 * Source of random data. Override and implement nextBytes() method to provide
 * custom random source that is application specific.
 */
public abstract class RandomSource {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.RandomSource");
    /** Entropy gathered from external sources via addEntropy() */
    public final RingBuffer entropy = new RingBuffer(4096);
    private long lastUseEntropyTimestamp = System.nanoTime();

    // MARK: - Entropy gathering and usage

    /**
     * Contribute entropy from external source.
     *
     * @param value entropy data
     **/
    public synchronized void addEntropy(final byte value) {
        entropy.push(value);
    }

    /**
     * Contribute entropy from external source.
     *
     * @param value entropy data
     **/
    public synchronized void addEntropy(final long value) {
        entropy.push(value);
    }

    /**
     * Collect entropy from external sources. Use detected BLE MAC addresses as an entropy source
     * as these addresses are mostly, if not all, derived from disparate SecureRandom instances.
     *
     * @param value BLE MAC address of target device, only the hex digits [0-9A-Z] are used
     */
    public synchronized void addEntropy(@Nullable final String value) {
        if (null == value) {
            return;
        }
        // Add target identifier and detection time as entropy data
        final Data entropyData = new Data();
        // Add address hex value as entropy
        // Using Locale.US to force ASCII string conversion
        final String hexAddress = value.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
        //noinspection ConstantConditions
        if (null == hexAddress || hexAddress.isEmpty()) {
            return;
        }
        entropyData.append(new Data(hexAddress.getBytes(StandardCharsets.UTF_8)));
        // Add detection time (last 16 bits) as entropy
        entropyData.append(new Int16((int) (System.nanoTime() & 0xFFFF)));
        // Add entropy
        entropy.push(entropyData);
    }

    /**
     * Spend entropy from external source. Entropy is cleared upon use.
     *
     * @return Entropy gathered by addEntropy()
     */
    protected synchronized long useEntropy() {
        // Hash function may fail if SHA-256 is not available, or there is no entropy data.
        Data hash = entropy.hash();
        if (null == hash || hash.value.length < 8) {
            // Revert to elapsed time alone as entropy source. Hash function will always succeed.
            final Data data = new Data();
            data.append(new Int64(lastUseEntropyTimestamp ^ System.nanoTime()));
            hash = hash(data);
        }
        lastUseEntropyTimestamp = System.nanoTime();
        entropy.clear();
        //noinspection ConstantConditions
        return hash.uint64(0).value;
    }

    /**
     * Spend entropy by transferring entropy data to sink. Spent entropy is cleared and not reused.
     * @param bytes Maximum number of bytes to transfer if available.
     * @param sink Target data buffer for transferring entropy data.
     */
    public synchronized void useEntropy(final int bytes, @NonNull final Data sink) {
        sink.append(entropy.pop(bytes));
    }

    // MARK:- Random data

    /**
     * Get random bytes from the random source.
     *
     * @param bytes Fill byte array with random data.
     */
    public abstract void nextBytes(@NonNull final byte[] bytes);

    /**
     * Get random int value from random source.
     *
     * @return Random int value derived from 4 random bytes.
     */
    public int nextInt() {
        final Data data = new Data(new byte[4]);
        nextBytes(data.value);
        //noinspection ConstantConditions
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
        //noinspection ConstantConditions
        return data.int64(0).value;
    }

    /**
     * Cryptographic hash function : SHA256
     * Reverts to non-cryptographic Java hash function in the unlikely event that
     * SHA256 is not supported on the system. This should never happen as SHA256
     * must be supported in all Java implementations.
     */
    @NonNull
    protected static Data hash(@NonNull final Data data) {
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
