//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;

import java.security.SecureRandom;
import java.util.Random;

/// Source of random data
public class RandomSource {
    final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.RandomSource");
    public final Method method;
    private Random random = null;
    private short externalEntropy = 0;
    public enum Method {
        // Singleton random source reused per call
        Random, SecureRandomSingleton, SecureRandom, SecureRandomNIST
    }

    public RandomSource(final Method method) {
        this.method = method;
        externalEntropy = (short) (Math.random() * Short.MAX_VALUE);
    }

    /// Contribute entropy from external source, e.g. unpredictable time intervals
    public synchronized void addEntropy(final long value) {
        final short contribution = (short) (value % Short.MAX_VALUE);
        externalEntropy += contribution;
    }

    // MARK:- Random data

    public void nextBytes(byte[] bytes) {
        init();
        random.nextBytes(bytes);
    }

    public int nextInt() {
        init();
        return random.nextInt();
    }

    public long nextLong() {
        init();
        return random.nextLong();
    }

    public double nextDouble() {
        init();
        return random.nextDouble();
    }

    // MARK:- Random source initialisation

    /// Initialise random according to method
    protected synchronized void init() {
        switch (method) {
            case Random: {
                random = getRandom();
                break;
            }
            case SecureRandomSingleton: {
                random = getSecureRandomSingleton();
                break;
            }
            case SecureRandom: {
                random = getSecureRandom();
                break;
            }
            case SecureRandomNIST: {
                random = getSecureRandomNIST();
                break;
            }
        }
        // Use external entropy to adjust PRNG sequence position by 0-128 bits
        final int skipPositions = (Math.abs(externalEntropy) % 128);
        for (int i=skipPositions; i-->0;) {
            random.nextBoolean();
        }
        externalEntropy = 0;
    }

    // MARK:- PRNG implementations

    /// Non-blocking random number generator with reliable entropy source.
    private static long getRandomLongLastCalledAt = System.nanoTime();
    private synchronized final static Random getRandom() {
        // Use unpredictable time between calls to add entropy
        final long timestamp = System.nanoTime();
        final long entropy = (timestamp - getRandomLongLastCalledAt);
        // Skip 0 - 128 values on shared random resource Math.random() to increase search space
        // from Math.random() values to Math.random() seed. Note, other systems are also calling
        // Math.random(), thus the number of skipped values is inherent unpredictable. This has
        // been added to provide assurance that values are skipped even if nothing else is using
        // the Math.random() function.
        final int skipRandomSequence = (int) Math.abs(entropy % 128);
        for (int i=skipRandomSequence; i-->0;) {
            Math.random();
        }
        // Create a new instance of Random with seed from Math.random() to increase search space
        // from value obtained from new Random instance to seed of Math.random().
        final Random random = new Random(Math.round(Math.random() * Long.MAX_VALUE));
        // Skip 256 - 1280 bits on new Random instance to increase search space from
        // new Random instance values to its seed. Using Math.random() to select skip
        // distance to increase search space.
        final int skipInitialBits = (int) Math.abs(256 + Math.round(Math.random() * 1024));
        for (int i=skipInitialBits; i-->0;) {
            random.nextBoolean();
        }
        // Update timestamp for use in next call
        getRandomLongLastCalledAt = timestamp;
        // Get next long
        return random;
    }

    /// Secure random number generator that is blocking after about 7.5 hours
    /// on idle devices due to lack of entropy.
    private static SecureRandom secureRandomSingleton = null;
    private synchronized final static Random getSecureRandomSingleton() {
        if (secureRandomSingleton == null) {
            secureRandomSingleton = new SecureRandom();
        }
        return secureRandomSingleton;
    }


    /// Secure random number generator that is blocking after about 4.5 hours
    /// on idle devices due to lack of entropy.
    private final static Random getSecureRandom() {
        return new SecureRandom();
    }

    /// Secure random number generator that is blocking after about 6.0 hours
    /// on idle devices due to lack of entropy.
    /// SecureRandom seeded according to NIST SP800-90A recommendations
    /// - SHA1PRNG algorithm
    /// - Algorithm seeded with 440 bits of secure random data
    /// - Skips first random number of bytes to mitigate against poor implementations
    /// Compliance to NIST SP800-90A offers quality assurance against an accepted
    /// standard. The aim here is not to offer the most perfect random source, but
    /// a source with well defined and understood characteristics, thus enabling
    /// selection of the most appropropriate method, given the intented purpose.
    /// This implementation supports security strength for NIST SP800-57
    /// Part 1 Revision 5 (informally, generation of cryptographic keys for
    /// encryption of sensitive data).
    private final static Random getSecureRandomNIST() {
        try {
            // Obtain SHA1PRNG specifically where possible for NIST SP800-90A compliance.
            // Ignoring Android recommendation to use "new SecureRandom()" because that
            // decision was taken based on a single peer reviewed statistical test that
            // showed SHA1PRNG has bias. The test has not been adopted by NIST yet which
            // already uses 15 other statistical tests for quality assurance. This does
            // not mean the new test is invalid, but it is more appropriate for this work
            // to adopt and comply with an accepted standard for security assurance.
            final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            // Obtain the most secure PRNG claimed by the platform for generating the seed
            // according to Android recommendation.
            final SecureRandom secureRandomForSeed = new SecureRandom();
            // NIST SP800-90A (see section 10.1) recommends 440 bit seed for SHA1PRNG
            // to support security strength defined in NIST SP800-57 Part 1 Revision 5.
            final byte[] seed = secureRandomForSeed.generateSeed(55);
            // Seed secure random with 440 bit seed according to NIST SP800-90A recommendation.
            secureRandom.setSeed(seed); // seed with random number
            // Skip the first 256 - 1280 bytes as mitigation against poor implementations
            // of SecureRandom where the initial values are predictable given the seed
            secureRandom.nextBytes(new byte[256 + secureRandom.nextInt(1024)]);
            return secureRandom;
        } catch (Throwable e) {
            // Android OS may mandate the use of "new SecureRandom()" and forbid the use
            // of a specific provider in the future. Fallback to Android mandated option
            // and log the fact that it is no longer NIST SP800-90A compliant.
            final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.PseudoDeviceAddress");
            logger.fault("NIST SP800-90A compliant SecureRandom initialisation failed, reverting back to SecureRandom", e);
            return getSecureRandom();
        }
    }

}
