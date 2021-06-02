//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype.random;

import androidx.annotation.NonNull;

import java.util.Random;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Non-blocking cryptographically secure pseudo random number generator (CSPRNG) for applications
 * where the system is mostly idle, and the call time of nextBytes() is truly random. The former
 * condition (idle) causes SecureRandom to block due to lack of entropy from operating system
 * events. This is mitigated by managing entropy collection within the application according to
 * use cases. For contact tracing applications, system uptime is truly unpredictable because the
 * attacker cannot know when the app first started, or restarted. Furthermore, the elapsed time
 * between requests for random data (to generate the pseudo device address) is also truly
 * unpredictable because it depends on the state of the currently registered devices in the
 * BLEDatabase, and also the proximity and processing speed of devices in the user environment.
 * Finally, for additional assurance, entropy can also be gathered from the mac addresses of
 * encountered devices, where the majority of, if not all, addresses should have been generated
 * from their own SecureRandom source.
 *
 * As an overview, this CSPRNG is based on:
 * - Random seed derived from entropy gathered from truly random events
 * - Deterministic PRNG offering uniform distribution of random values given the initial seed
 * - Crytographic hash function for separating random values from the random seed
 *
 * The expectation is that it will be possible to identify a random seed that yielded an individual
 * observation, and also a seed that yielded consecutive values using brute force methods. However,
 * this is intentional as the design aims to ensure observations are associated with most candidate
 * seeds (2^61 out of 2^64). This deliberate strategy makes an attack unattractive due to the level
 * of uncertainty. The ability to rapidly find one of the seeds is evidence to show the attacker
 * that little information has been gained for exploitation, as the identified seed is just one of
 * many candidates. Running the process for a long period will show there are multiple seeds that
 * can yield the observations.
 */
public class NonBlockingCSPRNG extends RandomSource {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Datatype.NonBlockingCSPRNG");
    // Enable manual debug mode to write internal data to logger for visual inspection,
    // using explicit flag here to ensure data is not leaked to log by mistake
//    private final static boolean manualDebugMode = true;
    private long getRandomLastCalledAt = System.nanoTime();
    // Using 2048 bits of random data to derive the random seed via a cryptographic hash function
    private final Data randomSeedSourceData = new Data(new byte[256]);
    // Using 2048 bits of random data to derive a long value via a cryptographic hash function
    private final Data nextLongSourceData = new Data(new byte[256]);

