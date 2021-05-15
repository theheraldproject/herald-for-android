//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import io.heraldprox.herald.sensor.datatype.Data;

/// Matching key
public class MatchingKey extends Data {

    public MatchingKey(Data value) {
        super(value);
    }

    public MatchingKey(byte repeating, int count) {
        super(repeating, count);
    }
}
