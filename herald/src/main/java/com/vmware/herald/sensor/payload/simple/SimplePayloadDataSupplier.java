package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.payload.PayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

public class SimplePayloadDataSupplier implements PayloadDataSupplier {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.SimplePayloadDataSupplier");
    private final static int payloadLength = 23;
    private Data commonHeader;
//    private let commonPayload: Data
//    private let matchingKeys: [MatchingKey]
//    // Cache contact identifiers for the day
//    private var day: Int?
//    private var contactIdentifiers: [ContactIdentifier]?

//    public SimplePayloadDataSupplier(protocolAndVersion: UInt8, countryCode: UInt16, stateCode: UInt16, secretKey: SecretKey) {
//        // Generate common header
//        // All data is big endian
//        var protocolAndVersionBigEndian = protocolAndVersion.bigEndian
//        let protocolAndVersionData = Data(bytes: &protocolAndVersionBigEndian, count: MemoryLayout.size(ofValue: protocolAndVersionBigEndian))
//        var countryCodeBigEndian = countryCode.bigEndian
//        let countryCodeData = Data(bytes: &countryCodeBigEndian, count: MemoryLayout.size(ofValue: countryCodeBigEndian))
//        var stateCodeBigEndian = stateCode.bigEndian
//        let stateCodeData = Data(bytes: &stateCodeBigEndian, count: MemoryLayout.size(ofValue: stateCodeBigEndian))
//        // Common header = protocolAndVersion + countryCode + stateCode
//        var commonHeader = Data()
//        commonHeader.append(protocolAndVersionData)
//        commonHeader.append(countryCodeData)
//        commonHeader.append(stateCodeData)
//        self.commonHeader = commonHeader
//
//        // Generate common payload
//        // Transmit power is not available on iOS, pre-compute common payload
//        let transmitPower: Float = 0
//        var transmitPowerBinary16 = F.binary16(transmitPower)
//        let transmitPowerBinary16Data = Data(bytes: &transmitPowerBinary16, count: MemoryLayout.size(ofValue: transmitPowerBinary16))
//        // Common payload = commonHeader + transmitPower
//        var commonPayload = Data()
//        commonPayload.append(commonHeader)
//        commonPayload.append(transmitPowerBinary16Data)
//        self.commonPayload = commonPayload
//
//        // Generate matching keys from secret key
//        matchingKeys = K.matchingKeys(secretKey)
//    }


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
