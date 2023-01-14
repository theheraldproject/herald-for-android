//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Cryptographically secure hash function
 */
public interface Integrity {

    /**
     * Compute hash of data.
     * @param data Data to be hashed.
     * @return Hash of data.
     */
    @NonNull
    Data hash(@NonNull Data data);
}
