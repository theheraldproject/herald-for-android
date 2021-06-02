//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Tuple<A, B> {
    @Nullable
    public final A a;
    @Nullable
    public final B b;

    public Tuple(@Nullable final A a, @Nullable final B b) {
        this.a = a;
        this.b = b;
    }

    @NonNull
    @Override
    public String toString() {
        return "(" + a + "," + b + ")";
    }
}
