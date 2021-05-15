//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import io.heraldprox.herald.sensor.datatype.Data;

/// Secret key
public class SecretKey extends Data {

    public SecretKey(byte[] value) {
        super(value);
    }

    public SecretKey(byte repeating, int count) {
        super(repeating, count);
    }
}
