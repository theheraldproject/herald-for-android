//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import androidx.annotation.NonNull;

public interface SensorLogger extends Resettable {

    void debug(@NonNull final String message, final Object... values);

    void info(@NonNull final String message, final Object... values);

    void fault(@NonNull final String message, final Object... values);
}