    /**
     * Get non-blocking CSPRNG by managing entropy collection
     * @return
     */
    @NonNull
    private synchronized Random getCSPRNG() {
        // Requirement 1 : Truly random seed
        // A PRNG must be initialised with a truly random seed to be cryptographically secure.
        // SecureRandom achieves this by using entropy collected from system events. This causes
        // blocking on idle systems due to lack of entropy. The same is achieved here by using
        // knowledge of the target application to derive truly random data from real-world events.
        // - 1A. Entropy from elapsed time between calls which is determined by state of recently
        //       encountered devices, and the proximity and state of devices in the user environment.
        //       Even if the device is in isolation, the call time is still unpredictable at nano
        //       time scale as the source time keeper is based on an infinite CPU loop that samples
        //       current time at millisecond scale, quantized to 500ms scale.
        final long entropyFromElapsedTime = System.nanoTime() ^ getRandomLastCalledAt;
        getRandomLastCalledAt = System.nanoTime();
        // - 1B. Entropy from system up time which is determined by when the Java virtual machine
        //       was initialised. This is truly random as the app can be started or restarted at
        //       any time. Using Math.random() as proxy, because this static function is backed by
        //       a singleton instance of Random() which is initialised with System.nanoTime() on
        //       system start. The initialisation also applies scrambling to derive the seed.
        final long entropyFromSystemUpTime = Double.doubleToLongBits(Math.random() * 0xFFFFFFFFFFFFFFFFL);
        // - 1C. Entropy from external sources that are likely to have been derived from disparate
        //       SecureRandom instances. Use addEntropy() to incorporate detected BLE MAC addresses.
        final long entropyFromExternalSources = useEntropy();
        // - 1D. Combination of available entropy
        final long entropy = entropyFromElapsedTime ^ entropyFromSystemUpTime ^ entropyFromExternalSources;
//        if (manualDebugMode) {
//            logger.debug("getCSPRNG entropy (elapsed={},upTime={},external={},combined={})", entropyFromElapsedTime, entropyFromSystemUpTime, entropyFromExternalSources, entropy);
//        }
        // Requirement 2 : Uniformly distributed PRNG
        // A PRNG must deliver uniformly distributed random values and a long period length, such
        // that knowledge of prior values offer negligible or no benefit in predicting future values.
        // - 2A. A linear congruential generator (LCG) based PRNG is efficient, non-blocking, and
        //       delivers uniformly distributed random values. Java's random offers a period length
        //       of 2^48. Analysis of the first 559,677,879,539,559 seeds have confirmed the values
        //       are uniformed distributed.
        final Random randomSeedSource = new Random(entropy);
        // - 2B. Cryptographically separate the random seed source from the actual random seed to
        //       be used for generating the one-time use random instance. The first 2048 bits of
        //       random data is hashed by SHA256. The resulting 256-bit hash is truncated to
        //       provide the random seed for initialising the one-time use PRNG. Taking 2048 bits
        //       of random data from a PRNG sequence with period length 2^48 means there are
        //       2^48 / 2048 = 2^48 / 2^11 = 2^37 source seeds that can generate the same output.
        //       This offers a large search space that make an attack impractical given limited
        //       observations.
        randomSeedSource.nextBytes(randomSeedSourceData.value);
        final Data randomSeedSourceDataHash = hash(randomSeedSourceData);
        // - 2C. Truncating cryptographic hash at a random index to derive the seed for the
        //       one-time use PRNG. Index is selected by a random source seeded by truly random
        //       events. Truncating the 256-bit hash down to a 64-bit random seed in 8-bit blocks
        //       means there are 2^256 / 2^64 / 2^8 = 2^24 possible source seeds that can generate
        //       the same random seed. This increases the search space that make an attack
        //       impractical given limited observations.
        final int index = randomSeedSource.nextInt(randomSeedSourceDataHash.value.length - 8);
        //noinspection ConstantConditions
        final long randomSeed = randomSeedSourceDataHash.int64(index).value;
        // - 2D. Create a non-blocking PRNG for one-time use where the seed has been derived from
        //       truly random events, and via a cryptographic hash function to protect the source
        //       seed from compromise. The combination of these two methods mean there are
        //       2^37 * 2^24 = 2^61 possible entropy values that can generate the same random seed.
        //       This is a large search space that make an attack impractical given limited
        //       observations.
//        if (manualDebugMode) {
//            logger.debug("getCSPRNG seed (seed={},index={},hash={})", randomSeed, index, randomSeedSourceDataHash.hexEncodedString());
//        }
        return new Random(randomSeed);
    }

    @Override
    public synchronized void nextBytes(@NonNull final byte[] bytes) {
        getCSPRNG().nextBytes(bytes);
    }

    @Override
    public synchronized int nextInt() {
        // Get one-time use CSPRNG
        final Random random = getCSPRNG();
        // Get 2048 bits of random data from the CSPRNG
        random.nextBytes(nextLongSourceData.value);
        // Truncate random data to derive random long value, using the CSPRNG to select index
        final int index = random.nextInt(nextLongSourceData.value.length - 4);
        //noinspection UnnecessaryLocalVariable,ConstantConditions
        final int randomValue = nextLongSourceData.int32(index).value;
//        if (manualDebugMode) {
//            logger.debug("nextInt (value={},index={},hash={})", randomValue, index, nextLongSourceData.hexEncodedString());
//        }
        return randomValue;
    }

    @Override
    public synchronized long nextLong() {
        // Get one-time use CSPRNG
        final Random random = getCSPRNG();
        // Get 2048 bits of random data from the CSPRNG
        random.nextBytes(nextLongSourceData.value);
        // Truncate random data to derive random long value, using the CSPRNG to select index
        final int index = random.nextInt(nextLongSourceData.value.length - 8);
        //noinspection UnnecessaryLocalVariable,ConstantConditions
        final long randomValue = nextLongSourceData.int64(index).value;
//        if (manualDebugMode) {
//            logger.debug("nextLong (value={},index={},hash={})", randomValue, index, nextLongSourceData.hexEncodedString());
//        }
        return randomValue;
    }

}
