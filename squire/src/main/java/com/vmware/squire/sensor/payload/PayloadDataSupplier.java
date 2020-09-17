//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.squire.sensor.payload;

import com.vmware.squire.sensor.datatype.Data;
import com.vmware.squire.sensor.datatype.PayloadData;
import com.vmware.squire.sensor.datatype.PayloadTimestamp;

import java.util.List;

/// Payload data supplier, e.g. BeaconCodes in C19X and BroadcastPayloadSupplier in Sonar.
public interface PayloadDataSupplier {
    /// Get payload for given timestamp. Use this for integration with any payload generator, e.g. BeaconCodes or SonarBroadcastPayloadService
    PayloadData payload(PayloadTimestamp timestamp);

    /// Parse raw data into payloads
    List<PayloadData> payload(Data data);
}
