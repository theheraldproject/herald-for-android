//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import androidx.annotation.NonNull;

/// Calibration data for interpreting proximity value between sensor and target, e.g. Transmit power for BLE.
public class Calibration {
    /// Unit of measurement, e.g. transmit power
    public final CalibrationMeasurementUnit unit;
    /// Measured value, e.g. transmit power in BLE advert
    public final Double value;

    public Calibration(CalibrationMeasurementUnit unit, Double value) {
        this.unit = unit;
        this.value = value;
    }

    /// Get plain text description of calibration data
    public String description() {
        return unit + ":" + value;
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
