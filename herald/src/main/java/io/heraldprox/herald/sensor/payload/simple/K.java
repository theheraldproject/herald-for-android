//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.TimeInterval;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Key generation functions for the Simple Payload.
 */
@SuppressWarnings("ConstantConditions")
public class K {
    // Secret key length
    private final static int secretKeyLength = 2048;
    // Days supported by key derivation function
    private final static int days = 2000;
    // Periods per day
    private final static int periods = 240;
    // Epoch as time interval since 1970
    private final static TimeInterval epoch = K.getEpoch();

    /**
     * Date from string date "yyyy-MM-dd'T'HH:mm:ssXXXX" in UTC
     * 
     * @param fromString Time string in UTC timezone
     * @return Herald Date instance
     */
    @Nullable
    protected static Date date(@NonNull final String fromString) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);
        // Only ever used in tests. K class only uses UTC.
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(fromString);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Epoch for calculating days and periods
     * @return
     */
    @NonNull
    protected static TimeInterval getEpoch() {
        final Date date = date("2020-09-24T00:00:00+0000");
        //noinspection ConstantConditions
        return new TimeInterval(date.getTime() / 1000);
    }

    /**
     * Epoch day for selecting matching key
     * @param onDate
     * @return
     */
    protected static int day(@NonNull final Date onDate) {
        return (int) ((new TimeInterval(onDate).value - epoch.value) / 86400);
    }

    /**
     * Epoch day period for selecting contact key
     * @param atTime
     * @return
     */
    protected static int period(@NonNull final Date atTime) {
        final int second = (int) ((new TimeInterval(atTime).value - epoch.value) % 86400);
        return second / (86400 / periods);
    }

    /**
     * Generate 2048-bit secret key, K_s
     * @return
     */
    @NonNull
    protected static SecretKey secretKey() {
        final SecureRandom secureRandom = getSecureRandom();
        final byte[] bytes = new byte[secretKeyLength];
        secureRandom.nextBytes(bytes);
        return new SecretKey(bytes);
    }

    @NonNull
    public static SecureRandom getSecureRandom() {
        // TODO : Switch to NonBlockingSecureRandom
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.Simple.K");
        try {
            // Retrieve a SHA1PRNG
            final SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            // Generate a secure seed
            final SecureRandom seedSr = new SecureRandom();
            // We need a 440 bit seed - see NIST SP800-90A
            final byte[] seed = seedSr.generateSeed(55);
            sr.setSeed(seed); // seed with random number
            // Securely generate bytes
            sr.nextBytes(new byte[256 + sr.nextInt(1024)]); // start from random position
            return sr;
        } catch (Throwable e) {
            logger.fault("Could not retrieve SHA1PRNG SecureRandom instance", e);
            return new SecureRandom();
        }
    }

    /**
     * Generate matching keys K_{m}^{0...days}
     *
     * @param secretKey The SecretKey to use to generate the sequence of daily MatchingKeys
     * @return
     */
    @NonNull
    protected static MatchingKey[] matchingKeys(@NonNull final SecretKey secretKey) {
        final int n = days;
        final MatchingKeySeed[] matchingKeySeed = new MatchingKeySeed[n + 1];
        matchingKeySeed[n] = new MatchingKeySeed(F.hash(secretKey));
        for (int i=n; i-->0;) {
            matchingKeySeed[i] = new MatchingKeySeed(F.hash(F.truncate(matchingKeySeed[i + 1])));
        }
        final MatchingKey[] matchingKey = new MatchingKey[n + 1];
        for (int i=1; i<=n; i++) {
            matchingKey[i] = new MatchingKey(F.hash(F.xor(matchingKeySeed[i], matchingKeySeed[i - 1])));
        }
        final MatchingKeySeed matchingKeySeedMinusOne = new MatchingKeySeed(F.hash(F.truncate(matchingKeySeed[0])));
        matchingKey[0] = new MatchingKey(F.hash(F.xor(matchingKeySeed[0], matchingKeySeedMinusOne)));
        return matchingKey;
    }

    /**
     * Generate contact keys K_{c}^{0...periods}
     *
     * @param matchingKey The daily MatchingKey to use to generate a set of period ContactKeys
     * @return The ContactKey set generated from the MatchingKey
     */
    @NonNull
    protected static ContactKey[] contactKeys(@NonNull final MatchingKey matchingKey) {
        final int n = periods;

        final ContactKeySeed[] contactKeySeed = new ContactKeySeed[n + 1];
        contactKeySeed[n] = new ContactKeySeed(F.hash(matchingKey));
        for (int j=n; j-->0;) {
            contactKeySeed[j] = new ContactKeySeed(F.hash(F.truncate(contactKeySeed[j + 1])));
        }
        final ContactKey[] contactKey = new ContactKey[n + 1];
        for (int j=1; j<=n; j++) {
            contactKey[j] = new ContactKey(F.hash(F.xor(contactKeySeed[j], contactKeySeed[j - 1])));
        }
        final ContactKeySeed contactKeySeedMinusOne = new ContactKeySeed(F.hash(F.truncate(contactKeySeed[0])));
        contactKey[0] = new ContactKey(F.hash(F.xor(contactKeySeed[0], contactKeySeedMinusOne)));
        return contactKey;
    }

    /**
     * Generate contact identifer I_{c}
     *
     * @param contactKey The ContactKey used to generate this period's ContactIdentifier
     */
    @NonNull
    protected static ContactIdentifier contactIdentifier(@NonNull final ContactKey contactKey) {
        return new ContactIdentifier(F.truncate(contactKey, 16));
    }
}
