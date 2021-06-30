//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class EncryptionKey extends Data {

    public EncryptionKey(@NonNull final Data data) {
        super(data);
    }

    public EncryptionKey(@NonNull final String hexEncodedString) {
        this(Data.fromHexEncodedString(hexEncodedString));
    }
}
