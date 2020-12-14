//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Unsigned integer (8 bits)
public class UInt8 {
    public final int value;
    public final Data bigEndian;

    public UInt8(int value) {
        this.bigEndian = encode(value);
        this.value = decode(bigEndian);
    }

    protected final static Data encode(int value) {
        final int valueForEncoding = (value < 0 ? 0 : (value > Byte.MAX_VALUE ? Byte.MAX_VALUE : value));
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put((byte) valueForEncoding);
        return new Data(byteBuffer.array());
    }

    protected final static int decode(Data data) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put(data.value, 0, 1);
        byteBuffer.position(0);
        final int value = byteBuffer.get();
        return (value < 0 ? 0 : (value > Byte.MAX_VALUE ? Byte.MAX_VALUE : value));
    }
}
