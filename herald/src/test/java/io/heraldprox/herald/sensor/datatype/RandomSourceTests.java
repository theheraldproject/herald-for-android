//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandom;
import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandomNIST;
import io.heraldprox.herald.sensor.datatype.random.BlockingSecureRandomSingleton;
import io.heraldprox.herald.sensor.datatype.random.NonBlockingPRNG;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RandomSourceTests {

    @Test
    public void testNextInt() {
        final List<RandomSource> randomSources = Arrays.asList(
                new NonBlockingPRNG(),
                new BlockingSecureRandom(),
                new BlockingSecureRandomSingleton(),
                new BlockingSecureRandomNIST());
        for (RandomSource randomSource : randomSources) {
            int duplicates = 0;
            int lastValue = randomSource.nextInt();
            for (int i=0; i<1000; i++) {
                final int value = randomSource.nextInt();
                if (value == lastValue) {
                    duplicates++;
                }
                lastValue = value;
            }
            // Duplicates may occur, but should be unlikely
            assertTrue(duplicates < 900);
        }
    }

    @Test
    public void testNextLong() {
        final List<RandomSource> randomSources = Arrays.asList(
                new NonBlockingPRNG(),
                new BlockingSecureRandom(),
                new BlockingSecureRandomSingleton(),
                new BlockingSecureRandomNIST());
        for (RandomSource randomSource : randomSources) {
            int duplicates = 0;
            long lastValue = randomSource.nextLong();
            for (int i=0; i<1000; i++) {
                final long value = randomSource.nextLong();
                if (value == lastValue) {
                    duplicates++;
                }
                lastValue = value;
            }
            // Duplicates may occur, but should be unlikely
            assertTrue(duplicates < 900);
        }
    }

    @Test
    public void testExternalEntropy() {
        final List<RandomSource> randomSources = Arrays.asList(
                new NonBlockingPRNG(),
                new BlockingSecureRandom(),
                new BlockingSecureRandomSingleton(),
                new BlockingSecureRandomNIST());
        for (RandomSource randomSource : randomSources) {
            int duplicates = 0;
            int lastValue = randomSource.nextInt();
            for (int i=0; i<1000; i++) {
                randomSource.addEntropy(i);
                final int value = randomSource.nextInt();
                if (value == lastValue) {
                    duplicates++;
                }
                lastValue = value;
            }
            // Duplicates may occur, but should be unlikely
            assertTrue(duplicates < 900);
        }
    }


}
