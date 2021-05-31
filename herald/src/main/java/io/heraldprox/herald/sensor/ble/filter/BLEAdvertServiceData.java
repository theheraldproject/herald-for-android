//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertServiceData {
    public final byte[] service;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertServiceData(final byte[] service, final byte[] dataBigEndian, final Data raw) {
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
