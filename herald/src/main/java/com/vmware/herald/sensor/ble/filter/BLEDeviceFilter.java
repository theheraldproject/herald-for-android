//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
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

import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Device filter for avoiding connection to devices that definitely cannot
/// host sensor services.
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
    private final List<FilterPattern> filterPatterns;
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training samples
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    // Pattern for filtering device based on message content
    public final static class FilterPattern {
        public final String regularExpression;
        public final Pattern pattern;
        public FilterPattern(final String regularExpression, final Pattern pattern) {
            this.regularExpression = regularExpression;
            this.pattern = pattern;
        }
    }

    // Match of a filter pattern
    public final static class MatchingPattern {
        public final FilterPattern filterPattern;
        public final String message;
        public MatchingPattern(FilterPattern filterPattern, String message) {
            this.filterPattern = filterPattern;
            this.message = message;
        }
    }

    /// BLE device filter for matching devices against filters defined
    /// in BLESensorConfiguration.deviceFilterFeaturePatterns.
    public BLEDeviceFilter() {
        this(null, null, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /// BLE device filter for matching devices against BLESensorConfiguration.deviceFilterFeaturePatterns
    /// and writing advert data to file for analysis.
    public BLEDeviceFilter(final Context context, final String file) {
        this(context, file, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /// BLE device filter for matching devices against the given set of patterns
    /// and writing advert data to file for analysis.
    public BLEDeviceFilter(final Context context, final String file, final String[] patterns) {
        if (context == null || file == null) {
            textFile = null;
        } else {
            textFile = new TextFile(context, file);
            if (textFile.empty()) {
                textFile.write("time,ignore,featureData,scanRecordRawData,identifier,rssi,deviceModel,deviceName");
            }
        }
        if (BLESensorConfiguration.deviceFilterTrainingEnabled || patterns == null || patterns.length == 0) {
            filterPatterns = null;
        } else {
            filterPatterns = compilePatterns(patterns);
        }
    }

    // MARK:- Pattern matching functions
    // Using regular expression over hex representation of feature data for maximum flexibility and usability

    /// Match message against all patterns in sequential order, returns matching pattern or null
    protected static FilterPattern match(final List<FilterPattern> filterPatterns, final String message) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        if (message == null) {
            return null;
        }
        for (final FilterPattern filterPattern : filterPatterns) {
            try {
                final Matcher matcher = filterPattern.pattern.matcher(message);
                if (matcher.find()) {
                    return filterPattern;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    /// Compile regular expressions into patterns.
    protected static List<FilterPattern> compilePatterns(final String[] regularExpressions) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        final List<FilterPattern> filterPatterns = new ArrayList<>(regularExpressions.length);
        for (final String regularExpression : regularExpressions) {
            try {
                final Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                if (regularExpression != null && !regularExpression.isEmpty() && pattern != null) {
                    final FilterPattern filterPattern = new FilterPattern(regularExpression, pattern);
                    filterPatterns.add(filterPattern);
                } else {
                    logger.fault("compilePatterns, invalid filter pattern (regularExpression={})", regularExpression);
                }
            } catch (Throwable e) {
                logger.fault("compilePatterns, invalid filter pattern (regularExpression={})", regularExpression);
            }
        }
        return filterPatterns;
    }

    /// Extract messages from manufacturer specific data
    protected final static List<Data> extractMessages(final byte[] rawScanRecordData) {
        // Parse raw scan record data in scan response data
        if (rawScanRecordData == null || rawScanRecordData.length == 0) {
            return null;
        }
        final BLEScanResponseData bleScanResponseData = BLEAdvertParser.parseScanResponse(rawScanRecordData, 0);
        // Parse scan response data into manufacturer specific data
        if (bleScanResponseData == null || bleScanResponseData.segments == null || bleScanResponseData.segments.isEmpty()) {
            return null;
        }
        final List<BLEAdvertManufacturerData> bleAdvertManufacturerDataList = BLEAdvertParser.extractManufacturerData(bleScanResponseData.segments);
        // Parse manufacturer specific data into messages
        if (bleAdvertManufacturerDataList == null || bleAdvertManufacturerDataList.isEmpty()) {
            return null;
        }
        final List<BLEAdvertAppleManufacturerSegment> bleAdvertAppleManufacturerSegments = BLEAdvertParser.extractAppleManufacturerSegments(bleAdvertManufacturerDataList);
        // Convert segments to messages
        if (bleAdvertAppleManufacturerSegments == null || bleAdvertAppleManufacturerSegments.isEmpty()) {
            return null;
        }
        final List<Data> messages = new ArrayList<>(bleAdvertAppleManufacturerSegments.size());
        for (BLEAdvertAppleManufacturerSegment segment : bleAdvertAppleManufacturerSegments) {
            if (segment != null && segment.raw != null && segment.raw.value.length > 0) {
                messages.add(segment.raw);
            }
        }
        return messages;
    }

    // MARK:- Filtering functions

    /// Extract feature data from scan record
    private List<Data> extractFeatures(final ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        // Get message data
        final List<Data> featureList = new ArrayList<>();
        final List<Data> messages = extractMessages(scanRecord.getBytes());
        if (messages != null) {
            featureList.addAll(messages);
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

    /// Match filter patterns against data items, returning the first match
    protected final static MatchingPattern match(final List<FilterPattern> patternList, final Data rawData) {
        // No pattern to match against
        if (patternList == null || patternList.isEmpty()) {
            return null;
        }
        // Empty raw data
        if (rawData == null || rawData.value == null || rawData.value.length == 0) {
            return null;
        }
        // Extract messages
        final List<Data> messages = extractMessages(rawData.value);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (Data message : messages) {
            if (message == null) {
                continue;
            }
            try {
                final String hexEncodedString = message.hexEncodedString();
                final FilterPattern pattern = match(patternList, hexEncodedString);
                if (pattern != null) {
                    return new MatchingPattern(pattern, hexEncodedString);
                }
            } catch (Throwable e) {
                // Errors are acceptable
            }
        }
        return null;
    }

    /// Match scan record messages against all registered patterns, returns matching pattern or null.
    public MatchingPattern match(final BLEDevice device) {
        try {
            final ScanRecord scanRecord = device.scanRecord();
            // Cannot match device without any scan record data
            if (scanRecord == null) {
                return null;
            }
            // Cannot match scan record where data is null
            final byte[] bytes = scanRecord.getBytes();
            if (bytes == null) {
                return null;
            }
            final Data rawData = new Data(bytes);
            // Attempt to match
            final MatchingPattern matchingPattern = match(filterPatterns, rawData);
            if (matchingPattern == null || matchingPattern.filterPattern == null || matchingPattern.filterPattern.pattern == null || matchingPattern.filterPattern.regularExpression == null || matchingPattern.message == null) {
                return null;
            } else {
                return matchingPattern;
            }
        } catch (Throwable e) {
            logger.fault("match, unknown error (device={},scanRecord={})", device, device.scanRecord());
            return null;
        }
    }

    /// Should the device be ignored based on scan record data?
    private boolean ignoreBasedOnStatistics(final BLEDevice device) {
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
