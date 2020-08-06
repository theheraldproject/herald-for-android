package org.c19x.sensor.payload.sonar;

import org.c19x.sensor.datatype.Data;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.PayloadTimestamp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// Mock SONAR payload supplier for simulating payload transfer of the same size to test C19X-SENSOR
public class MockSonarPayloadDataSupplier implements SonarPayloadDataSupplier {
    private static int length = 129;
    private int identifier;

    public MockSonarPayloadDataSupplier(int identifier) {
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
        byteBuffer.put(networkByteOrderData(identifier).value);
        final PayloadData payloadData = new PayloadData(byteBuffer.array());
        return payloadData;
    }

    @Override
    public List<PayloadData> payload(Data data) {
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + length) <= bytes.length; index += length) {
            final byte[] payloadBytes = new byte[length];
            System.arraycopy(bytes, index, payloadBytes, 0, length);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }
}
