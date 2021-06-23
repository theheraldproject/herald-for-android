//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class LocationTests {

    @Test
    public void testInitNull() {
        assertNull(new Location(null, null, null).value);
        assertNull(new Location(null, null, null).start);
        assertNull(new Location(null, null, null).end);
        assertNotNull(new Location(null, null, null).description());
    }

    @Test
    public void testInit() {
        final LocationReference locationReference = new PlacenameLocationReference("place");
        assertEquals(locationReference, new Location(locationReference, new Date(1), new Date(2)).value);
        assertEquals(1, new Location(locationReference, new Date(1), new Date(2)).start.getTime());
        assertEquals(2, new Location(locationReference, new Date(1), new Date(2)).end.getTime());
        assertNotNull(new Location(locationReference, new Date(1), new Date(2)).description());
    }
}
