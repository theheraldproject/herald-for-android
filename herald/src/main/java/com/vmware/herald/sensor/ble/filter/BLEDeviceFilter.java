//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: MIT
//

package com.vmware.herald.sensor.ble.filter;

import android.bluetooth.le.ScanRecord;
import android.content.Context;

import com.vmware.herald.sensor.ble.BLEDevice;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.TextFile;
import com.vmware.herald.sensor.datatype.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Adaptive filter to ignoring devices that have repeatedly failed connection or does
/// not advertise sensor services.
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
            textFile.write("time,ignore,featureData,scanRecordRawData,identifier,rssi,deviceModel,deviceName");
        }
    }

    /// Extract all manufacturer data segments from raw scan record data
    protected final static List<Data> extractManufacturerData(final Data raw) {
        final List<Data> segments = new ArrayList<>(1);
        // Scan raw data to search for "FF4C00" and use preceding 1-byte to establish length of segment
        for (int i=0; i<raw.value.length-2; i++) {
            try {
                // Search for "FF4C00"
                if (raw.value[i] == (byte) 0xFF && raw.value[i+1] == (byte) 0x4C && raw.value[i+2] == (byte) 0x00) {
                    // Extract segment based on 1-byte length value preceding "FF4C00"
                    final int lengthOfManufacturerDataSegment = raw.value[i-1] & 0xFF;
                    final Data segment = raw.subdata(i+3, lengthOfManufacturerDataSegment - 3);
                    segments.add(segment);
                }
            } catch (Throwable e) {
                // Errors are expected due to parsing errors and corrupted data
            }
        }
        return segments;
    }

    /// Extract all messages from manufacturer data segments
    protected final static List<Data> extractMessageData(final List<Data> manufacturerData) {
        final List<Data> messages = new ArrayList<>();
        for (Data segment : manufacturerData) {
            try {
                // "01" marks legacy service UUID encoding
                if (segment.value[0] == (byte) 0x01) {
                    messages.add(segment);
                }
                // Assume all other prefixes mark new messages "Type:Length:Data"
                else {
                    final byte[] raw = segment.value;
                    for (int i=0; i<raw.length - 1; i++) {
                        // Type (1-byte), Length (1-byte), Data
                        final int lengthOfMessage = raw[i+1] & 0xFF;
                        final Data message = segment.subdata(i, lengthOfMessage + 2);
                        if (message != null) {
                            messages.add(message);
                        }
                        i += (lengthOfMessage + 1);
                    }
                }
            } catch (Throwable e) {
                // Errors are expected due to parsing errors and corrupted data
            }
        }
        return messages;
    }

    /// Extract feature data from scan record
    private List<Data> extractFeatures(final ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        // Get message data
        final List<Data> featureList = new ArrayList<>();
        final byte[] rawData = scanRecord.getBytes();
        if (rawData != null) {
            final List<Data> manufacturerData = extractManufacturerData(new Data(rawData));
            final List<Data> messageData = extractMessageData(manufacturerData);
            featureList.addAll(messageData);
        }
        return featureList;
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
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return;
        }
        // Update ignore yes/no counts for feature data
        for (Data featureData : featureList) {
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
            logger.debug("train (ignore={},feature={},scanRecord={},device={})", (ignore ? "Y" : "N"), featureData.hexEncodedString(), scanRecordData.hexEncodedString(), device.description());
            // Write sample to text file for analysis
            if (textFile == null) {
                return;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('"');
            stringBuilder.append(dateFormatter.format(new Date()));
            stringBuilder.append('"');
            stringBuilder.append(',');
            stringBuilder.append(ignore ? 'Y' : 'N');
            stringBuilder.append(',');
            stringBuilder.append(featureData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(scanRecordData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(device.identifier.value);
            stringBuilder.append(',');
            if (device.rssi() != null) {
                stringBuilder.append(device.rssi().value);
            }
            stringBuilder.append(',');
            if (device.model() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.model());
                stringBuilder.append('"');
            }
            stringBuilder.append(',');
            if (device.deviceName() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.deviceName());
                stringBuilder.append('"');
            }
            textFile.write(stringBuilder.toString());
        }
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
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return false;
        }
        for (Data featureData : featureList) {
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
        }
        // Do not ignore device if no decision is reached based on existing rules
        return false;
    }
}
