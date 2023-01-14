//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/**
 * Sensor state
 */
public enum SensorState {
    /**
     * Sensor is powered on, active and operational
     */
    on,
    /**
     * Sensor is powered off, inactive and not operational
     */
    off,
    /**
     * Sensor is not available
     */
    unavailable
}
