//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class RandomSourceTests {

    @Test
    public void testNextInt() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final RandomSource randomSource = new RandomSource(method);
            int lastValue = randomSource.nextInt();
            for (int i=0; i<1000; i++) {
                final int value = randomSource.nextInt();
                assertNotEquals(lastValue, value);
                lastValue = value;
            }
        }
    }

    @Test
    public void testNextLong() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final RandomSource randomSource = new RandomSource(method);
            long lastValue = randomSource.nextLong();
            for (int i=0; i<1000; i++) {
                final long value = randomSource.nextLong();
                assertNotEquals(lastValue, value);
                lastValue = value;
            }
        }
    }

    @Test
    public void testNextDouble() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final RandomSource randomSource = new RandomSource(method);
            double lastValue = randomSource.nextDouble();
            for (int i=0; i<1000; i++) {
                final double value = randomSource.nextDouble();
                assertNotEquals(lastValue, value, Double.MIN_VALUE);
                lastValue = value;
            }
        }
    }

    @Test
    public void testNextBytes() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final RandomSource randomSource = new RandomSource(method);
            for (int length=0; length<10; length++) {
                byte[] lastValue = new byte[length];
                randomSource.nextBytes(lastValue);
                for (int i = 0; i < 10; i++) {
                    final byte[] value = new byte[length];
                    randomSource.nextBytes(value);
                    if (length > 0) {
                        assertFalse(Arrays.equals(lastValue, value));
                    }
                    lastValue = value;
                }
            }
        }
    }

    @Test
    public void testExternalEntropy() {
        for (RandomSource.Method method : RandomSource.Method.values()) {
            final RandomSource randomSource = new RandomSource(method);
            int lastValue = randomSource.nextInt();
            for (int i=0; i<1000; i++) {
                randomSource.addEntropy(i);
                final int value = randomSource.nextInt();
                assertNotEquals(lastValue, value);
                lastValue = value;
            }
        }
    }


}
