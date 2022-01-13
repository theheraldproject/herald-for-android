//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.InertiaLocationReference;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

/**
 * CSV calibration log for post event analysis and visualisation. This is used for automated
 * analysis of RSSI over distance. The accelerometer data provides the signal for segmenting
 * data by distance when used on the cable car rig.
 */
public class CalibrationLog extends SensorDelegateLogger {

    public CalibrationLog(@NonNull final Context context, @NonNull final String filename) {
        super(context, filename);
    }

    public CalibrationLog(@NonNull final TextFile textFile) {
        super(textFile);
    }

    @Override
    protected String header() {
        return "time,payload,rssi,x,y,z";
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget, @NonNull final PayloadData withPayload) {
        write(Timestamp.timestamp() + "," + csv(withPayload.shortName()) + "," + didMeasure.value + ",,,");
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Location didVisit) {
        if (didVisit.value instanceof InertiaLocationReference) {
            final InertiaLocationReference reference = (InertiaLocationReference) didVisit.value;
            write(Timestamp.timestamp() + ",,," + reference.x + ","  + reference.y + "," + reference.z);
        }
    }
}
