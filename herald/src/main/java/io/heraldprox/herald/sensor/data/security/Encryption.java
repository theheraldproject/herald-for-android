//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Data;

/**
 * Cryptographically secure encryption and decryption algorithm
 */
public interface Encryption {

    /**
     * Encrypt data.
     * @param data Data to be encrypted.
     * @param with Encryption key.
     * @return Encrypted data, or null on failure.
     */
    @Nullable
    Data encrypt(@NonNull Data data, @NonNull EncryptionKey with);

    /**
     * Decrypt data.
     * @param data Data to be decrypted.
     * @param with Encryption key.
     * @return Decrypted data, or null on failure.
     */
    @Nullable
    Data decrypt(@NonNull Data data, @NonNull EncryptionKey with);
}
