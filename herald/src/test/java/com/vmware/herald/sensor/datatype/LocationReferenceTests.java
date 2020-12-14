//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class LocationReferenceTests {

    @Test
    public void testPlacenameLocationReference() {
        assertNull(new PlacenameLocationReference(null).name);
        assertNotNull(new PlacenameLocationReference(null).description());
        assertNotNull(new PlacenameLocationReference("place").name);
        assertEquals("place", new PlacenameLocationReference("place").name);
        assertNotNull(new PlacenameLocationReference("place").description());
    }

    @Test
    public void testWGS84CircularAreaLocationReference() {
        assertNull(new WGS84CircularAreaLocationReference(null, null, null, null).latitude);
        assertNull(new WGS84CircularAreaLocationReference(null, null, null, null).longitude);
        assertNull(new WGS84CircularAreaLocationReference(null, null, null, null).altitude);
        assertNull(new WGS84CircularAreaLocationReference(null, null, null, null).radius);
        assertEquals((Double) 1d, new WGS84CircularAreaLocationReference(1d, 2d, 3d, 4d).latitude);
        assertEquals((Double) 2d, new WGS84CircularAreaLocationReference(1d, 2d, 3d, 4d).longitude);
        assertEquals((Double) 3d, new WGS84CircularAreaLocationReference(1d, 2d, 3d, 4d).altitude);
        assertEquals((Double) 4d, new WGS84CircularAreaLocationReference(1d, 2d, 3d, 4d).radius);
        assertNotNull(new WGS84CircularAreaLocationReference(null, null, null, null).description());
        assertNotNull(new WGS84CircularAreaLocationReference(1d, 2d, 3d, 4d).description());
    }

    @Test
    public void testWGS84PointLocationReference() {
        assertNull(new WGS84PointLocationReference(null, null, null).latitude);
        assertNull(new WGS84PointLocationReference(null, null, null).longitude);
        assertNull(new WGS84PointLocationReference(null, null, null).altitude);
        assertEquals((Double) 1d, new WGS84PointLocationReference(1d, 2d, 3d).latitude);
        assertEquals((Double) 2d, new WGS84PointLocationReference(1d, 2d, 3d).longitude);
        assertEquals((Double) 3d, new WGS84PointLocationReference(1d, 2d, 3d).altitude);
        assertNotNull(new WGS84PointLocationReference(null, null, null).description());
        assertNotNull(new WGS84PointLocationReference(1d, 2d, 3d).description());
    }
}
