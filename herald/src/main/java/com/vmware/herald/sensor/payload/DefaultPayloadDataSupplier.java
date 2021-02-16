//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload;

import android.util.Log;

import com.vmware.herald.sensor.Device;
import com.vmware.herald.sensor.PayloadDataSupplier;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.LegacyPayloadData;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.SensorType;

import java.util.ArrayList;
import java.util.List;

/// Default payload data supplier implementing fixed length payload splitting method.
public abstract class DefaultPayloadDataSupplier implements PayloadDataSupplier {

    @Override
    public LegacyPayloadData legacyPayload(PayloadTimestamp timestamp, Device device) {
        return null;
    }

    @Override
    public List<PayloadData> payload(Data data) {
        // Get fixed length payload data
        final PayloadData fixedLengthPayloadData = payload(new PayloadTimestamp(), null);
        final int payloadDataLength = fixedLengthPayloadData.value.length;
        // Split raw data comprising of concatenated payloads into individual payloads
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + payloadDataLength) <= bytes.length; index += payloadDataLength) {
            final byte[] payloadBytes = new byte[payloadDataLength];
            System.arraycopy(bytes, index, payloadBytes, 0, payloadDataLength);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }
}
