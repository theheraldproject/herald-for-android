//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.DefaultSensorDelegate;
import io.heraldprox.herald.sensor.datatype.InertiaLocationReference;
import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/// CSV contact log for post event analysis and visualisation
public class CalibrationLog extends DefaultSensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    @NonNull
    private final TextFile textFile;

    public CalibrationLog(@NonNull final Context context, @NonNull final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,payload,rssi,x,y,z");
        }
    }

    @NonNull
    private static String timestamp() {
        return dateFormatter.format(new Date());
    }

    @NonNull
    private static String csv(@NonNull String value) {
        return TextFile.csv(value);
    }

    // MARK:- SensorDelegate


    @Override
    public void sensor(SensorType sensor, @NonNull Proximity didMeasure, TargetIdentifier fromTarget, @NonNull PayloadData withPayload) {
        textFile.write(timestamp() + "," + csv(withPayload.shortName()) + "," + didMeasure.value + ",,,");
    }

    @Override
    public void sensor(SensorType sensor, @NonNull Location didVisit) {
        if (didVisit.value instanceof InertiaLocationReference) {
            final InertiaLocationReference reference = (InertiaLocationReference) didVisit.value;
            textFile.write(timestamp() + ",,," + reference.x + ","  + reference.y + "," + reference.z);
        }
    }
}
