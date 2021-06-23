//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Float16Tests {

    @Test
    public void testEncodeDecodeIntegers() {
        // Integers between 0 and 2048 can be exactly represented (and also between −2048 and 0)
        for (int i = -2048; i < 2048; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 1);
        }
        // Integers between 2048 and 4096 round to a multiple of 2 (even number)
        for (int i = 2048; i < 4096; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 2);
        }
        for (int i = -2048; i > -4096; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 2);
        }
        // Integers between 4096 and 8192 round to a multiple of 4
        for (int i = 4096; i < 8192; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 4);
        }
        for (int i = -4096; i > -8192; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 4);
        }
        // Integers between 8192 and 16384 round to a multiple of 8
        for (int i = 8192; i < 16384; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 8);
        }
        for (int i = -8192; i > -16384; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 8);
        }
        // Integers between 16384 and 32768 round to a multiple of 16
        for (int i = 16384; i < 32768; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 16);
        }
        for (int i = -16384; i > -32768; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 16);
        }
        // Integers between 32768 and 65519 round to a multiple of 32
        for (int i = 32768; i < 65535; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 32);
        }
        for (int i = -32768; i > -65535; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertTrue(Math.abs(i - actual.value) < 32);
        }
        // Integers above 65519 are rounded to "infinity" if using round-to-even, or above 65535 if using round-to-zero, or above 65504 if using round-to-infinity.
        for (int i = 65536; i < 100000; i++) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertEquals(Float.POSITIVE_INFINITY, actual.value, Float.MIN_VALUE);
        }
        for (int i = -65536; i > -100000; i--) {
            final Float16 actual = new Float16(new Float16(i).bigEndian);
            assertEquals(Float.NEGATIVE_INFINITY, actual.value, Float.MIN_VALUE);
        }
    }
}
