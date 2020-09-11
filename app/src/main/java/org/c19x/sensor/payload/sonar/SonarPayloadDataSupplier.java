package org.c19x.sensor.payload.sonar;

import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadTimestamp;
import org.c19x.sensor.payload.DefaultPayloadDataSupplier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// Mock SONAR payload supplier for simulating payload transfer of 129 byte Sonar payload data.
public class SonarPayloadDataSupplier extends DefaultPayloadDataSupplier {
    private static final int length = 129;
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
