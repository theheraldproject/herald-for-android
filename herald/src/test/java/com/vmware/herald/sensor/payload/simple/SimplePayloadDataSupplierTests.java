//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.datatype.Float16;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.TimeInterval;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.datatype.UInt8;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        // Generate same secret keys
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final SecretKey ks2 = new SecretKey((byte) 0, 2048);
        // Generate a different secret key
        final SecretKey ks3 = new SecretKey((byte) 1, 2048);

        // Generate matching keys based on the same secret key
        final MatchingKey[] km1 = K.matchingKeys(ks1);
        final MatchingKey[] km2 = K.matchingKeys(ks2);
        // Generate matching keys based on a different secret key
        final MatchingKey[] km3 = K.matchingKeys(ks3);

        // 2001 matching keys in total (key 2000 is not used)
        assertEquals(km1.length, 2001);
        assertEquals(km2.length, 2001);
        assertEquals(km3.length, 2001);

        // Matching key is 32 bytes
        assertEquals(km1[0].value.length, 32);

        // Same secret key for same matching keys
        assertTrue(Arrays.deepEquals(km1, km2));
        // Different secret keys for different matching keys
        assertFalse(Arrays.deepEquals(km1, km3));
        assertFalse(Arrays.deepEquals(km2, km3));
    }

    @Test
    public void testContactKeys() {
        // Generate secret and matching keys
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final MatchingKey[] km1 = K.matchingKeys(ks1);

        // Generate contact keys based on the same matching key
        final ContactKey[] kc1 = K.contactKeys(km1[0]);
        final ContactKey[] kc2 = K.contactKeys(km1[0]);
        // Generate contact keys based on a different matching key
        final ContactKey[] kc3 = K.contactKeys(km1[1]);

        // 241 contact keys per day (key 241 is not used)
        assertEquals(kc1.length, 241);
        assertEquals(kc2.length, 241);
        assertEquals(kc3.length, 241);

        // Contact key is 32 bytes
        assertEquals(kc1[0].value.length, 32);

        // Same contact keys for same matching key
        assertTrue(Arrays.deepEquals(kc1, kc2));
        // Different contact keys for different matching keys
        assertFalse(Arrays.deepEquals(kc1, kc3));
        assertFalse(Arrays.deepEquals(kc2, kc3));

        // Contact key changes throughout the day
        for (int i=0; i<239; i++) {
            for (int j=(i+1); j<240; j++) {
                assertFalse(Arrays.equals(kc1[i].value, kc1[j].value));
                assertFalse(Arrays.equals(kc2[i].value, kc2[j].value));
                assertFalse(Arrays.equals(kc3[i].value, kc3[j].value));
            }
        }
    }

    @Test
    public void testContactIdentifier() {
        // Generate secret and matching keys
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final MatchingKey[] km1 = K.matchingKeys(ks1);

        // Generate contact keys based on the same matching key
        final ContactKey[] kc1 = K.contactKeys(km1[0]);

        // Generate contact identifier based on contact key
        final ContactIdentifier Ic1 = K.contactIdentifier(kc1[0]);
        final ContactIdentifier Ic2 = K.contactIdentifier(kc1[0]);
        final ContactIdentifier Ic3 = K.contactIdentifier(kc1[1]);

        // Contact identifier is 16 bytes
        assertEquals(Ic1.value.length, 16);

        // Same contact identifier for same contact key
        assertEquals(Ic1, Ic2);
        assertNotEquals(Ic2, Ic3);
    }

    @Test
    public void testPayload() {
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final SimplePayloadDataSupplier pds1 = new ConcreteSimplePayloadDataSupplier(new UInt8(0), new UInt16(0), new UInt16(0), ks1);

        // Payload is 23 bytes long
        assertNotNull(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))).value.length, 23);

        // Same payload in same period
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:05:59+0000"))));
        // Different payloads in different periods
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-24T00:06:00+0000"))));

        // Same payload in different periods before epoch
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:06:00+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T23:54:00+0000"))));
        // Valid payload on first epoch period
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2020-09-23T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2020-09-23T23:54:01+0000"))));

        // Same payload in same periods on epoch + 2000 days
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:05:59+0000"))));
        // Different payloads in different periods on epoch + 2000 days
        assertNotEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-17T00:06:00+0000"))));

        // Same payload in different periods after epoch + 2001 days
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:06:00+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:05:59+0000"))));
        assertEquals(pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:00:00+0000"))), pds1.payload(new PayloadTimestamp(K.date("2026-03-18T00:06:00+0000"))));
    }

    @Test
    public void testContactIdentifierPerformance() {
        final MatchingKey km1 = new MatchingKey((byte) 0, 32);
        final long t0 = System.currentTimeMillis();
        for (int i=0; i<1000; i++) {
            ConcreteSimplePayloadDataSupplier.contactIdentifiers(km1);
        }
        final long t1 = System.currentTimeMillis();
    }

    @Test
    public void testCrossPlatformUInt8() {
        System.out.println("value,uint8");
        for (int i=0; i<256; i++) {
            System.out.println(i + "," + new UInt8(i).bigEndian.base64EncodedString());
        }
    }

    @Test
    public void testCrossPlatformUInt16() {
        System.out.println("value,uint16");
        for (int i=0; i<128; i++) {
            System.out.println(i + "," + new UInt16(i).bigEndian.base64EncodedString());
        }
        for (int i=65536-128; i<65536; i++) {
            System.out.println(i + "," + new UInt16(i).bigEndian.base64EncodedString());
        }
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
    public void testContactIdentifierCrossPlatform() {
        System.out.println("day,period,matchingKey,contactKey,contactIdentifier");
        // Generate secret and matching keys
        final SecretKey ks1 = new SecretKey((byte) 0, 2048);
        final MatchingKey[] km1 = K.matchingKeys(ks1);
        // Print first 10 days of contact keys for comparison across iOS and Android implementations
        for (int day=0; day<=10; day++) {
            final ContactKey[] kc1 = K.contactKeys(km1[day]);
            for (int period=0; period<=240; period++) {
                final ContactIdentifier Ic1 = K.contactIdentifier(kc1[period]);
                System.out.println(day + "," + period + "," + km1[day].base64EncodedString() + "," + kc1[period].base64EncodedString() + "," + Ic1.base64EncodedString());
            }
        }
    }
}
