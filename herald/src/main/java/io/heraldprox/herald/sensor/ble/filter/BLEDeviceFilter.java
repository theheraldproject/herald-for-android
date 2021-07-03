//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import android.bluetooth.le.ScanRecord;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.ble.BLEDevice;
import io.heraldprox.herald.sensor.ble.BLESensorConfiguration;
import io.heraldprox.herald.sensor.data.ConcreteSensorLogger;
import io.heraldprox.herald.sensor.data.SensorLogger;
import io.heraldprox.herald.sensor.data.TextFile;
import io.heraldprox.herald.sensor.datatype.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Device filter for avoiding connection to devices that definitely cannot host sensor services.
 */
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
    static {
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
    @Nullable
    private final List<FilterPattern> filterPatterns;
    @Nullable
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // Counter for training samples
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    // Pattern for filtering device based on message content
    public final static class FilterPattern {
        @NonNull
        public final String regularExpression;
        @NonNull
        public final Pattern pattern;

        public FilterPattern(@NonNull final String regularExpression, @NonNull final Pattern pattern) {
            this.regularExpression = regularExpression;
            this.pattern = pattern;
        }
    }

    // Match of a filter pattern
    public final static class MatchingPattern {
        @NonNull
        public final FilterPattern filterPattern;
        @NonNull
        public final String message;

        public MatchingPattern(@NonNull final FilterPattern filterPattern, @NonNull final String message) {
            this.filterPattern = filterPattern;
            this.message = message;
        }
    }

    /**
     * BLE device filter for matching devices against filters defined in
     * BLESensorConfiguration.deviceFilterFeaturePatterns.
     */
    public BLEDeviceFilter() {
        this(null, null, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /**
     * BLE device filter for matching devices against BLESensorConfiguration.deviceFilterFeaturePatterns
     * and writing advert data to file for analysis.
     * @param context Application context
     * @param file Target file
     */
    public BLEDeviceFilter(@Nullable final Context context, @Nullable final String file) {
        this(context, file, BLESensorConfiguration.deviceFilterFeaturePatterns);
    }

    /**
     * BLE device filter for matching devices against the given set of patterns and writing
     * advert data to file for analysis.
     * @param context Application context
     * @param file Target file
     * @param patterns Patterns for matching
     */
    public BLEDeviceFilter(@Nullable final Context context, @Nullable final String file, @Nullable final String[] patterns) {
        if (null == context || null == file) {
            textFile = null;
        } else {
            textFile = new TextFile(context, file);
            if (textFile.empty()) {
                textFile.write("time,ignore,featureData,scanRecordRawData,identifier,rssi,deviceModel,deviceName");
            }
        }
        if (BLESensorConfiguration.deviceFilterTrainingEnabled || null == patterns || 0 == patterns.length) {
            filterPatterns = null;
        } else {
            filterPatterns = compilePatterns(patterns);
        }
    }

    // MARK:- Pattern matching functions
    // Using regular expression over hex representation of feature data for maximum flexibility and usability

    /**
     * Match message against all patterns in sequential order, returns matching pattern or null
     * @param filterPatterns Filter patterns
     * @param message Message for matching against patterns
     * @return First matching pattern, or null if no match was found
     */
    @Nullable
    protected static FilterPattern match(@NonNull final List<FilterPattern> filterPatterns, @Nullable final String message) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        if (null == message) {
            return null;
        }
        for (final FilterPattern filterPattern : filterPatterns) {
            try {
                final Matcher matcher = filterPattern.pattern.matcher(message);
                if (matcher.find()) {
                    return filterPattern;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Compile regular expressions into patterns.
     * @param regularExpressions Regular expressions
     * @return Filter patterns
     */
    @NonNull
    protected static List<FilterPattern> compilePatterns(@NonNull final String[] regularExpressions) {
        final SensorLogger logger = new ConcreteSensorLogger("Sensor", "BLE.BLEDeviceFilter");
        final List<FilterPattern> filterPatterns = new ArrayList<>(regularExpressions.length);
        for (final String regularExpression : regularExpressions) {
            try {
                final Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                //noinspection ConstantConditions
                if (null != regularExpression && !regularExpression.isEmpty() && null != pattern) {
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

    /**
     * Extract messages from manufacturer specific data.
     * @param rawScanRecordData Raw data
     * @return Messages
     */
    @Nullable
    protected static List<Data> extractMessages(@Nullable final byte[] rawScanRecordData) {
        // Parse raw scan record data in scan response data
        if (null == rawScanRecordData || 0 == rawScanRecordData.length) {
            return null;
        }
        final BLEScanResponseData bleScanResponseData = BLEAdvertParser.parseScanResponse(rawScanRecordData, 0);
        // Parse scan response data into manufacturer specific data
        //noinspection ConstantConditions
        if (null == bleScanResponseData || null == bleScanResponseData.segments || bleScanResponseData.segments.isEmpty()) {
            return null;
        }
        final List<BLEAdvertManufacturerData> bleAdvertManufacturerDataList = BLEAdvertParser.extractManufacturerData(bleScanResponseData.segments);
        // Parse manufacturer specific data into messages
        //noinspection ConstantConditions
        if (null == bleAdvertManufacturerDataList || bleAdvertManufacturerDataList.isEmpty()) {
            return null;
        }
        final List<BLEAdvertAppleManufacturerSegment> bleAdvertAppleManufacturerSegments = BLEAdvertParser.extractAppleManufacturerSegments(bleAdvertManufacturerDataList);
        // Convert segments to messages
        //noinspection ConstantConditions
        if (null == bleAdvertAppleManufacturerSegments || bleAdvertAppleManufacturerSegments.isEmpty()) {
            return null;
        }
        final List<Data> messages = new ArrayList<>(bleAdvertAppleManufacturerSegments.size());
        for (final BLEAdvertAppleManufacturerSegment segment : bleAdvertAppleManufacturerSegments) {
            //noinspection ConstantConditions
            if (null != segment && null != segment.raw && segment.raw.value.length > 0) {
                messages.add(segment.raw);
            }
        }
        return messages;
    }

    /**
     * Extract feature data from scan record.
     * @param scanRecord Scan record
     * @return Feature data
     */
    @Nullable
    private List<Data> extractFeatures(@Nullable final ScanRecord scanRecord) {
        if (null == scanRecord) {
            return null;
        }
        // Get message data
        final List<Data> featureList = new ArrayList<>();
        final List<Data> messages = extractMessages(scanRecord.getBytes());
        if (null != messages) {
            featureList.addAll(messages);
        }
        return featureList;
    }

    /**
     * Add training example to adaptive filter.
     * @param device Example device
     * @param ignore Should this device be ignored
     */
    public synchronized void train(@NonNull final BLEDevice device, final boolean ignore) {
        final ScanRecord scanRecord = device.scanRecord();
        // Get feature data from scan record
        if (null == scanRecord) {
            return;
        }
        final Data scanRecordData = (null == scanRecord.getBytes() ? null : new Data(scanRecord.getBytes()));
        if (null == scanRecordData) {
            return;
        }
        final List<Data> featureList = extractFeatures(scanRecord);
        if (null == featureList) {
            return;
        }
        // Update ignore yes/no counts for feature data
        for (final Data featureData : featureList) {
            ShouldIgnore shouldIgnore = samples.get(featureData);
            if (null == shouldIgnore) {
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
            if (null == textFile) {
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
            if (null != device.rssi()) {
                //noinspection ConstantConditions
                stringBuilder.append(device.rssi().value);
            }
            stringBuilder.append(',');
            if (null != device.model()) {
                stringBuilder.append('"');
                stringBuilder.append(device.model());
                stringBuilder.append('"');
            }
            stringBuilder.append(',');
            if (null != device.deviceName()) {
                stringBuilder.append('"');
                stringBuilder.append(device.deviceName());
                stringBuilder.append('"');
            }
            textFile.write(stringBuilder.toString());
        }
    }

    /**
     * Match filter patterns against data items, returning the first match
     * @param patternList List of patterns
     * @param rawData Raw data
     * @return First matching pattern
     */
    @Nullable
    protected static MatchingPattern match(@Nullable final List<FilterPattern> patternList, @Nullable final Data rawData) {
        // No pattern to match against
        if (null == patternList || patternList.isEmpty()) {
            return null;
        }
        // Empty raw data
        //noinspection ConstantConditions
        if (null == rawData || null == rawData.value || 0 == rawData.value.length) {
            return null;
        }
        // Extract messages
        final List<Data> messages = extractMessages(rawData.value);
        if (null == messages || messages.isEmpty()) {
            return null;
        }
        for (final Data message : messages) {
            if (null == message) {
                continue;
            }
            try {
                final String hexEncodedString = message.hexEncodedString();
                final FilterPattern pattern = match(patternList, hexEncodedString);
                if (null != pattern) {
                    return new MatchingPattern(pattern, hexEncodedString);
                }
            } catch (Throwable e) {
                // Errors are acceptable
            }
        }
        return null;
    }

    /**
     * Match scan record messages against all registered patterns, returns matching pattern or null.
     * @param device BLE device
     * @return Matching pattern, or null if none is found
     */
    @Nullable
    public MatchingPattern match(@NonNull final BLEDevice device) {
        try {
            final ScanRecord scanRecord = device.scanRecord();
            // Cannot match device without any scan record data
            if (null == scanRecord) {
                return null;
            }
            // Cannot match scan record where data is null
            final byte[] bytes = scanRecord.getBytes();
            if (null == bytes) {
                return null;
            }
            final Data rawData = new Data(bytes);
            // Attempt to match
            final MatchingPattern matchingPattern = match(filterPatterns, rawData);
            //noinspection ConstantConditions
            if (null == matchingPattern || null == matchingPattern.filterPattern ||
                    null == matchingPattern.filterPattern.pattern ||
                    null == matchingPattern.filterPattern.regularExpression ||
                    null == matchingPattern.message) {
                return null;
            } else {
                return matchingPattern;
            }
        } catch (Throwable e) {
            logger.fault("match, unknown error (device={},scanRecord={})", device, device.scanRecord());
            return null;
        }
    }

    /**
     * Should the device be ignored based on scan record data?
     * @param device BLE device
     * @return True if device should be ignored, false otherwise
     */
    private boolean ignoreBasedOnStatistics(@NonNull final BLEDevice device) {
        final ScanRecord scanRecord = device.scanRecord();
        // Do not ignore device without any scan record data
        if (null == scanRecord) {
            return false;
        }
        // Extract feature data from scan record
        // Do not ignore device without any feature data
        final List<Data> featureList = extractFeatures(scanRecord);
        if (null == featureList) {
            return false;
        }
        for (final Data featureData : featureList) {
            // Get training example statistics
            final ShouldIgnore shouldIgnore = samples.get(featureData);
            // Do not ignore device based on unknown feature data
            if (null == shouldIgnore) {
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
