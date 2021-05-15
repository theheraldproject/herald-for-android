//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

public interface SensorLogger {

    void debug(String message, final Object... values);

    void info(String message, final Object... values);

    void fault(String message, final Object... values);
}
