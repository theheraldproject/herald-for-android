//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.Int64;

import java.util.Objects;

public class SampledID implements Comparable<SampledID> {
    public final long value;

    public SampledID(final long value) {
        this.value = value;
    }

    public SampledID(@NonNull final Data data) {
        final Data hashValue = new Data();
        hashValue.append(new Int64(0));
        for (int i=0, j=0; i<data.value.length; i++, j++) {
            if (j >= 8) {
                j = 0;
            }
            hashValue.value[j] = (byte) (hashValue.value[j] ^ data.value[i]);
        }
        //noinspection ConstantConditions
        this.value = hashValue.int64(0).value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        SampledID sampledID = (SampledID) o;
        return value == sampledID.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public int compareTo(@NonNull final SampledID o) {
        return Long.compare(value, o.value);
    }
}
