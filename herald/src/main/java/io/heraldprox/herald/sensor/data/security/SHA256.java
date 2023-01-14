//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.util.Arrays;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int32;

/**
 * SHA256 cryptographic hash function.
 * <br>
 * NCSC Foundation Profile for TLS requires integrity check using SHA-256.
 */
public class SHA256 implements Integrity {
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.Security.SHA256");

    @NonNull
    @Override
    public Data hash(@NonNull Data data) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha.digest(data.value);
            return new Data(hash);
        } catch (Throwable e) {
            // This should never happen as every implementation of Java should
            // support MD5, SHA1, and SHA256 as a minimum.
            logger.fault("SHA-256 unavailable, emulating with non-cryptographic hash function", e);
            // Revert to build-in hash function to emulate 256-bit (32-byte) hash
            final Data hash = new Data();
            hash.append(new Int32(Arrays.hashCode(data.value)));
            while (hash.value.length < 32) {
                hash.append(new Int32(Arrays.hashCode(hash.value)));
            }
            return hash;
        }
    }
}
