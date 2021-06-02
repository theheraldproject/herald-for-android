//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;

import java.security.SecureRandom;
import java.util.Random;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;

/**
 * Secure random number generator that is blocking after about 6.0 hours
 *  on idle devices due to lack of entropy.
 *  SecureRandom seeded according to NIST SP800-90A recommendations
 *  - SHA1PRNG algorithm
 *  - Algorithm seeded with 440 bits of secure random data
 *  - Skips first random number of bytes to mitigate against poor implementations
 *  Compliance to NIST SP800-90A offers quality assurance against an accepted
 *  standard. The aim here is not to offer the most perfect random source, but
 *  a source with well defined and understood characteristics, thus enabling
 *  selection of the most appropropriate method, given the intented purpose.
 *  This implementation supports security strength for NIST SP800-57
 *  Part 1 Revision 5 (informally, generation of cryptographic keys for
 *  encryption of sensitive data).
 **/
public class BlockingSecureRandomNIST extends BlockingSecureRandom {

    @NonNull
    private Random getSecureRandomNIST() {
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
            final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.Random.BlockingSecureRandomNIST");
            logger.fault("NIST SP800-90A compliant SecureRandom initialisation failed, reverting back to SecureRandom", e);
            return super.getSecureRandom();
        }
    }

    @Override
    public void nextBytes(@NonNull final byte[] bytes) {
        getSecureRandomNIST().nextBytes(bytes);
    }
}
