//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Payload timestamp, should normally be Date, but it may change to UInt64 in the future to use server synchronised relative timestamp.
 */
public class PayloadTimestamp {
    @NonNull
    public final Date value;

    public PayloadTimestamp(@NonNull final Date value) {
        this.value = value;
    }

    public PayloadTimestamp() {
        this(new Date());
    }
}
