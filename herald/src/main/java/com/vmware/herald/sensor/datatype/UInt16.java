//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Unsigned integer (16 bits)
public class UInt16 {
    public final static int MAX_VALUE = 65535;
    public final int value;
    public final Data bigEndian;

    public UInt16(int value) {
        this.bigEndian = encode(value);
        this.value = decode(bigEndian);
    }

    protected final static Data encode(int value) {
        final short valueForEncoding = (value < 0 ? 0 : (value > MAX_VALUE ? (short) MAX_VALUE : (short) value));
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort(valueForEncoding);
        return new Data(byteBuffer.array());
    }

    protected final static int decode(Data data) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put(data.value, 0, 2);
        byteBuffer.position(0);
        final int value = byteBuffer.getShort() & 0xffff;
        return value < 0 ? 0 : Math.min(value, MAX_VALUE);
    }

}
