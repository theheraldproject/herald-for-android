//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.sensor.data;

import android.content.Context;

import com.vmware.herald.sensor.DefaultSensorDelegate;
import com.vmware.herald.sensor.datatype.InertiaLocationReference;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/// CSV contact log for post event analysis and visualisation
public class CalibrationLog extends DefaultSensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final TextFile textFile;

    public CalibrationLog(final Context context, final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,payload,rssi,x,y,z");
        }
    }

    private static String timestamp() {
        return dateFormatter.format(new Date());
    }

    private static String csv(String value) {
        return TextFile.csv(value);
    }

    // MARK:- SensorDelegate


    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        textFile.write(timestamp() + "," + csv(withPayload.shortName()) + "," + didMeasure.value + ",,,");
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        if (didVisit.value instanceof InertiaLocationReference) {
            final InertiaLocationReference reference = (InertiaLocationReference) didVisit.value;
            textFile.write(timestamp() + ",,," + reference.x + ","  + reference.y + "," + reference.z);
        }
    }
}
