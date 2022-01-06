//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.Location;
import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.Proximity;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.List;

/**
 * CSV contact log for post event analysis and visualisation.
 */
public class ContactLog extends SensorDelegateLogger {
    @NonNull
    private final PayloadDataFormatter payloadDataFormatter;

    public ContactLog(@NonNull final Context context, @NonNull final String filename, @NonNull final PayloadDataFormatter payloadDataFormatter) {
        super(context, filename);
        this.payloadDataFormatter = payloadDataFormatter;
    }

    public ContactLog(@NonNull final Context context, @NonNull final String filename) {
        this(context, filename, new ConcretePayloadDataFormatter());
    }

    @Override
    protected String header() {
        return "time,sensor,id,detect,read,measure,share,visit,data";
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final TargetIdentifier didDetect) {
        write(timestamp() + "," + sensor.name() + "," + csv(didDetect.value) + ",1,,,,,");
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget) {
        write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,2,,,," + csv(payloadDataFormatter.shortFormat(didRead)));
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
        final String prefix = timestamp() + "," + sensor.name() + "," + csv(fromTarget.value);
        for (PayloadData payloadData : didShare) {
            write(prefix + ",,,,4,," + csv(payloadDataFormatter.shortFormat(payloadData)));
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Proximity didMeasure, @NonNull final TargetIdentifier fromTarget) {
        write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,,3,,," + csv(didMeasure.description()));
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final Location didVisit) {
        write(timestamp() + "," + sensor.name() + ",,,,,,5," + csv(didVisit.description()));
    }
}
