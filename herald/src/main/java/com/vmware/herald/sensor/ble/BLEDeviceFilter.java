package com.vmware.herald.sensor.ble;

import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Data;

import java.util.HashMap;
import java.util.Map;

public class BLEDeviceFilter {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.ConcreteBLEDeviceFilter");
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training data
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    /// BLE device filter with in-memory lookup table
    public BLEDeviceFilter() {
        textFile = null;
    }

    /// BLE device filter with in-memory lookup table and persisted training data file
    public BLEDeviceFilter(Context context, String file) {
        textFile = new TextFile(context, file);
        if (textFile.empty()) {
            textFile.write("signature,feature,ignore");
        }
    }

    /// Extract feature data from signature data for classification.
    private Data extractFeatures(Data signatureData) {
        return signatureData;
    }

    /// Add training example to filter to create an adaptive filter.
    public synchronized void train(Data signatureData, boolean ignore) {
        if (signatureData == null) {
            return;
        }
        final Data featureData = extractFeatures(signatureData);
        ShouldIgnore shouldIgnore = samples.get(featureData);
        if (shouldIgnore == null) {
            shouldIgnore = new ShouldIgnore();
            samples.put(featureData, shouldIgnore);
        }
        if (ignore) {
            shouldIgnore.yes++;
        } else {
            shouldIgnore.no++;
        }
        // Write training sample to text file for analysis
        if (textFile != null) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(signatureData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(featureData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(ignore);
            textFile.write(stringBuilder.toString());
        }
        logger.debug("train (ignore={},feature={},signature={})", (ignore ? "Y" : "N"), featureData, signatureData);
    }

    /// Should the device be ignored based on signature data (e.g. manufacturer data)?
    public boolean ignore(Data signatureData) {
        // Do not ignore device without any signature data
        if (signatureData == null) {
            return false;
        }
        // Extract feature data from signature data and get should ignore statistics
        final Data featureData = extractFeatures(signatureData);
        ShouldIgnore shouldIgnore = samples.get(featureData);
        // Do not ignore previously unseen device
        if (shouldIgnore == null) {
            return false;
        }
        // Do not ignore device if there is even one example of it being legitimate
        if (shouldIgnore.no > 0) {
            return false;
        }
        // Ignore device if the signature has been registered for ignore more than twice
        if (shouldIgnore.yes > 2) {
            return true;
        }
        // Do not ignore device if no decision is reached
        return false;
    }
}
