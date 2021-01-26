//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.test;

import com.vmware.herald.sensor.Device;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.Int32;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.payload.DefaultPayloadDataSupplier;

import java.nio.ByteBuffer;

/// Mock SONAR payload supplier for simulating payload transfer of 129 byte Sonar payload data.
public class TestPayloadDataSupplier extends DefaultPayloadDataSupplier {
    private final int length;
    private final int identifier;

    public TestPayloadDataSupplier(int identifier) {
        this(identifier, 129);
    }

    public TestPayloadDataSupplier(int identifier, int length) {
        this.identifier = identifier;
        this.length = length;
    }

    @Override
    public PayloadData payload(PayloadTimestamp timestamp, Device device) {
        final PayloadData payloadData = new PayloadData();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        // First 3 bytes are reserved for protocolAndVersion (UInt8) + countryCode (UInt16)
        payloadData.append(new Data((byte) 0, 3));
        // Next 4 bytes are used for fixed cross-platform identifier (Int32)
        payloadData.append(new Int32(identifier));
        // Fill remaining payload with blank data to make payload the test length
        payloadData.append(new Data((byte) 0, length - payloadData.value.length));
        return payloadData;
    }

}
