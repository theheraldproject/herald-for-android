//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

/**
 * Free text place name.
 */
public class PlacenameLocationReference implements LocationReference {
    @NonNull
    public final String name;

    public PlacenameLocationReference(@NonNull final String name) {
        this.name = name;
    }

    @NonNull
    public String description() {
        return "PLACE(name=" + name + ")";
    }
}
