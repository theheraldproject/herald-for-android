//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.c19x;

import java.nio.ByteBuffer;

public class JavaData {

    /// Convert byte array into long value using the first 8 bytes.
    public final static long byteArrayToLong(final byte[] byteArray) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        return byteBuffer.getLong(0);
    }

    /// Convert long to byte array.
    public final static byte[] longToByteArray(final long value) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(0, value);
        return byteBuffer.array();
    }

}
