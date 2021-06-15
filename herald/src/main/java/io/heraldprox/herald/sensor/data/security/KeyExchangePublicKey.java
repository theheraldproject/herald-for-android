//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class KeyExchangePublicKey extends Data {

    public KeyExchangePublicKey(@NonNull final Data data) {
        super(data);
    }

    public KeyExchangePublicKey(@NonNull final String hexEncodedString) {
        this(Data.fromHexEncodedString(hexEncodedString));
    }
}
