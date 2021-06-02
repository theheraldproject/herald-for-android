//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.sampling;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Date;

public class Sample<T> {
    private final Date taken;
    private final T value;

    public Sample(@NonNull final Date taken, @NonNull final T value) {
        this.taken = taken;
        this.value = value;
    }

    public Sample(final long secondsSinceUnixEpoch, @NonNull final T value) {
        this.taken = new Date(secondsSinceUnixEpoch);
        this.value = value;
    }

    public Sample(@NonNull final Sample<T> other) {
        this.taken = other.taken;
        this.value = other.value;
    }

    public Sample(@NonNull final T value) {
        this.taken = new Date();
        this.value = value;
    }

    @NonNull
    public Date taken() {
        return taken;
    }

    @NonNull
    public T value() {
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        return '(' + taken.toString() + ',' + value + ')';
    }

    @NonNull
    public static <T> Class<?> valueType(@NonNull final Sample<T> sample) {
        return sample.value.getClass();
    }
}
