//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

public class KeyExchangeKeyPair {
    @NonNull
    public final KeyExchangePrivateKey privateKey;
    @NonNull
    public final KeyExchangePublicKey publicKey;

    public KeyExchangeKeyPair(@NonNull final KeyExchangePrivateKey privateKey, @NonNull final KeyExchangePublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }
}
