//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/// Acceleration (x,y,z) in meters per second at point in time
public class InertiaLocationReference implements LocationReference {
    public final double x, y, z, magnitude;

    public InertiaLocationReference(Double x, Double y, Double z) {
        this.x = (null == x ? 0 : x);
        this.y = (null == y ? 0 : y);
        this.z = (null == z ? 0 : z);
        this.magnitude = Math.sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
    }

    public String description() {
        return "Inertia(magnitude=" + magnitude + ",x=" + x + ",y=" + y + ",z=" + z + ")";
    }
}
