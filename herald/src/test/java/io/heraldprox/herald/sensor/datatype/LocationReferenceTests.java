//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
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

    @Test
    public void testInertiaLocationReference() {
        assertEquals(0, new InertiaLocationReference(null, null, null).x, Double.MIN_VALUE);
        assertEquals(0, new InertiaLocationReference(null, null, null).y, Double.MIN_VALUE);
        assertEquals(0, new InertiaLocationReference(null, null, null).z, Double.MIN_VALUE);
        assertEquals(0, new InertiaLocationReference(null, null, null).magnitude, Double.MIN_VALUE);
        assertEquals(1, new InertiaLocationReference(1d, null, null).x, Double.MIN_VALUE);
        assertEquals(2, new InertiaLocationReference(null, 2d, null).y, Double.MIN_VALUE);
        assertEquals(3, new InertiaLocationReference(null, null, 3d).z, Double.MIN_VALUE);
        assertEquals(3.464, new InertiaLocationReference(2d, 2d, 2d).magnitude, 0.001);
        assertNotNull(new InertiaLocationReference(null, null, null).description());
        assertNotNull(new InertiaLocationReference(2d, 2d, 2d).description());
        assertNotNull(new InertiaLocationReference(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).description());
        assertNotNull(new InertiaLocationReference(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE).description());
        assertEquals(Double.POSITIVE_INFINITY, new InertiaLocationReference(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).magnitude, 0.001);
        assertEquals(Double.POSITIVE_INFINITY, new InertiaLocationReference(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE).magnitude, 0.001);
    }
}
