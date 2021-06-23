//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PayloadDataTests {

    @Test
    public void testShortName() {
        // Deliberately making it random but deterministic for consistent testing
        final Random random = new Random(0);
        for (int i=0; i<1000; i++) {
            final byte[] data = new byte[i];
            random.nextBytes(data);
            final PayloadData payloadData = new PayloadData(data);
            assertArrayEquals(data, payloadData.value);
            assertNotNull(payloadData.shortName());
            assertEquals(payloadData.toString(), payloadData.shortName());
        }
    }

}
