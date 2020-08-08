package org.c19x.sensor.data;

import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.datatype.Location;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// CSV contact log for post event analysis and visualisation
public class DetectionLog implements SensorDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.DetectionLog");
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final TextFile textFile;
    private final String payloadString;
    private final int prefixLength;
    private final String deviceName = android.os.Build.MODEL;
    private final String deviceOS = Integer.toString(android.os.Build.VERSION.SDK_INT);
    private final Map<String, String> payloads = new ConcurrentHashMap<>();

    public DetectionLog(String filename, String payloadString, int prefixLength) {
        textFile = new TextFile(filename);
        this.payloadString = payloadString;
        this.prefixLength = prefixLength;
    }

    private String timestamp() {
        return dateFormatter.format(new Date());
    }

    private String csv(String value) {
        if (value.contains(",")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }

    private void write() {
        final String device = deviceName + "(Android " + deviceOS + ")";
        final String payloadPrefix = payloadString.substring(0, Math.min(prefixLength, payloadString.length()));
        final List<String> payloadList = new ArrayList<>(payloads.size());
        for (String payload : payloads.keySet()) {
            payloadList.add(payload.substring(0, Math.min(prefixLength, payloadString.length())));
        }
        Collections.sort(payloadList);
        final StringBuilder content = new StringBuilder();
        content.append(csv(device));
        content.append(",id=");
        content.append(payloadPrefix);
        for (String payload : payloadList) {
            content.append(',');
            content.append(payload);
        }
        content.append("\n");
        textFile.overwrite(content.toString());
        logger.debug("write (content={})", content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        final String payload = didRead.base64EncodedString();
        if (payloads.put(payload, fromTarget.value) == null) {
            logger.debug("didRead (payload={})", payload);
            write();
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        for (PayloadData data : didShare) {
            final String payload = data.base64EncodedString();
            if (payloads.put(payload, fromTarget.value) == null) {
                logger.debug("didShare (payload={})", payload);
                write();
            }
        }
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
    }
}
