//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Cryptographically secure key exchange
 */
public interface KeyExchange {
    /**
     * Generate a random key pair for key exchange with peer
     * @return Private/Public key pair.
     */
    @NonNull
    KeyExchangeKeyPair keyPair();

    /**
     * Generate shared key by combining own private key and peer public key.
     * @param own Own private key.
     * @param peer Peer public key.
     * @return Shared key on success, null otherwise.
     */
    @Nullable
    KeyExchangeSharedKey sharedKey(@NonNull final KeyExchangePrivateKey own, @NonNull final KeyExchangePublicKey peer);
}
