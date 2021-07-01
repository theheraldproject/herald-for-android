//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Raw data for estimating proximity between sensor and target, e.g. RSSI for BLE.
 */
public class Proximity {
    /**
     * Unit of measurement, e.g. RSSI
     */
    @NonNull
    public final ProximityMeasurementUnit unit;
    /**
     * Measured value, e.g. raw RSSI value.
     */
    @NonNull
    public final Double value;
    /**
     * Calibration data (optional), e.g. transmit power
     */
    @Nullable
    public final Calibration calibration;

    public Proximity(@NonNull final ProximityMeasurementUnit unit, @NonNull final Double value) {
        this(unit, value, null);
    }

    public Proximity(@NonNull final ProximityMeasurementUnit unit, @NonNull final Double value, @Nullable final Calibration calibration) {
        this.unit = unit;
        this.value = value;
        this.calibration = calibration;
    }

    /**
     * Get plain text description of proximity data
     * @return Description
     */
    @NonNull
    public String description() {
        if (null == calibration) {
            return unit + ":" + value;
        }
        return unit + ":" + value + "[" + calibration.description() + "]";
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        Proximity proximity = (Proximity) o;
        return unit == proximity.unit &&
                Objects.equals(value, proximity.value) &&
                Objects.equals(calibration, proximity.calibration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, value, calibration);
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
