//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Matching key
 */
public class MatchingKey extends Data {

    public MatchingKey(@NonNull final Data value) {
        super(value);
    }

    public MatchingKey(final byte repeating, final int count) {
        super(repeating, count);
    }
}
