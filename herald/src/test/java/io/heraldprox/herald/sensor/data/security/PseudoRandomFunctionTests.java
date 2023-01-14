//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import org.junit.Test;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int64;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PseudoRandomFunctionTests {

    @Test
    public void testNextBytes() {
        final PseudoRandomFunction random = new SecureRandomFunction();
        final Data randomData = new Data((byte) 0, 10);
        assertTrue(random.nextBytes(randomData));
        System.out.println("nextBytes=" + randomData.hexEncodedString());
        assertNotEquals(randomData, new Data((byte) 0, 10));
    }

    @Test
    public void testNextBytesCount() {
        final PseudoRandomFunction random = new SecureRandomFunction();
        final Data randomData = random.nextBytes(10);
        assertNotNull(randomData);
        System.out.println("nextBytes(count)=" + randomData.hexEncodedString());
        assertNotEquals(randomData, new Data((byte) 0, 10));
    }

    @Test
    public void testNextInt64() {
        final PseudoRandomFunction random = new SecureRandomFunction();
        final Int64 randomValue = random.nextInt64();
        assertNotNull(randomValue);
        System.out.println("nextInt64=" + randomValue);
        assertNotEquals(randomValue.value, 0);
    }
}
