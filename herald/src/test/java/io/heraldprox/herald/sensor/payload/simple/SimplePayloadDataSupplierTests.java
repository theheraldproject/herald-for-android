//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.Float16;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.TimeInterval;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;

import org.junit.Test;

import java.io.PrintWriter;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("ConstantConditions")
public class SimplePayloadDataSupplierTests {

    @Test
    public void testTimeIntervalSince1970() {
        // Same string date, same date
        assertEquals(new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value - new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value, 0);
        // Parsing second in string date
        assertEquals(new TimeInterval(K.date("2020-09-24T00:00:01+0000")).value - new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value, 1);
        // Parsing minute in string date
        assertEquals(new TimeInterval(K.date("2020-09-24T00:01:00+0000")).value - new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value, 60);
        // Parsing hour in string date
        assertEquals(new TimeInterval(K.date("2020-09-24T01:00:00+0000")).value - new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value, 60 * 60);
        // Parsing day in string date
        assertEquals(new TimeInterval(K.date("2020-09-25T00:00:00+0000")).value - new TimeInterval(K.date("2020-09-24T00:00:00+0000")).value, 24 * 60 * 60);
    }

    @Test
    public void testDay() {
        // Day before epoch
        assertEquals(K.day(K.date("2020-09-23T00:00:00+0000")), -1);
        // Epoch day
        assertEquals(K.day(K.date("2020-09-24T00:00:00+0000")), 0);
        assertEquals(K.day(K.date("2020-09-24T00:00:01+0000")), 0);
        assertEquals(K.day(K.date("2020-09-24T23:59:59+0000")), 0);
        // Day after epoch
        assertEquals(K.day(K.date("2020-09-25T00:00:00+0000")), 1);
        assertEquals(K.day(K.date("2020-09-25T00:00:01+0000")), 1);
        assertEquals(K.day(K.date("2020-09-25T23:59:59+0000")), 1);
        // 2 days after epoch
        assertEquals(K.day(K.date("2020-09-26T00:00:00+0000")), 2);
    }

    @Test
    public void testPeriod() {
        // Period starts at midnight
        assertEquals(K.period(K.date("2020-09-24T00:00:00+0000")), 0);
        assertEquals(K.period(K.date("2020-09-24T00:00:01+0000")), 0);
        assertEquals(K.period(K.date("2020-09-24T00:05:59+0000")), 0);
        // A peroid is 6 minutes long
        assertEquals(K.period(K.date("2020-09-24T00:06:00+0000")), 1);
        assertEquals(K.period(K.date("2020-09-24T00:06:01+0000")), 1);
        assertEquals(K.period(K.date("2020-09-24T00:11:59+0000")), 1);
        // Last period of the day
        assertEquals(K.period(K.date("2020-09-24T23:54:00+0000")), 239);
        assertEquals(K.period(K.date("2020-09-24T23:54:01+0000")), 239);
        assertEquals(K.period(K.date("2020-09-24T23:59:59+0000")), 239);
        // Should never happen but still valid and correct
        assertEquals(K.period(K.date("2020-09-24T24:00:00+0000")), 0);
        assertEquals(K.period(K.date("2020-09-24T24:06:00+0000")), 1);
    }

    @Test
    public void testSecretKey() {
        // Secret keys are different every time
        for (int i=0; i<1000; i++) {
            final SecretKey k1 = K.secretKey();
            final SecretKey k2 = K.secretKey();
            assertNotNull(k1);
            assertNotNull(k2);
            assertEquals(k1.value.length, 2048);
            assertEquals(k2.value.length, 2048);
            assertFalse(Arrays.equals(k1.value, k2.value));
        }
    }

    @Test
    public void testMatchingKeys() {
        // Generate same secret keys (ks1, ks2) and a different secret key (ks3)
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final SecretKey ks2 = new SecretKey((byte) 0, 2048);
        final SecretKey ks3 = new SecretKey((byte) 1, 2048);
        assertEquals(ks1, ks2);
        assertNotEquals(ks1, ks3);
        assertNotEquals(ks2, ks3);

        // Same secret key should generate the same matching key seed on the same day,
        // different secret keys should generate different seeds on the same day
        final MatchingKeySeed kms1 = K.matchingKeySeed(ks1, 0);
        final MatchingKeySeed kms2 = K.matchingKeySeed(ks2, 0);
        final MatchingKeySeed kms3 = K.matchingKeySeed(ks3, 0);
        assertEquals(kms1, kms2);
        assertNotEquals(kms1, kms3);
        assertNotEquals(kms2, kms3);

        // Same matching key seed should generate the same matching key,
        // different matching key seeds should generate different keys on the same day
        final MatchingKey km1 = K.matchingKey(kms1);
        final MatchingKey km2 = K.matchingKey(kms2);
        final MatchingKey km3 = K.matchingKey(kms3);
        assertEquals(km1, km2);
        assertNotEquals(km1, km3);
        assertNotEquals(km2, km3);

        // Matching key is 32 bytes
        assertEquals(km1.value.length, 32);
        assertEquals(km2.value.length, 32);
        assertEquals(km3.value.length, 32);
    }

