//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import io.heraldprox.herald.sensor.datatype.Data;

public class BLEAdvertSegment {
    public final BLEAdvertSegmentType type;
    public final int dataLength;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;

    public BLEAdvertSegment(BLEAdvertSegmentType type, int dataLength, byte[] data, Data raw) {
        this.type = type;
        this.dataLength = dataLength;
        this.data = data;
        this.raw = raw;
    }

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
