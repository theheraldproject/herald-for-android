//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.payload.simple;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Data;

import java.security.MessageDigest;

/// Elementary functions
public class F {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Payload.SimplePayloadDataSupplier");

    /// Cryptographic hash function : SHA256
    protected static Data h(Data data) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(data.value);
            return new Data(hash);
        } catch (Throwable e) {
            logger.fault("SHA-256 unavailable", e);
            return null;
        }
    }

    /// Truncation function : Delete second half of data
    protected static Data t(Data data) {
        return t(data, data.value.length / 2);
    }

    /// Truncation function : Retain first n bytes of data
    protected static Data t(Data data, int n) {
        return data.subdata(0, n);
    }

    /// XOR function : Compute left xor right, assumes left and right are the same length
    protected static Data xor(Data left, Data right) {
        final byte[] leftByteArray = left.value;
        final byte[] rightByteArray = right.value;
        final byte[] resultByteArray = new byte[left.value.length];
        for (int i=0; i<leftByteArray.length; i++) {
            resultByteArray[i] = (byte) (leftByteArray[i] ^ rightByteArray[i]);
        }
        return new Data(resultByteArray);
    }
}
