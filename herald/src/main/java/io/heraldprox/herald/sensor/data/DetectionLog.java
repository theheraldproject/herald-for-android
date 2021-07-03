//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.data;

import android.content.Context;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.datatype.PayloadData;
import io.heraldprox.herald.sensor.datatype.SensorState;
import io.heraldprox.herald.sensor.datatype.SensorType;
import io.heraldprox.herald.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSV detection log for post event analysis and visualisation.
 */
public class DetectionLog extends SensorDelegateLogger {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.DetectionLog");
    @NonNull
    private final PayloadData payloadData;
    private final String deviceName = android.os.Build.MODEL;
    private final String deviceOS = Integer.toString(android.os.Build.VERSION.SDK_INT);
    private final Map<String, String> payloads = new ConcurrentHashMap<>();
    private final PayloadDataFormatter payloadDataFormatter;

    public DetectionLog(@NonNull final Context context, @NonNull final String filename, @NonNull final PayloadData payloadData, @NonNull final PayloadDataFormatter payloadDataFormatter) {
        super(context, filename);
        this.payloadData = payloadData;
        this.payloadDataFormatter = payloadDataFormatter;
        write();
    }

    public DetectionLog(@NonNull final Context context, @NonNull final String filename, @NonNull final PayloadData payloadData) {
        this(context, filename, payloadData, new ConcretePayloadDataFormatter());
    }

    private void write() {
        final StringBuilder content = new StringBuilder();
        content.append(csv(deviceName));
        content.append(',');
        content.append("Android");
        content.append(',');
        content.append(csv(deviceOS));
        content.append(',');
        content.append(csv(payloadDataFormatter.shortFormat(payloadData)));
        final List<String> payloadList = new ArrayList<>(payloads.size());
        for (final String payload : payloads.keySet()) {
            if (payload.equals(payloadDataFormatter.shortFormat(payloadData))) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (final String payload : payloadList) {
            content.append(',');
            content.append(payload);
        }
        logger.debug("write (content={})", content.toString());
        content.append("\n");
        overwrite(content.toString());
    }


    // MARK:- SensorDelegate


    @Override
    public void sensor(@NonNull SensorType sensor, @NonNull SensorState didUpdateState) {
        logger.debug("didUpdateState (state={})", didUpdateState);
        write();
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final PayloadData didRead, @NonNull final TargetIdentifier fromTarget) {
        if (null == payloads.put(payloadDataFormatter.shortFormat(didRead), fromTarget.value)) {
            logger.debug("didRead (payload={})", payloadDataFormatter.shortFormat(payloadData));
            write();
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
        for (final PayloadData payloadData : didShare) {
            if (null == payloads.put(payloadDataFormatter.shortFormat(payloadData), fromTarget.value)) {
                logger.debug("didShare (payload={})", payloadDataFormatter.shortFormat(payloadData));
                write();
            }
        }
    }
}
