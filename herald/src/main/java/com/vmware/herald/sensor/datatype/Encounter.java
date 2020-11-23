//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.datatype;

import java.text.SimpleDateFormat;
import java.util.Date;

/// Encounter record describing proximity with target at a moment in time
public class Encounter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public Date timestamp = null;
    public Proximity proximity = null;
    public PayloadData payload = null;

    public Encounter(Proximity didMeasure, PayloadData withPayload, Date timestamp) {
        this.timestamp = timestamp;
        this.proximity = didMeasure;
        this.payload = withPayload;
    }

    public Encounter(Proximity didMeasure, PayloadData withPayload) {
        this(didMeasure, withPayload, new Date());
    }

    public Encounter(String row) {
        final String[] fields = row.split(",");
        if (!(fields.length >= 6)) {
            return;
        }
        try {
            this.timestamp = dateFormatter.parse(fields[0]);
        } catch (Throwable e) {
        }
        Calibration calibration = null;
        try {
            final double calibrationValue = Double.parseDouble(fields[3]);
            final CalibrationMeasurementUnit calibrationUnit = CalibrationMeasurementUnit.valueOf(fields[4]);
            calibration = new Calibration(calibrationUnit, calibrationValue);
        } catch (Throwable e) {
        }
        try {
            final double proximityValue = Double.parseDouble(fields[1]);
            final ProximityMeasurementUnit proximityUnit = ProximityMeasurementUnit.valueOf(fields[2]);
            this.proximity = new Proximity(proximityUnit, proximityValue, calibration);
        } catch (Throwable e) {
        }
        this.payload = new PayloadData(fields[5]);
    }

    public String csvString() {
        final String f0 = dateFormatter.format(timestamp);
        final String f1 = proximity.value.toString();
        final String f2 = proximity.unit.name();
        final String f3 = (proximity.calibration == null || proximity.calibration.value == null ? "" : proximity.calibration.value.toString());
        final String f4 = (proximity.calibration == null || proximity.calibration.unit == null ? "" : proximity.calibration.unit.name());
        final String f5 = payload.base64EncodedString();
        return f0 + "," + f1 + "," + f2 + "," + f3 + "," + f4 + "," + f5;
    }

    public boolean isValid() {
        return timestamp != null && proximity != null && payload != null;
    }
}
