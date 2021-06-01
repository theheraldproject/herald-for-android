//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertServiceData {
    @NonNull
    public final byte[] service;
    @NonNull
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    @NonNull
    public final Data raw;

    public BLEAdvertServiceData(@NonNull final byte[] service, @NonNull final byte[] dataBigEndian, @NonNull final Data raw) {
        this.service = service;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @NonNull
    @Override
    public String toString() {
        return "BLEAdvertServiceData{" +
                "service=" + new Data(service).hexEncodedString() +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