    @Test
    public void testContactKeys() {
        // Generate same matching keys (km1, km2) and a different matching key (km3)
        final MatchingKey km1 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 0, 2048), 0));
        final MatchingKey km2 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 0, 2048), 0));
        final MatchingKey km3 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 1, 2048), 0));

        // Same matching key should generate the same contact key seed from the same period,
        // different matching keys should generate different seeds for the same period
        final ContactKeySeed kcs1 = K.contactKeySeed(km1, 0);
        final ContactKeySeed kcs2 = K.contactKeySeed(km2, 0);
        final ContactKeySeed kcs3 = K.contactKeySeed(km3, 0);
        assertEquals(kcs1, kcs2);
        assertNotEquals(kcs1, kcs3);
        assertNotEquals(kcs2, kcs3);

        // Same contact key seed should generate the same contact key,
        // different contact key seeds should generate different keys
        final ContactKey kc1 = K.contactKey(kcs1);
        final ContactKey kc2 = K.contactKey(kcs2);
        final ContactKey kc3 = K.contactKey(kcs3);
        assertEquals(kc1, kc2);
        assertNotEquals(kc1, kc3);
        assertNotEquals(kc2, kc3);

        // Contact key is 32 bytes
        assertEquals(kc1.value.length, 32);
        assertEquals(kc2.value.length, 32);
        assertEquals(kc3.value.length, 32);

        // Contact key changes throughout the day
        for (int i=0; i<=239; i++) {
            final ContactKey kc1i = K.contactKey(K.contactKeySeed(km1, i));
            for (int j=(i+1); j<=240; j++) {
                final ContactKey kc1j = K.contactKey(K.contactKeySeed(km1, j));
                assertNotEquals(kc1i, kc1j);
            }
        }
    }

    @Test
    public void testContactIdentifier() {
        // Generate same matching keys (km1, km2) and a different matching key (km3)
        final MatchingKey km1 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 0, 2048), 0));
        final MatchingKey km2 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 0, 2048), 0));
        final MatchingKey km3 = K.matchingKey(K.matchingKeySeed(new SecretKey((byte) 1, 2048), 0));

        // Generate same contact keys (kc1, kc2) and a different contact key (kc3)
        final ContactKey kc1 = K.contactKey(K.contactKeySeed(km1, 0));
        final ContactKey kc2 = K.contactKey(K.contactKeySeed(km2, 0));
        final ContactKey kc3 = K.contactKey(K.contactKeySeed(km3, 0));

        // Same contact key should generate the same contact identifier,
        // different contact keys should generate different identifiers
        final ContactIdentifier Ic1 = K.contactIdentifier(kc1);
        final ContactIdentifier Ic2 = K.contactIdentifier(kc2);
        final ContactIdentifier Ic3 = K.contactIdentifier(kc3);
        assertEquals(Ic1, Ic2);
        assertNotEquals(Ic1, Ic3);
        assertNotEquals(Ic2, Ic3);

        // Contact identifier is 16 bytes
        assertEquals(Ic1.value.length, 16);
        assertEquals(Ic2.value.length, 16);
        assertEquals(Ic3.value.length, 16);
    }

    @Test
    public void testPayload() {
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final SimplePayloadDataSupplier pds1 = new ConcreteSimplePayloadDataSupplier(new UInt8(0), new UInt16(0), new UInt16(0), ks1);

        // Payload is 23 bytes long
        assertNotNull(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null).value.length, ConcreteSimplePayloadDataSupplier.payloadLength);

        // Same payload in same period
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:05:59+0000")), null));
        // Different payloads in different periods
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:06:00+0000")), null));

        // Same payload in different periods before epoch
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:06:00+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T23:54:00+0000")), null));
        // Valid payload on first epoch period
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T23:54:01+0000")), null));

        // Same payload in same periods on epoch + 2000 days
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:05:59+0000")), null));
        // Different payloads in different periods on epoch + 2000 days
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:06:00+0000")), null));

        // Same payload in different periods after epoch + 2001 days
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:06:00+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:05:59+0000")), null));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000")), null), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:06:00+0000")), null));
    }

    @Test
    public void testContactIdentifierPerformance() {
        final MatchingKey km1 = new MatchingKey((byte) 0, 32);
        final long t0 = System.currentTimeMillis();
        for (int i=0; i<1000; i++) {
            K.forEachContactIdentifier(km1, new K.ForEachContactIdentifierAction() {
                @Override
                public void consume(@NonNull ContactIdentifier contactIdentifier, int period) {
                    // Ignored
                }
            });
        }
        final long t1 = System.currentTimeMillis();
    }

    @Test
    public void testCrossPlatformFloat16() {
        System.out.println("value,float16");
        System.out.println("-65504," + new Float16(-65504).bigEndian.base64EncodedString());
        System.out.println("-0.0000000596046," + new Float16(-0.0000000596046f).bigEndian.base64EncodedString());
        System.out.println("0," + new Float16(0).bigEndian.base64EncodedString());
        System.out.println("0.0000000596046," + new Float16(0.0000000596046f).bigEndian.base64EncodedString());
        System.out.println("65504," + new Float16(65504).bigEndian.base64EncodedString());
    }

    @Test
    public void testContactIdentifierCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("contactIdentifier.csv");
        out.println("day,period,matchingKeySeed,matchingKey,contactKeySeed,contactKey,contactIdentifier");
        // Generate secret and matching keys
        final SecretKey secretKey = new SecretKey((byte) 0, 2048);
        // Print first 10 days of contact keys for comparison across iOS and Android implementations
        for (int day=0; day<=10; day++) {
            final MatchingKeySeed matchingKeySeed = K.matchingKeySeed(secretKey, day);
            final MatchingKey matchingKey = K.matchingKey(matchingKeySeed);
            for (int period=0; period<=240; period++) {
                final ContactKeySeed contactKeySeed = K.contactKeySeed(matchingKey, period);
                final ContactKey contactKey = K.contactKey(contactKeySeed);
                final ContactIdentifier contactIdentifier = K.contactIdentifier(contactKey);
                out.println(day + "," + period + "," + matchingKeySeed.base64EncodedString() + "," + matchingKey.base64EncodedString() + "," + contactKeySeed.base64EncodedString() + "," + contactKey.base64EncodedString() + "," + contactIdentifier.base64EncodedString());
            }
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("contactIdentifier.csv");
    }
}
