//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.analysis.sampling;

import com.vmware.herald.sensor.datatype.Date;

import java.text.SimpleDateFormat;

public class Sample<T> {
    private final Date taken;
    private final T value;

    public Sample(final Date taken, final T value) {
        this.taken = taken;
        this.value = value;
    }

    public Sample(final long secondsSinceUnixEpoch, final T value) {
        this.taken = new Date(secondsSinceUnixEpoch);
        this.value = value;
    }

    public Sample(final Sample<T> other) {
        this.taken = other.taken;
        this.value = other.value;
    }

    public Sample(final T value) {
        this.taken = new Date();
        this.value = value;
    }

    public Date taken() {
        return taken;
    }

    public T value() {
        return value;
    }

    @Override
    public String toString() {
        return '(' + taken.toString() + ',' + value + ')';
    }

    public final static <T> Class<?> valueType(final Sample<T> sample) {
        return sample.value.getClass();
    }
}
