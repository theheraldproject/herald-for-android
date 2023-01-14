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

    public DetectionLog(@NonNull final TextFile textFile, @NonNull final PayloadData payloadData) {
        super(textFile);
        this.payloadData = payloadData;
        this.payloadDataFormatter = new ConcretePayloadDataFormatter();
        write();
    }

    private void read() {
        final String content = contentsOf();
        if (null == content || content.isEmpty()) {
            return;
        }
        // Parse log file to read previously stored payloads
        final String[] data = content.trim().split(",");
        final List<String> storedPayloads = new ArrayList<>(data.length);
        // File format is : deviceName,osName,osVersion,selfPayload,targetPayload1,...,targetPayloadN
        // Target payloads start at index 4
        for (int i=4; i<data.length; i++) {
            if (null != data[i] && !data[i].isEmpty()) {
                storedPayloads.add(data[i].trim());
            }
        }
        // Register stored payloads as { payload -> "stored" } where "stored" is the target identifier
        // if payload is not in memory already (due to app restart).
        final List<String> mergedPayloads = new ArrayList<>(storedPayloads.size());
        for (final String targetPayload : storedPayloads) {
            if (payloads.containsKey(targetPayload)) {
                continue;
            }
            payloads.put(targetPayload, "stored");
            mergedPayloads.add(targetPayload);
        }
        Collections.sort(storedPayloads);
        Collections.sort(mergedPayloads);
        logger.debug("read (stored={},merged={})", storedPayloads.toString(), mergedPayloads.toString());
    }

    private void write() {
        // Read stored payloads to combine with payloads in memory
        read();
        // Write all payloads
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
        final String previousTarget = payloads.put(payloadDataFormatter.shortFormat(didRead), fromTarget.value);
        if (null == previousTarget || "stored".equals(previousTarget)) {
            logger.debug("didRead (payload={})", payloadDataFormatter.shortFormat(payloadData));
            write();
        }
    }

    @Override
    public void sensor(@NonNull final SensorType sensor, @NonNull final List<PayloadData> didShare, @NonNull final TargetIdentifier fromTarget) {
        for (final PayloadData payloadData : didShare) {
            final String previousTarget = payloads.put(payloadDataFormatter.shortFormat(payloadData), fromTarget.value);
            if (null == previousTarget || "stored".equals(previousTarget)) {
                logger.debug("didShare (payload={})", payloadDataFormatter.shortFormat(payloadData));
                write();
            }
        }
    }
}
