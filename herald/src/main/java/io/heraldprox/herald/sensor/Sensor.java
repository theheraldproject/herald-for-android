//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor;

import androidx.annotation.NonNull;

/**
 * Sensor for detecting and tracking various kinds of disease transmission vectors, e.g. contact with people, time at location.
 */
public interface Sensor {

    /**
     * Add delegate for responding to sensor events.
     * @param delegate Delegate for responding to events.
     */
    void add(@NonNull final SensorDelegate delegate);

    /**
     * Start sensing.
     */
    void start();

    /**
     * Stop sensing.
     */
    void stop();
}
