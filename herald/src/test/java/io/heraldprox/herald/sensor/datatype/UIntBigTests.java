//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Random;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UIntBigTests {

    @Test
    public void longValue() {
        assertEquals(0, new UIntBig(0).uint64());
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            assertEquals(i, new UIntBig(i).uint64());
        }
    }

    // MARK: -

    @Test
    public void testIsZero() {
        assertTrue(new UIntBig(0).isZero());
        assertFalse(new UIntBig(1).isZero());
        assertFalse(new UIntBig(256).isZero());
        assertFalse(new UIntBig(Long.MAX_VALUE).isZero());
    }

    @Test
    public void testIsOne() {
        assertFalse(new UIntBig(0).isOne());
        assertTrue(new UIntBig(1).isOne());
        assertFalse(new UIntBig(257).isOne());
        assertFalse(new UIntBig(Long.MAX_VALUE).isOne());
    }

    @Test
    public void testIsOdd() {
        assertFalse(new UIntBig(0).isOdd());
        assertTrue(new UIntBig(1).isOdd());
        assertFalse(new UIntBig(512).isOdd());
        assertTrue(new UIntBig(513).isOdd());
        assertFalse(new UIntBig(Long.MAX_VALUE - 1).isOdd());
        assertTrue(new UIntBig(Long.MAX_VALUE).isOdd());
    }

    // MARK: - Right shift by one

    @Test
    public void testRightShiftByOne() {
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            final UIntBig a = new UIntBig(i);
            a.rightShiftByOne();
            final long b = a.uint64();
            final long c = i >>> 1;
            assertEquals(c, b);
        }
    }

    // MARK: - Times

    @Test
    public void testTimes() {
        // a * 0
        for (long i=1; i<Integer.MAX_VALUE && i>0; i*=3) {
            final UIntBig a = new UIntBig(i);
            final UIntBig b = new UIntBig(0);
            a.times(b);
            assertEquals(0, a.uint64());
        }
        // 0 * b
        for (long i=1; i<Integer.MAX_VALUE && i>0; i*=3) {
            final UIntBig a = new UIntBig(0);
            final UIntBig b = new UIntBig(i);
            a.times(b);
            assertEquals(0, a.uint64());
        }
        // a * b
        for (long i=1; i<Integer.MAX_VALUE && i>0; i*=3) {
            for (long j=1; j<Integer.MAX_VALUE && j>0; j*=3) {
                final UIntBig a = new UIntBig(i);
                final UIntBig b = new UIntBig(j);
                a.times(b);
                assertEquals(i*j, a.uint64());
            }
        }
    }

    // MARK: - Minus

    @Test
    public void testMinus() {
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            for (long j=1; j<=i && j>0; j*=7) {
                for (short k=1; j*k<=i && k>0; k*=11) {
                    final UIntBig a = new UIntBig(i);
                    final UIntBig b = new UIntBig(j);
                    //noinspection UnnecessaryLocalVariable
                    final short multiplier = k;
                    a.minus(b, multiplier, 0);
                    assertEquals(i-j*k, a.uint64());
                }
            }
        }
    }

    @Test
    public void testMinusOffset() {
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            for (long j=1; j<=i && j>0; j*=7) {
                for (short k=1; (j<<16)*k<=i && k>0; k*=11) {
                    final UIntBig a = new UIntBig(i);
                    final UIntBig b = new UIntBig(j);
                    if (a.magnitude().length >= b.magnitude().length + 1) {
                        //noinspection UnnecessaryLocalVariable
                        final short multiplier = k;
                        a.minus(b, multiplier, 1);
                        assertEquals(i - (j << 16) * k, a.uint64());
                    }
                }
            }
        }
    }

    // MARK: - Compare

    @Test
    public void testCompare() {
        assertEquals(-1, new UIntBig(0).compare(new UIntBig(1)));
        assertEquals(0, new UIntBig(0).compare(new UIntBig(0)));
        assertEquals(1, new UIntBig(1).compare(new UIntBig(0)));
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            final UIntBig a = new UIntBig(i);
            for (long j = 1; j < Long.MAX_VALUE && j > 0; j *= 3) {
                final UIntBig b = new UIntBig(j);
                assertEquals(Long.compare(i, j), UIntBig.compare(a.magnitude(), b.magnitude()));
                assertEquals(Long.compare(i, j), a.compare(b));

                assertEquals(i == j, a.equals(b));
                assertEquals(i == j, a.hashCode() == b.hashCode());
            }
        }
    }

    // MARK: - Mod

    @Test
    public void testMod() {
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            for (long j = 1; j < Long.MAX_VALUE && j>0; j*=7) {
                final UIntBig a = new UIntBig(i);
                final UIntBig b = new UIntBig(j);
                a.mod(b);
                assertEquals(i % j, a.uint64());
            }
        }
    }

    // MARK: - ModPow

    @Test
    public void testModPow() {
        for (long i=1; i<Long.MAX_VALUE && i>0; i *= 11) {
            for (long j = 1; j < Long.MAX_VALUE && j > 0; j *= 7) {
                for (long k = 1; k < Long.MAX_VALUE && k > 0; k *= 3) {
                    final long ex = new BigInteger(Long.toString(i)).modPow(new BigInteger(Long.toString(j)), new BigInteger(Long.toString(k))).longValue();
                    final UIntBig b = new UIntBig(i);
                    final UIntBig m = b.modPow(new UIntBig(j), new UIntBig(k));
                    final long ac = m.uint64();
                    assertEquals(ex, ac);
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testModPowPerformance() {
        final Random random = new Random();
        final long samples = Short.MAX_VALUE;
        long t0 = 0, t1 = 0;
        for (long x=0; x<samples; x++) {
            final long i=random.nextLong();
            final long j=random.nextLong();
            final long k=random.nextLong();
            if (i >0 && j>0 & k>0) {
                final BigInteger a0 = new BigInteger(Long.toString(i));
                final BigInteger b0 = new BigInteger(Long.toString(j));
                final BigInteger c0 = new BigInteger(Long.toString(k));
                final long tS0 = System.nanoTime();
                final BigInteger d0 = a0.modPow(b0, c0);
                final long tE0 = System.nanoTime();
                final long e0 = d0.longValue();

                final UIntBig a1 = new UIntBig(i);
                final UIntBig b1 = new UIntBig(j);
                final UIntBig c1 = new UIntBig(k);
                final long tS1 = System.nanoTime();
                final UIntBig m = a1.modPow(b1, c1);
                final long tE1 = System.nanoTime();
                final long e1 = m.uint64();
                assertEquals(e0, e1);
                t0 += (tE0 - tS0);
                t1 += (tE1 - tS1);
                if (x > 0 && (x % samples == 0)) {
                    System.err.println("sample=" + x + ",hardware=" + (t0 / x) + "ns/call,UIntBig=" + (t1 / x) + "ns/call");
                }
            }
        }
        System.err.println("sample=" + samples + ",hardware=" + (t0 / samples) + "ns/call,UIntBig=" + (t1 / samples) + "ns/call");
    }

    // MARK: - Hex

    @Test
    public void testHexEncodedString() {
        assertEquals(0, new UIntBig("").uint64());
        assertEquals(0, new UIntBig("0").uint64());
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            final String hex = Long.toHexString(i);
            final UIntBig a = new UIntBig(hex);
            final String hexEncodedString = (hex.length() % 2 == 1 ? "0" : "") + hex.toUpperCase();
            assertEquals(i, a.uint64());
            assertEquals(hexEncodedString, a.hexEncodedString());
        }
    }

    // MARK: - Bit length

    @Test
    public void testBitLength() {
        assertEquals(0, new UIntBig(0).bitLength());
        for (long i=1; i<Long.MAX_VALUE && i>0; i*=3) {
            final BigInteger a = BigInteger.valueOf(i);
            final UIntBig b = new UIntBig(i);
            assertEquals(a.toString(2).length(), b.bitLength());
        }
    }

    private final static class MockRandom extends RandomSource {
        public MockRandom() {
        }
        @Override
        public void nextBytes(@NonNull byte[] bytes) {
            for (int i=bytes.length; i-->0;) {
                bytes[i] = (byte) 0xFF;
            }
        }
    }

    @Test
    public void testRandom() {
        final RandomSource random = new MockRandom();
        for (int i=0; i<Short.MAX_VALUE; i++) {
            final UIntBig a = new UIntBig(i, random);
            assertEquals(i, a.bitLength());
        }
    }

    // MARK: - Cross-platform

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uintBig.csv");
        out.println("value,data");
        long i = 1;
        while (i <= (Long.MAX_VALUE / 7)) {
            final Data data = new Data();
            data.append(new UIntBig(i));
            System.err.println(i + "," + new UIntBig(i).hexEncodedString() + "," + data.uintBig(0).hexEncodedString());
            assertEquals(new UIntBig(i), data.uintBig(0));
            out.println(i + "," + data.base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("uintBig.csv");
    }

    @Test
    public void testCrossPlatformModPow() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("uintBigModPow.csv");
        out.println("a,b,c,d");
        for (long i=1; i<0x000FFFFFFFFFFFFFL; i *= 11) {
            for (long j = 1; j < 0x000FFFFFFFFFFFFFL; j *= 7) {
                for (long k = 1; k < 0x000FFFFFFFFFFFFFL; k *= 3) {
                    final UIntBig a = new UIntBig(i);
                    final UIntBig b = new UIntBig(j);
                    final UIntBig c = new UIntBig(k);
                    final UIntBig d = a.modPow(b,c);
                    final Data data = new Data();
                    data.append(d);
                    out.println(i+","+j+","+k+","+data.base64EncodedString());
                }
            }
        }
        out.flush();
        out.close();
        // Pending iOS implementation
        TestUtil.assertEqualsCrossPlatform("uintBigModPow.csv");
    }
}
