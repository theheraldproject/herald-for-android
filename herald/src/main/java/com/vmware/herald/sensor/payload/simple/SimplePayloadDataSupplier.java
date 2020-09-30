package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.Float16;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.datatype.UInt8;
import com.vmware.herald.sensor.payload.PayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

public class SimplePayloadDataSupplier implements PayloadDataSupplier {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.SimplePayloadDataSupplier");
    private final static int payloadLength = 23;
    private final Data commonHeader = new Data();

    /// Simple payload data supplier where transmit power is unknown.
    public SimplePayloadDataSupplier(UInt8 protocolAndVersion, UInt16 countryCode, UInt16 stateCode, SecretKey secretKey) {
        this(protocolAndVersion, countryCode, stateCode, new Float16(0), secretKey);
    }

    public SimplePayloadDataSupplier(UInt8 protocolAndVersion, UInt16 countryCode, UInt16 stateCode, Float16 transmitPower, SecretKey secretKey) {
        // Generate common header
        // All data is big endian
        commonHeader.append(protocolAndVersion.bigEndian);
        commonHeader.append(countryCode.bigEndian);
        commonHeader.append(stateCode.bigEndian);
        commonHeader.append(transmitPower.bigEndian);
    }

    @Override
    public PayloadData payload(PayloadTimestamp timestamp) {
        return null;
    }

    @Override
    public List<PayloadData> payload(Data data) {
        // Split raw data comprising of concatenated payloads into individual payloads
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + payloadLength) <= bytes.length; index += payloadLength) {
            final byte[] payloadBytes = new byte[payloadLength];
            System.arraycopy(bytes, index, payloadBytes, 0, payloadLength);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }
}
