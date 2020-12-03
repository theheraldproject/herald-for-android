//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calibration that = (Calibration) o;
        return unit == that.unit &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, value);
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
