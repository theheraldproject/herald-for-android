//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import io.heraldprox.herald.sensor.Device;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;
import io.heraldprox.herald.sensor.datatype.UInt16;
import io.heraldprox.herald.sensor.datatype.UInt8;
import io.heraldprox.herald.sensor.payload.DefaultPayloadDataSupplier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/// Simple payload data supplier.
public class ConcreteSimplePayloadDataSupplier extends DefaultPayloadDataSupplier implements SimplePayloadDataSupplier {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.SimplePayloadDataSupplier");
    public final static int payloadLength = 21;
    private final Data commonPayload = new Data();
    private final MatchingKey[] matchingKeys;
    // Cache contact identifiers for the day
    private Integer day = null;
    private ContactIdentifier[] contactIdentifiers = null;

    public ConcreteSimplePayloadDataSupplier(UInt8 protocolAndVersion, UInt16 countryCode, UInt16 stateCode, SecretKey secretKey) {
        // Generate common header
        // All data is big endian
        commonPayload.append(protocolAndVersion);
        commonPayload.append(countryCode);
        commonPayload.append(stateCode);

        // Generate matching keys from secret key
        matchingKeys = K.matchingKeys(secretKey);
    }

    /// Generate a new secret key
    public static SecretKey generateSecretKey() {
        return K.secretKey();
    }

    /// Generate contact identifiers for a matching key
    public static ContactIdentifier[] contactIdentifiers(MatchingKey matchingKey) {
        final ContactKey[] contactKeys = K.contactKeys(matchingKey);
        final ContactIdentifier[] contactIdentifiers = new ContactIdentifier[contactKeys.length];
        for (int i=contactKeys.length; i-->0;) {
            contactIdentifiers[i] = K.contactIdentifier(contactKeys[i]);
        }
        return contactIdentifiers;
    }

    /// Generate contact identifier for time
    private ContactIdentifier contactIdentifier(Date time) {
        final int day = K.day(time);
        final int period = K.period(time);

        if (!(day >= 0 && day < matchingKeys.length)) {
            logger.fault("Contact identifier out of day range (time={},day={})", time, day);
            return null;
        }

        // Generate and cache contact keys for specific day on-demand
        if (null == this.day || this.day != day) {
            contactIdentifiers = contactIdentifiers(matchingKeys[day]);
            this.day = day;
        }

        if (null == contactIdentifiers) {
            logger.fault("Contact identifiers unavailable (time={},day={})", time, day);
            return null;
        }

        if (!(period >= 0 && period < contactIdentifiers.length)) {
            logger.fault("Contact identifier out of period range (time={},period={})", time, period);
            return null;
        }

        // Defensive check
        if (contactIdentifiers[period].value.length != 16) {
            logger.fault("Contact identifier not 16 bytes (time={},count={})", time, contactIdentifiers[period].value.length);
            return null;
        }

        return contactIdentifiers[period];
    }

    // MARK:- SimplePayloadDataSupplier

    @Override
    public PayloadData payload(PayloadTimestamp timestamp, Device device) {

        // TODO Add length of data here so it's compliant with the Herald Envelope Standard V1

        final PayloadData payloadData = new PayloadData();
        payloadData.append(commonPayload);
        final ContactIdentifier contactIdentifier = contactIdentifier(timestamp.value);
        if (contactIdentifier != null) {
            payloadData.append(contactIdentifier);
        } else {
            payloadData.append(new ContactIdentifier((byte) 0, 16));
        }
        return payloadData;
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
