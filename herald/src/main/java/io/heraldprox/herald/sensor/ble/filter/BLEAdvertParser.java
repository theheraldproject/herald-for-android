//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.ble.filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.Data;
import io.heraldprox.herald.sensor.datatype.UInt8;

import java.util.List;
import java.util.ArrayList;

public class BLEAdvertParser {

    @NonNull
    public static BLEScanResponseData parseScanResponse(@NonNull final byte[] raw, final int offset) {
        // Multiple segments until end of binary data
        return new BLEScanResponseData(raw.length - offset, extractSegments(raw, offset));
    }

    @NonNull
    public static List<BLEAdvertSegment> extractSegments(@NonNull final byte[] raw, final int offset) {
        final ArrayList<BLEAdvertSegment> segments = new ArrayList<>();
        int position = offset;
        int segmentLength;
        int segmentType;
        byte[] segmentData;
        Data rawData;
        int c;

        while (position < raw.length) {
            if ((position + 2) <= raw.length) {
                segmentLength = raw[position++] & 0xff;
                segmentType = raw[position++] & 0xff;
                // Note: Unsupported types are handled as 'unknown'
                // check reported length with actual remaining data length
                if ((position + segmentLength - 1) <= raw.length) {
                    segmentData = subDataBigEndian(raw, position, segmentLength - 1); // Note: type IS INCLUDED in length
                    rawData = new Data(subDataBigEndian(raw, position - 2, segmentLength + 1));
                    position += segmentLength - 1;
                    segments.add(new BLEAdvertSegment(BLEAdvertSegmentType.typeFor(segmentType), segmentLength - 1, segmentData, rawData));
                } else {
                    // error in data length - advance to end
                    position = raw.length;
                }
            } else {
                // invalid segment - advance to end
                position = raw.length;
            }
        }

        return segments;
    }

    @NonNull
    public static String hex(@NonNull final byte[] bytes) {
        final StringBuilder result = new StringBuilder();
        for (final byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @NonNull
    public static String binaryString(@NonNull final byte[] bytes) {
        final StringBuilder result = new StringBuilder();
        for (final byte b : bytes) {
            result.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            result.append(" ");
        }
        return result.toString();
    }

    @NonNull
    public static byte[] subDataBigEndian(@Nullable final byte[] raw, final int offset, final int length) {
        if (null == raw) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        final byte[] data = new byte[length];
        int position = offset;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position++];
        }
        return data;
    }

    @NonNull
    public static byte[] subDataLittleEndian(@Nullable final byte[] raw, final int offset, final int length) {
        if (null == raw) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        final byte[] data = new byte[length];
        int position = offset + length - 1;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position--];
        }
        return data;
    }

    @Nullable
    public static Integer extractTxPower(@NonNull final List<BLEAdvertSegment> segments) {
        // find the txPower code segment in the list
        for (final BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.txPowerLevel) {
                return (new UInt8(segment.data[0])).value;
            }
        }
        return null;
    }

    @NonNull
    public static List<BLEAdvertManufacturerData> extractManufacturerData(@NonNull final List<BLEAdvertSegment> segments) {
        // find the manufacturerData code segment in the list
        final List<BLEAdvertManufacturerData> manufacturerData = new ArrayList<>();
        for (final BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.manufacturerData) {
                // Ensure that the data area is long enough
                if (segment.data.length < 2) {
                    continue; // there may be a valid segment of same type... Happens for manufacturer data
                }
                // Create a manufacturer data segment
                final int intValue = ((segment.data[1]&0xff) << 8) | (segment.data[0]&0xff);
                manufacturerData.add(new BLEAdvertManufacturerData(intValue,subDataBigEndian(segment.data,2,segment.dataLength - 2), segment.raw));
            }
        }
        return manufacturerData;
    }

    @NonNull
    public static List <BLEAdvertAppleManufacturerSegment> extractAppleManufacturerSegments(@NonNull final List <BLEAdvertManufacturerData> manuData) {
        final List<BLEAdvertAppleManufacturerSegment> appleSegments = new ArrayList<>();
        for (final BLEAdvertManufacturerData manu : manuData) {
            int bytePos = 0;
            while (bytePos < manu.data.length) {
                final byte type = manu.data[bytePos];
                final int typeValue = type & 0xFF;
                // "01" marks legacy service UUID encoding without length data
                if (0x01 == type) {
                    final int length = manu.data.length - bytePos - 1;
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 1, length));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, manu.data.length - bytePos));
                    final BLEAdvertAppleManufacturerSegment segment = new BLEAdvertAppleManufacturerSegment(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos = manu.data.length;
                }
                // Parse according to Type-Length-Data
                else {
                    final int length = manu.data[bytePos + 1] & 0xFF;
                    final int maxLength = Math.min(length, manu.data.length - bytePos - 2);
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 2, maxLength));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, maxLength + 2));
                    final BLEAdvertAppleManufacturerSegment segment = new BLEAdvertAppleManufacturerSegment(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos += (maxLength + 2);
                }
            }
        }
        return appleSegments;
    }

    @NonNull
    public static List<BLEAdvertServiceData> extractServiceUUID16Data(@NonNull final List<BLEAdvertSegment> segments) {
        // find the serviceData code segment in the list
        final List<BLEAdvertServiceData> serviceData = new ArrayList<>();
        for (final BLEAdvertSegment segment : segments) {
            if (segment.type == BLEAdvertSegmentType.serviceUUID16Data) {
                // Ensure that the data area is long enough
                if (segment.data.length < 2) {
                    continue; // there may be a valid segment of same type... Happens for manufacturer data
                }
                // Create a service data segment
                final byte[] serviceUUID16LittleEndian = subDataLittleEndian(segment.data,0,2);
                serviceData.add(new BLEAdvertServiceData(serviceUUID16LittleEndian, subDataBigEndian(segment.data,2,segment.dataLength - 2), segment.raw));
            }
        }
        return serviceData;
    }
}
