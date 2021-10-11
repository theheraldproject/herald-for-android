//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.payload.simple;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

/**
 * Simple payload data supplier.
 */
public class ConcreteSimplePayloadDataSupplier extends DefaultPayloadDataSupplier implements SimplePayloadDataSupplier {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.SimplePayloadDataSupplier");
    public final static int payloadLength = 21;
    private final Data commonPayload = new Data();
    @NonNull
    private final SecretKey secretKey;
    // Cache contact identifiers for the day
    @Nullable
    private Integer day = null;
    @Nullable
    private MatchingKey matchingKey = null;
    @Nullable
    private Integer period = null;
    @Nullable
    private ContactKey contactKey = null;
    @Nullable
    private ContactIdentifier contactIdentifier = null;

    public ConcreteSimplePayloadDataSupplier(@NonNull final UInt8 protocolAndVersion, @NonNull final UInt16 countryCode, @NonNull final UInt16 stateCode, @NonNull final SecretKey secretKey) {
        // Generate common header
        // All data is big endian
        commonPayload.append(protocolAndVersion);
        commonPayload.append(countryCode);
        commonPayload.append(stateCode);

        // Generate matching keys from secret key
        this.secretKey = secretKey;
    }

    /**
     * Generate a new secret key
     * @return
     */
    @NonNull
    public static SecretKey generateSecretKey() {
        return K.secretKey();
    }

    /**
     * Generate matching key for day
     */
    @Nullable
    public MatchingKey matchingKey(@NonNull final Date time) {
        final int day = K.day(time);
        final MatchingKeySeed matchingKeySeed = K.matchingKeySeed(secretKey, day);
        if (null == matchingKeySeed) {
            logger.fault("Failed to generate matching key seed (time={},day={})", time, day);
            return null;
        }
        return K.matchingKey(matchingKeySeed);
    }

    /**
     * Generate contact identifier for time.
     * @param time Time
     * @return Contact identifier
     */
    @Nullable
    private ContactIdentifier contactIdentifier(@NonNull final Date time) {
        // Generate contact key and contact identifier
        final int day = K.day(time);
        if (null == this.day || this.day != day || null == this.matchingKey) {
            this.matchingKey = matchingKey(time);
            this.day = day;
            // Reset contact key on matching key change
            this.contactKey = null;
            this.period = null;
        }

        final int  period = K.period(time);
        if (null == this.period || this.period != period) {
            final MatchingKey matchingKey = this.matchingKey;
            if (null == matchingKey) {
                logger.fault("Contact identifier out of range, failed to generate matching key (time={},day={})", time, day);
                return null;
            }
            final ContactKeySeed contactKeySeed = K.contactKeySeed(matchingKey, period);
            if (null == contactKeySeed) {
                logger.fault("Contact identifier out of range, failed to generate contact key seed (time={},day={})", time, day);
                return null;
            }
            this.contactKey = K.contactKey(contactKeySeed);
            this.period = period;
            this.contactIdentifier = K.contactIdentifier(this.contactKey);
        }

        // Defensive check
        final ContactIdentifier contactIdentifier = this.contactIdentifier;
        if (null == contactIdentifier || contactIdentifier.value.length != 16) {
            logger.fault("Contact identifier out of range (time={},day={})", time, day);
            return null;
        }

        return contactIdentifier;
    }

    // MARK:- SimplePayloadDataSupplier

    @NonNull
    @Override
    public PayloadData payload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device) {

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

    @NonNull
    @Override
    public List<PayloadData> payload(@NonNull final Data data) {
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
