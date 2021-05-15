//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TripleTests {

    @Test
    public void testInit() {
        assertEquals("A", new Triple<>("A","B", "C").a);
        assertEquals("B", new Triple<>("A","B", "C").b);
        assertEquals("C", new Triple<>("A","B", "C").c);
        assertNotNull(new Triple<>("A","B", "C").toString());
        assertNotNull(new Triple<String,String,String>(null,null, null).toString());
    }
}
