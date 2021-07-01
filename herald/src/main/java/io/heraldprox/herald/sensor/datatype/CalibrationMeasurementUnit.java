//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

/**
 * Measurement unit for calibrating the proximity transmission data values, e.g. BLE transmit power
 */
public enum CalibrationMeasurementUnit {
    /**
     * Bluetooth transmit power for describing expected RSSI at 1 metre for interpretation of measured RSSI value.
     */
    BLETransmitPower
}
