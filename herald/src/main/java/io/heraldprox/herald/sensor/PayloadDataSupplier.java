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

/// Payload data supplier, e.g. BeaconCodes in C19X and BroadcastPayloadSupplier in Sonar.
public interface PayloadDataSupplier {
    /// Legacy payload supplier callback - for those transitioning their apps to Herald. Note: Device may be null if Payload in use is same for all receivers
    @Nullable
    LegacyPayloadData legacyPayload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device);

    /// Get payload for given timestamp. Use this for integration with any payload generator, e.g. BeaconCodes or SonarBroadcastPayloadService
    @NonNull
    PayloadData payload(@NonNull final PayloadTimestamp timestamp, @Nullable final Device device);

    /// Parse raw data into payloads
    @NonNull
    List<PayloadData> payload(@NonNull final Data data);
}
