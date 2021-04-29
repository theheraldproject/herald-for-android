//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.ble.filter;

import com.vmware.herald.sensor.datatype.Data;

public class BLEAdvertServiceData {
    public final byte[] service;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertServiceData(final byte[] service, final byte[] dataBigEndian, final Data raw) {
        this.service = service;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "BLEAdvertServiceData{" +
                "service=" + new Data(service).hexEncodedString() +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
