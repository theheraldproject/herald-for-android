//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

/**
 * GPS coordinates and region radius, e.g. latitude and longitude in decimal format and radius in meters.
 */
public class WGS84CircularAreaLocationReference implements LocationReference {
    @NonNull
    public final Double latitude, longitude, altitude, radius;

    public WGS84CircularAreaLocationReference(@NonNull final Double latitude, @NonNull final Double longitude, @NonNull final Double altitude, @NonNull final Double radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.radius = radius;
    }

    @NonNull
    public String description() {
        return "WGS84(lat=" + latitude + ",lon=" + longitude + ",alt=" + altitude + ",radius=" + radius + ")";
    }
}
