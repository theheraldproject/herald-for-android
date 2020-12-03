//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Objects;

/// Raw data for estimating proximity between sensor and target, e.g. RSSI for BLE.
public class Proximity {
    /// Unit of measurement, e.g. RSSI
    public final ProximityMeasurementUnit unit;
    /// Measured value, e.g. raw RSSI value.
    public final Double value;
    /// Calibration data (optional), e.g. transmit power
    public final Calibration calibration;

    public Proximity(ProximityMeasurementUnit unit, Double value) {
        this(unit, value, null);
    }

    public Proximity(ProximityMeasurementUnit unit, Double value, Calibration calibration) {
        this.unit = unit;
        this.value = value;
        this.calibration = calibration;
    }

    /// Get plain text description of proximity data
    public String description() {
        if (calibration == null) {
            return unit + ":" + value;
        }
        return unit + ":" + value + "[" + calibration.description() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
