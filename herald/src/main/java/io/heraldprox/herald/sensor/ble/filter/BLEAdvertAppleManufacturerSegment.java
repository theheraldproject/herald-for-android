//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertAppleManufacturerSegment {
    public final int type;
    public final int reportedLength;
    @NonNull
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    @NonNull
    public final Data raw;

    public BLEAdvertAppleManufacturerSegment(final int type, final int reportedLength, @NonNull final byte[] dataBigEndian, @NonNull final Data raw) {
        this.type = type;
        this.reportedLength = reportedLength;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @NonNull
    @Override
    public String toString() {
        return raw.hexEncodedString();
    }
}
