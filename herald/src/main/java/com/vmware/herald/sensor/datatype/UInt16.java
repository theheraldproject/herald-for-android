//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Unsigned integer (16 bits)
public class UInt16 {
    public final int value;
    public final Data bigEndian;

    public UInt16(int value) {
        assert(value >= 0);
        this.value = value;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) value);
        this.bigEndian = new Data(byteBuffer.array());
    }
}
