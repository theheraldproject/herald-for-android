//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertSegment {
    @NonNull
    public final BLEAdvertSegmentType type;
    public final int dataLength;
    @NonNull
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    @NonNull
    public final Data raw;

    public BLEAdvertSegment(@NonNull final BLEAdvertSegmentType type, final int dataLength, @NonNull final byte[] data, @NonNull final Data raw) {
        this.type = type;
        this.dataLength = dataLength;
        this.data = data;
        this.raw = raw;
    }

    @NonNull
    @Override
    public String toString() {
        return "BLEAdvertSegment{" +
                "type=" + type +
                ", dataLength=" + dataLength +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
