//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Raw location data for estimating indirect exposure.
 */
public class Location {
    /**
     * Measurement values, e.g. GPS coordinates in comma separated string format for latitude and longitude
     */
    public final LocationReference value;
    /**
     * Time spent at location.
     */
    public final Date start, end;

    public Location(@NonNull final LocationReference value, @NonNull final Date start, @NonNull final Date end) {
        this.value = value;
        this.start = start;
        this.end = end;
    }

    /**
     * Get plain text description of proximity data
     * @return Description
     */
    @NonNull
    public String description() {
        //noinspection ConstantConditions
        return (null == value ? "null" : value.description()) + ":[from=" + start + ",to=" + end + "]";
    }
}
