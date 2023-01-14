//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Legacy payload data received from target
 */
public class LegacyPayloadData extends PayloadData {
    @Nullable
    public final UUID service; // null value is permitted
    public enum ProtocolName {
        UNKNOWN, NOT_AVAILABLE, HERALD, OPENTRACE, ADVERT
    }

    public LegacyPayloadData(@Nullable final UUID service, @NonNull final byte[] value) {
        super(value);
        this.service = service; // null value is permitted
    }

    @NonNull
    public ProtocolName protocolName() {
        if (null == service) {
            return ProtocolName.NOT_AVAILABLE;
        } else if (service == BLESensorConfiguration.interopOpenTraceServiceUUID) {
            return ProtocolName.OPENTRACE;
        } else if (service == BLESensorConfiguration.interopAdvertBasedProtocolServiceUUID) {
            return ProtocolName.ADVERT;
        } else if (service == BLESensorConfiguration.linuxFoundationServiceUUID) {
            return ProtocolName.HERALD;
        } else if (BLESensorConfiguration.legacyHeraldServiceDetectionEnabled &&
                   service == BLESensorConfiguration.legacyHeraldServiceUUID) {
            return ProtocolName.HERALD;
        } else {
            return ProtocolName.UNKNOWN;
        }
    }

    @NonNull
    @Override
    public String shortName() {
        // Decoder for test payload to assist debugging of OpenTrace interop
        if (service == BLESensorConfiguration.interopOpenTraceServiceUUID) {
            try {
                final JSONObject jsonObject = new JSONObject(new String(value));
                final String base64EncodedPayloadData = jsonObject.getString("id");
                final PayloadData payloadData = new PayloadData(base64EncodedPayloadData);
                return payloadData.shortName();
            } catch (Throwable e) {
                // Errors are expected for real payloads
            }
        }
        return super.shortName();
    }
}
