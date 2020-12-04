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
    private final Method method;
    private final boolean newInstancePerCall;
    private Random random = null;
    private boolean secureRandom;
    private boolean nistCompliant;
    private short externalEntropy = 0;
    public enum Method {
        // Singleton random source reused per call
        RANDOM, SECURE_RANDOM, NIST,
        // Random source re-initialised per call
        RANDOM_NEW, SECURE_RANDOM_NEW, NIST_NEW
    }

    public RandomSource(final Method method) {
        this.method = method;
        this.newInstancePerCall = (method == Method.RANDOM_NEW || method == Method.SECURE_RANDOM_NEW || method == Method.NIST_NEW);
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
        if (random == null || newInstancePerCall) {
            switch (method) {
                case RANDOM:
                case RANDOM_NEW:
                    initRandom();
                    break;
                case SECURE_RANDOM:
                case SECURE_RANDOM_NEW:
                    initSecureRandom();
                    break;
                case NIST:
                case NIST_NEW:
                    initSecureRandomNistCompliant();
                    break;
            }
        }
        // Use external entropy to adjust PRNG sequence position
        for (int i=(externalEntropy < 0 ? -externalEntropy : externalEntropy); i-->0;) {
            random.nextBoolean();
        }
        externalEntropy = 0;
    }

    /// Get random instance
    protected void initRandom() {
        this.random = new Random();
        this.secureRandom = false;
        this.nistCompliant = false;
    }

    /// Get secure random instance
    protected void initSecureRandom() {
        try {
            this.random = new SecureRandom();
            this.secureRandom = true;
            this.nistCompliant = false;
        } catch (Throwable e) {
            logger.fault("SecureRandom initialisation failed, reverting back to Random", e);
            initRandom();
        }
    }

    /// Get secure random instance seed according to NIST SP800-90A recommendations
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
    protected void initSecureRandomNistCompliant() {
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
            secureRandom.nextBytes(new byte[256 + secureRandomForSeed.nextInt(1024)]);
            this.random = secureRandom;
            this.secureRandom = true;
            this.nistCompliant = true;
        } catch (Throwable e) {
            // Android OS may mandate the use of "new SecureRandom()" and forbid the use
            // of a specific provider in the future. Fallback to Android mandated option
            // and log the fact that it is no longer NIST SP800-90A compliant.
            logger.fault("NIST SP800-90A compliant SecureRandom initialisation failed, reverting back to SecureRandom", e);
            initSecureRandom();
        }
    }
}
