//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data.security;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int64;
import io.heraldprox.herald.sensor.datatype.random.NonBlockingSecureRandom;
import io.heraldprox.herald.sensor.datatype.random.RandomSource;

public abstract class PseudoRandomFunction {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.Security.PseudoRandomFunction");
    private final RandomSource randomSource = new NonBlockingSecureRandom();
    /**
     * Get next bytes from random function.
     * @param data Fill data with random bytes.
     * @return True if successful, false otherwise.
     */
    public abstract boolean nextBytes(@NonNull final Data data);

    @NonNull
    public Data nextBytes(final int count) {
        final byte[] bytes = new byte[count];
        final Data data = new Data(bytes);
        if (!nextBytes(data)) {
            logger.fault("Random function failed, reverting to default random");
            randomSource.nextBytes(bytes);
        }
        return data;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public Int64 nextInt64() {
        return nextBytes(8).int64(0);
    }
}
