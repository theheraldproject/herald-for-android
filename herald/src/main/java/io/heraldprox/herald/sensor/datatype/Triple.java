//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Triple<A, B, C> {
    @Nullable
    public final A a;
    @Nullable
    public final B b;
    @Nullable
    public final C c;

    public Triple(@Nullable final A a, @Nullable final B b, @Nullable final C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @NonNull
    @Override
    public String toString() {
        return "Triple{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                '}';
    }
}
