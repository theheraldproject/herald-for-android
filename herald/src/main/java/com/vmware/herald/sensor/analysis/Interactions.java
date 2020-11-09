package com.vmware.herald.sensor.analysis;

import android.content.Context;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Encounter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/// Log of interactions for recording encounters (time, proximity, and identity).
/// This is can be used as basis for maintaining a persistent log
/// of encounters for on-device or centralised matching.
public class Interactions {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "Analysis.EncounterLog");
    private final TextFile textFile;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<Encounter> encounters = new ArrayList<>();

    public Interactions() {
        textFile = null;
    }

    public Interactions(final Context context, final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,proximity,unit,payload");
        } else {
            final String content = textFile.contentsOf();
            for (String line : content.split("\n")) {
                final Encounter encounter = new Encounter(line);
                if (encounter.isValid()) {
                    encounters.add(encounter);
                }
            }
            logger.debug("Loaded historic encounters (count={})", encounters.size());
        }
    }
}
