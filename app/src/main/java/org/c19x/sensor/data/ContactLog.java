package org.c19x.sensor.data;

import org.c19x.sensor.SensorDelegate;
import org.c19x.sensor.datatype.Location;
import org.c19x.sensor.datatype.PayloadData;
import org.c19x.sensor.datatype.Proximity;
import org.c19x.sensor.datatype.SensorType;
import org.c19x.sensor.datatype.TargetIdentifier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/// CSV contact log for post event analysis and visualisation
public class ContactLog implements SensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final TextFile textFile;

    public ContactLog(String filename) {
        textFile = new TextFile(filename);
        if (textFile.empty()) {
            textFile.write("time,sensor,id,detect,read,measure,share,visit,data");
        }
    }

    private String timestamp() {
        final String timestamp = dateFormatter.format(new Date());
        return timestamp;
    }

    private String csv(String value) {
        if (value.contains(",")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }

    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(didDetect.value) + ",1,,,,,");
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,2,,,," + csv(didRead.description()));
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        final String prefix = timestamp() + "," + sensor.name() + "," + csv(fromTarget.value);
        final StringBuilder payloads = new StringBuilder();
        payloads.append('[');
        for (int i = 0; i < didShare.size(); i++) {
            if (i > 0) {
                payloads.append(',');
            }
            payloads.append(didShare.get(i).description());
        }
        payloads.append(']');
        textFile.write(prefix + ",,,,4,," + csv(payloads.toString()));
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        textFile.write(timestamp() + "," + sensor.name() + "," + csv(fromTarget.value) + ",,,3,,," + csv(didMeasure.description()));
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        textFile.write(timestamp() + "," + sensor.name() + ",,,,,,5," + csv(didVisit.description()));
    }
}
