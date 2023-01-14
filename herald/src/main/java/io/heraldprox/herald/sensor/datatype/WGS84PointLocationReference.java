//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

/**
 * GPS coordinates (latitude,longitude,altitude) in WGS84 decimal format and meters from sea level.
 */
public class WGS84PointLocationReference implements LocationReference {
    @NonNull
    public final Double latitude, longitude, altitude;

    public WGS84PointLocationReference(@NonNull final Double latitude, @NonNull final Double longitude, @NonNull final Double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    @NonNull
    public String description() {
        return "WGS84(lat=" + latitude + ",lon=" + longitude + ",alt=" + altitude + ")";
    }
}
