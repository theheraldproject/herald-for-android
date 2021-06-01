//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.datatype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Encounter record describing proximity with target at a moment in time
 */
public class Encounter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    @Nullable
    public Date timestamp = null;
    @Nullable
    public Proximity proximity = null;
    @Nullable
    public PayloadData payload = null;

    public Encounter(@Nullable final Proximity didMeasure, @Nullable final PayloadData withPayload, @Nullable final Date timestamp) {
        this.timestamp = timestamp;
        this.proximity = didMeasure;
        this.payload = withPayload;
    }

    public Encounter(@Nullable final Proximity didMeasure, @Nullable final PayloadData withPayload) {
        this(didMeasure, withPayload, new Date());
    }

    public Encounter(@NonNull final String row) {
        final String[] fields = row.split(",", -1);
        if (!(fields.length >= 6)) {
            return;
        }
        try {
            if (null != fields[0] && !fields[0].isEmpty()) {
                this.timestamp = dateFormatter.parse(fields[0]);
            }
        } catch (Throwable e) {
            // TODO report issue here
        }
        Calibration calibration = null;
        try {
            if (null != fields[3] && null != fields[4] && !fields[3].isEmpty() && !fields[4].isEmpty()) {
                final double calibrationValue = Double.parseDouble(fields[3]);
                final CalibrationMeasurementUnit calibrationUnit = CalibrationMeasurementUnit.valueOf(fields[4]);
                calibration = new Calibration(calibrationUnit, calibrationValue);
            }
        } catch (Throwable e) {
            // TODO report issue here
        }
        try {
            if (null != fields[1] && null != fields[2] && !fields[1].isEmpty() && !fields[2].isEmpty()) {
                final double proximityValue = Double.parseDouble(fields[1]);
                final ProximityMeasurementUnit proximityUnit = ProximityMeasurementUnit.valueOf(fields[2]);
                this.proximity = new Proximity(proximityUnit, proximityValue, calibration);
            }
        } catch (Throwable e) {
            // TODO report issue here
        }
        if (null != fields[5]) {
            this.payload = new PayloadData(fields[5]);
        }
    }

    @NonNull
    public String csvString() {
        final String f0 = (null == timestamp ? "" : dateFormatter.format(timestamp));
        //noinspection ConstantConditions
        final String f1 = (null == proximity || null == proximity.value ? "" : proximity.value.toString());
        //noinspection ConstantConditions
        final String f2 = (null == proximity || null == proximity.unit ? "" : proximity.unit.name());
        //noinspection ConstantConditions
        final String f3 = (null == proximity || null == proximity.calibration || null == proximity.calibration.value ? "" : proximity.calibration.value.toString());
        //noinspection ConstantConditions
        final String f4 = (null == proximity || null == proximity.calibration || null == proximity.calibration.unit ? "" : proximity.calibration.unit.name());
        final String f5 = (null == payload ? "" : payload.base64EncodedString());
        return f0 + "," + f1 + "," + f2 + "," + f3 + "," + f4 + "," + f5;
    }

    public boolean isValid() {
        return null != timestamp && null != proximity && null != payload;
    }
}
