//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.Device;
import io.heraldprox.herald.sensor.PayloadDataSupplier;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Default payload data supplier implementing fixed length payload splitting method.
 */
public abstract class DefaultPayloadDataSupplier implements PayloadDataSupplier {

    @Nullable
    @Override
    public LegacyPayloadData legacyPayload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device) {
        return null;
    }

    @NonNull
    @Override
    public List<PayloadData> payload(@NonNull final Data data) {
        // Get fixed length payload data
        final PayloadData fixedLengthPayloadData = payload(new PayloadTimestamp(), null);
        final int payloadDataLength = fixedLengthPayloadData.value.length;
        // Split raw data comprising of concatenated payloads into individual payloads
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + payloadDataLength) <= bytes.length; index += payloadDataLength) {
            final byte[] payloadBytes = new byte[payloadDataLength];
            System.arraycopy(bytes, index, payloadBytes, 0, payloadDataLength);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }
}
