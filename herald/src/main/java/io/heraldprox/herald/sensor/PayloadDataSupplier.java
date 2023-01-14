//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.PayloadTimestamp;

import java.util.List;

/**
 * Payload data supplier.
 */
public interface PayloadDataSupplier {
    /**
     * Legacy payload supplier callback - for those transitioning their apps to Herald.
     * <br>
     * Note: Device may be null if Payload in use is same for all receivers.
     * @param timestamp Timestamp for the payload, normally current time.
     * @param device Target device for the payload.
     * @return Legacy payload for a specific target device at a specific time.
     */
    @Nullable
    LegacyPayloadData legacyPayload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device);

    /**
     * Get payload for given timestamp. Use this for integration with any payload generator.
     * @param timestamp Timestamp for the payload, normally current time.
     * @param device Target device for the payload.
     * @return Payload for a specific target device at a specific time.
     */
    @NonNull
    PayloadData payload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device);

    /**
     * Parse raw data into payloads. The calling card mechanism in Herald acts as a payload relay
     * between devices. It will relay several payloads per call for efficiency by concatenating all
     * the relayed payloads into a single data block for transfer. If the payload format is fixed
     * size, the default implementation in {@link io.heraldprox.herald.sensor.payload.DefaultPayloadDataSupplier}
     * will automatically segment the received data block back into individual payloads. If the
     * payload format is variable sized, this method will need to be implemented to parse the
     * concatenated data block into individual payloads.
     * @param data Concatenated payloads.
     * @return List of individual payloads.
     */
    @NonNull
    List<PayloadData> payload(@NonNull final Data data);
}
