package com.vmware.herald.sensor.ble;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Data;

import java.util.HashMap;
import java.util.Map;

/// Adaptive filter to ignoring devices that have repeatedly failed connection or does
/// not advertise sensor services.
public class BLEDeviceFilter {
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training samples
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    /// BLE device filter with in-memory lookup table
    public BLEDeviceFilter() {
        textFile = null;
    }

    /// BLE device filter with in-memory lookup table and persisted training data file
    public BLEDeviceFilter(final Context context, final String file) {
        textFile = new TextFile(context, file);
        if (textFile.empty()) {
            textFile.write("ignore,featureData,scanRecordRawData,identifier");
        }
    }

    /// Extract feature data from scan record
    private Data extractFeatures(final ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        // Get manufacturer data
        final byte[] manufacturerSpecificData = scanRecord.getManufacturerSpecificData(BLESensorConfiguration.manufacturerIdForApple);
        if (manufacturerSpecificData == null) {
            return null;
        }
        final Data featureData = new Data(manufacturerSpecificData);
        return featureData;
    }

    /// Add training example to adaptive filter.
    public synchronized void train(final BLEDevice device, final boolean ignore) {
        final ScanRecord scanRecord = device.scanRecord();
        // Get feature data from scan record
        if (scanRecord == null) {
            return;
        }
        final Data scanRecordData = (scanRecord.getBytes() == null ? null : new Data(scanRecord.getBytes()));
        if (scanRecordData == null) {
            return;
        }
        final Data featureData = extractFeatures(scanRecord);
        if (featureData == null) {
            return;
        }
        // Update ignore yes/no counts for feature data
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
        logger.debug("train (ignore={},feature={},scanRecord={},identifier={})", (ignore ? "Y" : "N"), featureData.hexEncodedString(), scanRecordData.hexEncodedString(), device.identifier);
        // Write sample to text file for analysis
        if (textFile == null) {
            return;
        }
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ignore ? 'Y' : 'N');
        stringBuilder.append(',');
        stringBuilder.append(featureData.hexEncodedString());
        stringBuilder.append(',');
        stringBuilder.append(scanRecordData.hexEncodedString());
        stringBuilder.append(',');
        stringBuilder.append(device.identifier.value);
        textFile.write(stringBuilder.toString());
    }

    /// Should the device be ignored based on scan record data?
    public boolean ignore(final BLEDevice device) {
        final ScanRecord scanRecord = device.scanRecord();
        // Do not ignore device without any scan record data
        if (scanRecord == null) {
            return false;
        }
        // Extract feature data from scan record
        // Do not ignore device without any feature data
        final Data featureData = extractFeatures(scanRecord);
        if (featureData == null) {
            return false;
        }
        // Get training example statistics
        final ShouldIgnore shouldIgnore = samples.get(featureData);
        // Do not ignore device based on unknown feature data
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
        // Do not ignore device if no decision is reached based on existing rules
        return false;
    }
}
