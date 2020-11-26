//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.sonar;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.payload.DefaultPayloadDataSupplier;

import java.nio.ByteBuffer;

/// Mock SONAR payload supplier for simulating payload transfer of 129 byte Sonar payload data.
public class SonarPayloadDataSupplier extends DefaultPayloadDataSupplier {
    private final static int length = 129;
    private final int identifier;

    public SonarPayloadDataSupplier(int identifier) {
        this.identifier = identifier;
    }

    private Data networkByteOrderData(int identifier) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(0, identifier);
        return new Data(byteBuffer.array());
    }

    @Override
    public PayloadData payload(PayloadTimestamp timestamp) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        // First 3 bytes are reserved in SONAR
        byteBuffer.position(3);
        byteBuffer.put(networkByteOrderData(identifier).value);
        return new PayloadData(byteBuffer.array());
    }
}
