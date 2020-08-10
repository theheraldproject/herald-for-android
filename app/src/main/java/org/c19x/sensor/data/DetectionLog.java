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
    private final PayloadData payloadData;
    private final String deviceName = android.os.Build.MODEL;
    private final String deviceOS = Integer.toString(android.os.Build.VERSION.SDK_INT);
    private final Map<String, String> payloads = new ConcurrentHashMap<>();

    public DetectionLog(String filename, PayloadData payloadData) {
        textFile = new TextFile(filename);
        this.payloadData = payloadData;
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
        final String device = deviceName + " (Android " + deviceOS + ")";
        final List<String> payloadList = new ArrayList<>(payloads.size());
        for (String payloadDataShortName : payloads.keySet()) {
            payloadList.add(payloadDataShortName);
        }
        Collections.sort(payloadList);
        final StringBuilder content = new StringBuilder();
        content.append(csv(device));
        content.append(",id=");
        content.append(payloadData.shortName());
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
        if (payloads.put(didRead.shortName(), fromTarget.value) == null) {
            logger.debug("didRead (payload={})", payloadData.shortName());
            write();
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        for (PayloadData payloadData : didShare) {
            if (payloads.put(payloadData.shortName(), fromTarget.value) == null) {
                logger.debug("didShare (payload={})", payloadData.shortName());
                write();
            }
        }
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
    }
}
