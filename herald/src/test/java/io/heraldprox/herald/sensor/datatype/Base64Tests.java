//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class Base64Tests {

    @Test
    public void testEncodeDecode() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        // Ensure data of all lengths are encoded and decoded correctly
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final String base64String = Base64.encode(data);
            assertNotNull(base64String);
            if (length > 0) {
                assertFalse(base64String.isEmpty());
            }
            final byte[] actual = Base64.decode(base64String);
            assertArrayEquals(data, actual);
        }
    }

    @Test
    public void testEncodeDecodeWithInvalidCharacters() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        // Ensure data of all lengths are encoded and decoded correctly
        for (int length=0; length<1000; length++) {
            final byte[] data = new byte[length];
            random.nextBytes(data);
            final String base64String = Base64.encode(data);
            assertNotNull(base64String);
            if (length > 0) {
                assertFalse(base64String.isEmpty());
            }
            final String base64StringWithInvalidCharacter = "!@£$%^&*()" + base64String + "!@£$%^&*()";
            final byte[] actual = Base64.decode(base64StringWithInvalidCharacter);
            assertArrayEquals(data, actual);
        }
    }
}
