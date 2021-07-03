//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Calibration data for interpreting proximity value between sensor and target, e.g. Transmit power for BLE.
 */
public class Calibration {
    /**
     * Unit of measurement, e.g. transmit power
     */
    @NonNull
    public final CalibrationMeasurementUnit unit;
    /**
     * Measured value, e.g. transmit power in BLE advert
     */
    @NonNull
    public final Double value;

    public Calibration(@NonNull final CalibrationMeasurementUnit unit, @NonNull final Double value) {
        this.unit = unit;
        this.value = value;
    }

    /**
     * Get plain text description of calibration data
     * @return Description
     */
    @NonNull
    public String description() {
        return unit + ":" + value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
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
