//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/**
 * Measurement unit for interpreting the proximity data values.
 */
public enum ProximityMeasurementUnit {
    /**
     * Received signal strength indicator, e.g. BLE signal strength as proximity estimator.
     */
    RSSI,
    /**
     * Roundtrip time, e.g. Audio signal echo time duration as proximity estimator.
     */
    RTT
}
