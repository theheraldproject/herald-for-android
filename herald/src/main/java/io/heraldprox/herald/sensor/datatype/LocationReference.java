//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

/**
 * Raw location data for estimating indirect exposure, e.g. WGS84 coordinates.
 */
public interface LocationReference {
    @NonNull
    String description();
}
