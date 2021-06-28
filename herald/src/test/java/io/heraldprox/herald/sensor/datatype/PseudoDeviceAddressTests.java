//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import io.heraldprox.herald.sensor.TestUtil;
import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandom;
import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandomNIST;
import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandomSingleton;
import io.heraldprox.herald.sensor.datatype.random.NonBlockingPRNG;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

import org.junit.Test;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PseudoDeviceAddressTests {

    @Test
    public void testRandom() {
        final List<RandomSource> randomSources = Arrays.asList(
                new NonBlockingPRNG(),
                new BlockingSecureRandom(),
                new BlockingSecureRandomSingleton(),
                new BlockingSecureRandomNIST());
        for (RandomSource randomSource : randomSources) {
            int duplicateAddress = 0;
            int duplicateBytes = 0;
            long lastAddress = 0;
            byte[] lastBytes = new byte[6];
            for (int i = 0; i < 1000; i++) {
                final PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress(randomSource);
                // Address should be different every time
                if (lastAddress == pseudoDeviceAddress.address) {
                    duplicateAddress++;
                }
                // Bytes should be different most of the time
                assertEquals(6, pseudoDeviceAddress.data.length);
                for (int j=0; j<6; j++) {
                    if (pseudoDeviceAddress.data[j] == lastBytes[j]) {
                        duplicateBytes++;
                    }
                }
                lastAddress = pseudoDeviceAddress.address;
                lastBytes = pseudoDeviceAddress.data;
            }
            // Tolerate 20% duplicate addresses for testing, but expect zero most of the time
            assertTrue(duplicateAddress < (1000 / 20));
            // Tolerate 20% duplicate bytes for testing, but expect close to zero most of the time
            assertTrue(duplicateBytes < (1000 * 6 / 20));
        }
    }

    @Test
    public void testEncodeDecode() {
        // Test encoding and decoding to ensure same data means same address
        for (long i=1; i>0; i*=7) {
            // Test positive
            final PseudoDeviceAddress expectedPositive = new PseudoDeviceAddress(i);
            final PseudoDeviceAddress expectedNegative = new PseudoDeviceAddress(-i);
            final PseudoDeviceAddress actualPositive = new PseudoDeviceAddress(expectedPositive.data);
            final PseudoDeviceAddress actualNegative = new PseudoDeviceAddress(expectedNegative.data);
            assertEquals(expectedPositive.address, actualPositive.address);
            assertEquals(expectedNegative.address, actualNegative.address);
            assertEquals(expectedPositive, actualPositive);
            assertEquals(expectedNegative, actualNegative);
            assertEquals(expectedPositive.hashCode(), actualPositive.hashCode());
            assertEquals(expectedNegative.hashCode(), actualNegative.hashCode());
            assertEquals(expectedPositive.toString(), actualPositive.toString());
            assertEquals(expectedNegative.toString(), actualNegative.toString());
        }
    }

    @Test
    public void testVisualCheck() {
        // Visual check for randomness and byte fill
        for (int i=0; i<10; i++) {
            final PseudoDeviceAddress address = new PseudoDeviceAddress();
            System.err.println(Arrays.toString(address.data));
        }
    }

    @Test
    public void testPerformance() {
        final List<RandomSource> randomSources = Arrays.asList(
                new NonBlockingPRNG(),
                new BlockingSecureRandom(),
                new BlockingSecureRandomSingleton(),
                new BlockingSecureRandomNIST());
        for (RandomSource randomSource : randomSources) {
            final Distribution distribution = new Distribution();
            long t0, t1;
            for (int i = 100000; i-- > 0; ) {
                t0 = System.nanoTime();
                new PseudoDeviceAddress(randomSource);
                t1 = System.nanoTime();
                distribution.add(t1 - t0);
            }
            System.err.println(randomSource.getClass().getSimpleName() + " : " + distribution);
        }
    }

    @Test
    public void testCrossPlatform() throws Exception {
        final PrintWriter out = TestUtil.androidPrintWriter("pseudoDeviceAddress.csv");
        out.println("value,data");
        long i = 1;
        while (i <= (Int64.max.value / 7)) {
            out.println(i + "," + new Data(new PseudoDeviceAddress(i).data).base64EncodedString());
            out.println(-i + "," + new Data(new PseudoDeviceAddress(-i).data).base64EncodedString());
            i *= 7;
        }
        out.flush();
        out.close();
        TestUtil.assertEqualsCrossPlatform("pseudoDeviceAddress.csv");
    }

    @Test
    public void testDataRange() {
        // Variable data length shouldn't cause exception
        // Address data should be 6 bytes regardless of original data
        for (int i=0; i<512; i++) {
            final PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress(new byte[i]);
            assertEquals(0, pseudoDeviceAddress.address);
            assertEquals(6, pseudoDeviceAddress.data.length);
        }
    }
}
